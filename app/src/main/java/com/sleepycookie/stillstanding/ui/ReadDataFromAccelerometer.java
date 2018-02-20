package com.sleepycookie.stillstanding.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sleepycookie.stillstanding.AnalyzeDataFromAccelerometer;
import com.sleepycookie.stillstanding.Emergency;
import com.sleepycookie.stillstanding.R;
import com.sleepycookie.stillstanding.data.AppDatabase;

public class ReadDataFromAccelerometer extends AppCompatActivity implements SensorEventListener{

    private SensorManager mSensorManager;

    AnalyzeDataFromAccelerometer mService;
    Boolean mBound;

    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int DETECTION_INTERVAL_SECONDS = 20;
    public static final int DETECTION_INTERVAL_MILLISECONDS =
            MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;

    public static final int DETECTION_INTERVAL_ASAP = 0;
    static public Context context;
    public TextView mLabelTextView;
    public double ax,ay,az;
    public double svTotalAcceleration;
    static int BUFFER_SIZE = 50;
    static int SAMPLES_BUFFER_SIZE = 10;
    static public double[] samples = new double[SAMPLES_BUFFER_SIZE];
    public String[] states = new String[BUFFER_SIZE];

    private double[] location = new double[2];
    final static double GRAVITY_ACCELERATION = 9.81;

    static public String currentState = "";

    
    public Button quitButton;
    public Button triggerButton;
    public Toast mToast;

    private Boolean accelerationBalanced;
    private Boolean stoodUp;
    private Boolean userFell;
    private static Boolean onCall = false;
    private static Boolean active;

    private Context mContext;

    private Long timeOfFall;

    public android.support.v7.widget.CardView warningCard;
    public Button warningOkButton;
    public Button warningEmergencyButton;

    private AppDatabase db;

