package com.sleepycookie.stillstanding.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sleepycookie.stillstanding.AnalyzeDataFromAccelerometer;
import com.sleepycookie.stillstanding.R;
import com.sleepycookie.stillstanding.data.AppDatabase;
import com.sleepycookie.stillstanding.data.Incident;
import com.sleepycookie.stillstanding.data.Preferences;

import java.util.Date;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

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
    AlertDialog alertDialog;

    private Boolean accelerationBalanced;
    private Boolean stoodUp;
    private Boolean userFell;
    private Boolean acceleratorStarted = false;

    private Context mContext;

    private Long timeOfFall;

    public android.support.v7.widget.CardView warningCard;
    public Button warningOkButton;
    public Button warningEmergencyButton;

    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_data_from_accelerometer);
        ReadDataFromAccelerometer.context = getApplicationContext();

        db = AppDatabase.getInstance(this);

        mContext = this;

/*        sensor provides data according to relationship :
                linear acceleration = acceleration - acceleration due to gravity
 */

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

//            if(userFell){
//                //update user state table
//                currentState = "fell";
////                setTimeOfFall(System.currentTimeMillis());
//            }

            if(userFell && getTimeOfFall()!=null){
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
            showAToast("It seems like you fell");

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
                triggerEmergency();
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


    /**
     * This method will be used to trigger the actions needed to be done in case user falls.
     * Probably a phone calling functionality to the emergency contact will be added or even a message
     * to his cell phone or Messenger, WhatsApp, Viber etc.
     *
     * Currently this method places a call to the saved phone.
     */

    //TODO Handle the case where the user has set no emergency contact. Maybe play an alarm, instead of calling null.

    public void triggerEmergency(){
        mLabelTextView.setText("Fall Detected!");
        showAToast("User fell and didn't stand up");


        boolean smsPref = Preferences.getSmsPref(this);
        String smsBody = Preferences.getSmsBody(this);
        boolean locationPref = Preferences.getLocationPref(this);
        String mNumber = Preferences.getNumber(this);

        //initialize state values to prevent multiple calling events.
        initValues();

        try{
            if(!smsPref){
                int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE);
                if (permissionCheck==PERMISSION_GRANTED){
                    Intent callingIntent = new Intent(Intent.ACTION_CALL);
                    callingIntent.setData(Uri.parse("tel:" + mNumber));
                    db.incidentDao().insertIncidents(new Incident(new Date(), "Call to " + mNumber, 1, 0, 0));
                    mContext.startActivity(callingIntent);
                }
                else{
                    Log.d("Place emergency call", "User didn't give permission (phone)");
                }
            }else if(smsPref && !locationPref){
                int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
                if (permissionCheck==PERMISSION_GRANTED) {
                    StringBuffer smsBodyBuilder = new StringBuffer();
                    smsBodyBuilder.append(smsBody);

                    db.incidentDao().insertIncidents(new Incident(new Date(), "SMS to " + mNumber, 2, 0, 0));
                    SmsManager manager = SmsManager.getDefault();
                    manager.sendTextMessage(mNumber, null, smsBodyBuilder.toString(), null, null);
                    Log.d("Trigger", smsBodyBuilder.toString());

                    showAToast("SMS sent to " + mNumber);
                }else{
                    Log.d("Place emergency call", "User didn't give permission (SMS)");
                }
            }else if(smsPref && locationPref){
                int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
                int permissionCheck2 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
                if (permissionCheck==PERMISSION_GRANTED && permissionCheck2==PERMISSION_GRANTED){
                    new LocationRetrieving().execute(new String[] {mNumber,smsBody});
                }else{
                    Log.d("Place emergency call", "User didn't give permission (SMS/Location)");
                }
            }else{ //TODO review if we want this
                //play alarm
                db.incidentDao().insertIncidents(new Incident(new Date(), "Alarm played", 3, 0, 0));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
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

        // Don't receive any more updates from sensor.
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setAccelerationStrated(false);

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
                triggerEmergency();
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
                triggerEmergency();
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

        if(mBound==null || !mBound){
            Intent intent = new Intent(this,AnalyzeDataFromAccelerometer.class);
            bindService(intent, mConnection,BIND_AUTO_CREATE);
        }

        if(extras!=null){
            fell = extras.getBoolean(getString(R.string.fall_detected_key));
            setUserFell(fell);
            if(fell){
                long time = extras.getLong(getString(R.string.fall_deteciton_time_key));
                setTimeOfFall(time);
            }
        }
    }

    public void setAccelerationStrated(Boolean accelerationStrated){this.acceleratorStarted = accelerationStrated;}
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
            setAccelerationStrated(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public class LocationRetrieving extends AsyncTask<String, Void, String[]> implements LocationListener{
        Location currentLocation;
        Double currentLatitude, currentLongitude;
//        ProgressDialog dialog = new ProgressDialog(mContext);

        @Override
        protected void onPreExecute() {
            Toast.makeText(context,"Sending SMS to emergency contact...", Toast.LENGTH_SHORT).show();
        }

        /**
         *
         * This method takes the location which has already been retrieved by doInBackground.
         * After location is set we enhance the SMS body already specified by user with his/her
         * latitude & longitude. Finally we sent an SMS to the user's emergency contact.
         *
         * @param SMSAttributes first attribute contains number and second contains SMS body
         */

        @Override
        protected void onPostExecute(String[] SMSAttributes) {
            //first argument is number, second is sms body
            //latitude and longitude can be acquired by getCurrentLocation and the respective getters.
            String number = SMSAttributes[0];
            String smsBody = SMSAttributes[1];

            StringBuffer smsBodyBuilder = new StringBuffer();
            smsBodyBuilder.append(smsBody);

            double[] coordinates = {getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude()};

            smsBodyBuilder.append("\n \n" + getString(R.string.sms_location_text));
            smsBodyBuilder.append("http://maps.google.com?q=");
            smsBodyBuilder.append(String.format ("%.7f", coordinates[0]).replaceAll(",", "."));
            smsBodyBuilder.append(",");
            smsBodyBuilder.append(String.format ("%.7f", coordinates[1]).replaceAll(",", "."));

            db.incidentDao().insertIncidents(new Incident(new Date(), "SMS to " + number, 2, coordinates[0], coordinates[1]));
            SmsManager manager = SmsManager.getDefault();
            manager.sendTextMessage(number, null, smsBodyBuilder.toString(), null, null);

            Toast.makeText(context,"SMS sent to "+ number,Toast.LENGTH_SHORT).show();
        }

        /**
         *
         * In background there is a location manager initialized and responsible for location tracking.
         * We only get the location once, either by gps or by network depending on what's available on
         * the user's phone.
         * Due to the fact that it may take some time to get the new location we initialize a looper
         * for waiting until the location is returned by the location manager. Once the location returned
         * the looper stops waiting.
         * When location is returned we store it to the appropriate variable through updateLocation()
         *
         *
         * @param args
         * @return an array of Strings with the phone number and the sms body.
         */
        @Override
        protected String[] doInBackground(String... args) {
            if(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PERMISSION_GRANTED){
                LocationManager mLocationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
                boolean isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                boolean isGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

                if (isNetworkEnabled){
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                    Looper.prepare();
                    mLocationManager.requestSingleUpdate(criteria,this,null);
                    Looper.loop();

                }else if(isGPSEnabled){
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_FINE);
                    Looper.prepare();
                    mLocationManager.requestSingleUpdate(criteria,this,null);
                    Looper.loop();
                }

                if(getCurrentLocation()==null){
                    Log.e("Location","No location returned");
                }
                return new String[]{args[0],
                        args[1]};

            }else{
                //return a dummy location
                Location dummyLocation = new Location("");
                dummyLocation.setLatitude(0.0d);
                dummyLocation.setLongitude(0.0d);
                updateLocation(dummyLocation);
                Log.e("Location","returned dummy location");
                return new String[]{args[0],
                        args[1]};
            }
        }

        /**
         * Updates the user's location
         *
         * @param location
         */
        protected void updateLocation(Location location){
            currentLocation = location;
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();
        }

        protected Location getCurrentLocation(){return currentLocation;}

        //-----------------------------------LocationListener's Overrided methods------------------------------------

        /**
         * Updates location and stops looper.
         * @param location
         */
        @Override
        public void onLocationChanged(Location location) {
            updateLocation(location);
            Looper.myLooper().quit();
        }

        @Override public void onStatusChanged(String s, int i, Bundle bundle) {}
        @Override public void onProviderEnabled(String s) {}
        @Override public void onProviderDisabled(String s) {}
    }
}