    private CountDownTimer timer;
    private TextView timerTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_data_from_accelerometer);
        ReadDataFromAccelerometer.context = getApplicationContext();

        db = AppDatabase.getInstance(this);

        mContext = this;

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        triggerButton = findViewById(R.id.btn_trigger);
        initTriggerFunctionality();

        quitButton = findViewById(R.id.btn_quit);
        initQuitFunctionality();
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_UI);

        mLabelTextView = findViewById(R.id.tv_collecting);
        mLabelTextView.setText("Analyzing Data...");

        timerTextView = findViewById(R.id.tv_emergency_timer);

        warningCard = findViewById(R.id.warning_card);
        warningEmergencyButton = findViewById(R.id.warning_action_fell);
        warningOkButton = findViewById(R.id.warning_action_ok);
        initWarningButtonFunctionality();

        initValues();
    }

    /**
     * This method initializes the arrays used for keeping track of the states and the acceleration values.
     */
    public void initValues(){
        for(int i=0;i<samples.length;i++){
            samples[i] = 0;
        }
        currentState = "none";
        setUserFell(false);
        setAccelerationBalanced(false);
        setStoodUp(false);
        setTimeOfFall(null);
        for (int j = 0; j<states.length; j++){
            states[j] = currentState;
        }
        warningCard.setVisibility(View.GONE);
        mLabelTextView.setText("Analyzing Data...");
    }

    /**
     * Method triggered every time the accelerometer sensor detects a change and calculates the total acceleration
     * the device has.
     *
     * Maybe we could pass a high-pass filter to make the acceleration values smoother as indicated
     * in https://developer.android.com/reference/android/hardware/SensorEvent.html but definitely not
     * a low-pass filter cause this will filter out the sudden quick movements which literally define a fall.
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float [] events = sensorEvent.values;

            ax = events[0];
            ay = events[1];
            az = events[2];

            // calculate the sum of vector of acceleration
            svTotalAcceleration = Math.sqrt(Math.pow(ax,2)
                    +Math.pow(ay,2)
                    +Math.pow(az,2));

            for (int i =0 ; i<SAMPLES_BUFFER_SIZE-1 ; i++ ){
                //last place of buffer cleared
                samples[i] = samples[i+1];
            }
            samples[SAMPLES_BUFFER_SIZE-1] = svTotalAcceleration;

            if(userFell && getTimeOfFall()!=null){
                if(timer==null){
                    Log.d("userFell","timer initialized");
                    timerTextView.setVisibility(View.VISIBLE);
                    timer = new CountDownTimer(15000,MILLISECONDS_PER_SECOND) {
                        @Override
                        public void onTick(long l) {
                            timerTextView.setText((l/1000)+" seconds remaining until emergency triggered");
                        }

                        @Override
                        public void onFinish() {
                            timerTextView.setVisibility(View.GONE);
                        }
                    };
                    timer.start();
                }
                if(!mLabelTextView.getText().toString().equals(getString(R.string.fall_detected))){
                    mLabelTextView.setText(getString(R.string.fall_detected));
                }
                checkPosture(timeOfFall);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //TODO ?
    }

    /**
     * This method is used to detect a fall of the user.
     *
     * What we do here is the following. We already have a collection of the 10 latest acceleration
     * values. We, then, make a comparison between the newest and the oldest value of the accelerations.
     * If the difference is greater or equal than 2.5 * GRAVITY_ACCELERATION (9.81 [m/s^2]) then we believe
     * this indicates a fall and a true value is returned. In any other case a false value is returned.
     *
     * TL;DR
     * @return true in case a fall was detected false otherwise.
     */
    public boolean fallDetected(){
        //1. compare acceleration amplitude with lower threshold
        //2. if acceleration is less than low_threshold compare if next_acceleration_amplitude > high_threshold
        //3. if true fall detected!

        if(samples[SAMPLES_BUFFER_SIZE-1] - samples[0] >= 2.5 * GRAVITY_ACCELERATION){
//            Log.d("Fall Detection","Fall Detected!");
            showAToast(getString(R.string.toast_potential_fall));

            return true;
        }
        return false;
    }

    /**
     * This method checks weather the user stood up or is still down, assuming he/she has already fell.
     *
     * If a fall of user is detected the next thing to monitor will be whether the user will stand up
     * or will remain on the ground. If the user stands up we check the acceleration the device has and we
     * determine weather the user is up, and therefore there is no need for automatic emergency triggering
     * or if he/she cannot stand up and therefore there is an emergency triggering.
     */

    public void checkPosture(long timeSinceFall){
        //wait for 15 seconds (setting the time randomly) to see if user stands up during this time
        Log.d("checkPosture:", "time since fall in ms: "+timeSinceFall);
        long currentTime = System.currentTimeMillis();

        Log.d("checkPosture: ","time passed from fall: "+(currentTime-timeSinceFall));

        if(!getAccelerationBalanced()){
            Log.d("checkPosture","in acceleration balanced if statement");
            // we check for the last measurement to see if it's between the following limits
            accelerationBalanced = (samples[SAMPLES_BUFFER_SIZE-1] >= 9.6 && samples[SAMPLES_BUFFER_SIZE-1] <= 10.0);
            Log.d("checkPosture","accelerationBalanced: " + accelerationBalanced);
        }else{
            //acceleration has balanced between (9.5,10) so now we assume user is lying down
            Log.d("checkPosture","stoodup: "+stoodUp);
            if(!getStoodUp() && (currentTime - timeSinceFall < 5* MILLISECONDS_PER_SECOND)){
                //check to see if he stood up
                stoodUp = (samples[SAMPLES_BUFFER_SIZE-1] <= 0.65 * GRAVITY_ACCELERATION);
                Log.d("CheckPosture", "Not stood up yet.");
            }else if(!getStoodUp() && (currentTime - timeSinceFall >= 15* MILLISECONDS_PER_SECOND)){
                //user didn't stand up since fall and there's been 15 seconds => trigger action
                Log.d("checkPosture","not stood up and gonna trigger emergency");
                initValues();
                new Emergency(this, db, mToast).triggerEmergency();
            }else if(getStoodUp() && (currentTime - timeSinceFall <= 15 * MILLISECONDS_PER_SECOND)){

                warningCard.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Use this to prevent multiple Toasts spamming the UI
     *
     * @param message
     */
    public void showAToast(String message){
        if(mToast != null){
            mToast.cancel();
        }
        mToast = Toast.makeText(context,message,Toast.LENGTH_LONG);
        mToast.show();
    }

    public static void setUsersState(String setState){
        ReadDataFromAccelerometer.currentState = setState;
//        Log.d("setUsersState","currentState = " + currentState);
    }

    @Override
    protected void onResume(){
        super.onResume();

        mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("onPause","I'm heree");
    }

    @Override
    protected void onStop() {
        super.onStop();
        setActiveStatus(false);
        Log.d("onStop","I'm heree in on Stop");
        // Don't receive any more updates from sensor.
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


        unbindService(mConnection);
        mBound = false;
    }

    /**
    *Method used to initialize functionality of Quit button
    **/
    public void initQuitFunctionality(){
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishAffinity();

            }
        });
    }

    /**
     *Method used to initialize functionality of Trigger button
     **/
    public void initTriggerFunctionality(){
        triggerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initValues();
                new Emergency(ReadDataFromAccelerometer.this, db, mToast).triggerEmergency();
            }
        });
    }

    /**
     *Method used to initialize functionality of Warning card buttons
     **/
    public void initWarningButtonFunctionality(){
        warningEmergencyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initValues();
                new Emergency(ReadDataFromAccelerometer.this, db, mToast).triggerEmergency();
            }
        });

        warningOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initValues();
            }
        });
    }

    //TODO add startAccelerometer service somewhere for the detection to start.
    @Override
    protected void onStart() {
        super.onStart();

        Boolean fell;
        Bundle extras = getIntent().getExtras();
        setActiveStatus(true);

        if(mBound==null || !mBound){
            Intent intent = new Intent(this,AnalyzeDataFromAccelerometer.class);
            bindService(intent, mConnection,BIND_AUTO_CREATE);
        }

        if(extras!=null && !onCall){
            Log.d("onStart","Hey there!!");
            fell = extras.getBoolean(getString(R.string.fall_detected_key));
            setUserFell(fell);
            if(fell){
                long time = extras.getLong(getString(R.string.fall_deteciton_time_key));
                setTimeOfFall(time);
            }
        }
        setOnCall(false);
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void setUserFell(Boolean fell){this.userFell = fell;}

    private Long getTimeOfFall(){
        return timeOfFall;
    }

    private void setTimeOfFall(Long time){
        timeOfFall = time;
    }

    public void setAccelerationBalanced(Boolean accelerationBalanced) {this.accelerationBalanced = accelerationBalanced;}
    public Boolean getAccelerationBalanced(){return accelerationBalanced;}

    public void setStoodUp(Boolean stoodUp) {this.stoodUp = stoodUp;}
    public Boolean getStoodUp(){return stoodUp;}

    //for debugging purposes
    public static void toastingBoom(String msg){
        Log.d("Activity Detected",msg);
    }
    public static void setActiveStatus(Boolean status){active = status;}
    public static Boolean getActiveStatus(){return active;}

    public static void setOnCall(Boolean onCall){ReadDataFromAccelerometer.onCall = onCall;}
    public static Boolean getOnCall(){return ReadDataFromAccelerometer.onCall;}

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            AnalyzeDataFromAccelerometer.AnalyzeDataBinder binder = (AnalyzeDataFromAccelerometer.AnalyzeDataBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.startAccelerometer();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}