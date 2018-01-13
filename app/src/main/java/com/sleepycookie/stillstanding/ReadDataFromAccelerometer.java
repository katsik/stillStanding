package com.sleepycookie.stillstanding;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;

public class ReadDataFromAccelerometer extends AppCompatActivity implements SensorEventListener,
                                                            GoogleApiClient.ConnectionCallbacks,
                                                            GoogleApiClient.OnConnectionFailedListener{

    private SensorManager mSensorManager;

    static public Context context;
    public TextView mAccelerationLabel;
    public double ax,ay,az;
    public double svTotalAcceleration;
    static int BUFFER_SIZE = 50;
    static int SAMPLES_BUFFER_SIZE = 2;
    static public double[] samples = new double[SAMPLES_BUFFER_SIZE];
    static public String[] states = new String[BUFFER_SIZE];

    final static double GRAVITY_ACCELERATION = 9.81;

    public static String currentState;
    double sigma = 0.5,th =10, th1 = 5, th2 = 2;

    public GoogleApiClient mApiClient;
    public ActivityRecognitionClient activityRecognitionClient;
    private PendingIntent pendingIntent;

    public Button quitButton;
    public Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_data_from_accelerometer);
        ReadDataFromAccelerometer.context = getApplicationContext();

/*        sensor provides data according to relationship :
                linear acceleration = acceleration - acceleration due to gravity
 */
        quitButton = (Button)findViewById(R.id.btn_quit);
        initQuitFunctionality();
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_UI);

        mAccelerationLabel = (TextView)findViewById(R.id.tv_collecting);

        for(int i=0;i<samples.length;i++){
            samples[i] = 0;
        }
        currentState = "none";

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();
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
        //TODO
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
            mAccelerationLabel.setText(Double.toString(svTotalAcceleration));
            samples[SAMPLES_BUFFER_SIZE-1] = svTotalAcceleration;
            if(fallDetected()){
                //update user state table
                currentState = "fell";
            }
            renewStates();
            checkPosture();
        }
    }

    public void renewStates(){
        for(int i = 0; i < BUFFER_SIZE - 2; i++){
            states[i] = states[i+1];
        }
        states[BUFFER_SIZE-1] = currentState;
//        if (states[BUFFER_SIZE-1]!="fell"){
//            Log.d("Renew States","last element: "+states[BUFFER_SIZE-1]);
//        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //TODO ?
    }

    /**
     * This method is used to detect a fall of the user.
     *
     * What we do here is the following. First we compare the last acceleration value with a low threshold
     * which is 0.5*gravity_acceleration(=9.81 m/s^2) if this value is less than our low threshold we
     * continue to comparing the previous from the last value to check if a value over our high threshold
     * was sampled (high threshold = 3 * gravity_acceleration). If both comparisons come true then a fall was detected.
     *
     * TL;DR
     * @return true in case a fall was detected false otherwise.
     */
    public boolean fallDetected(){
        //1. compare acceleration amplitude with lower threshold
        //2. if acceleration is less than low_threshold compare if next_acceleration_amplitude > high_threshold
        //3. if true fall detected!

        //setting as the lowest threshold acceptable the 0.6*9.81 [m/s^2]
        if (samples[SAMPLES_BUFFER_SIZE-1] <= 0.5 * GRAVITY_ACCELERATION){
            //highest threshold acceptable is 2.5 * 9.81 [m/s^2]
            if (samples[0] <= 3 * GRAVITY_ACCELERATION){
                // Fall detected because currently acceleration hit high threshold
                // and previously had hit the low threshold.
                Log.d("Fall Detection","Fall Detected!");
                showAToast("It seems like you fell");

                return true;
            }

        }

        return false;
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
     * This method checks weather the user is still, walking, running etc.
     *
     * If a fall of user is detected the next thing to monitor will be whether the user will stand up
     * or will remain on the ground. If the user stands up and starts walking/running this means that
     * either we had a FP fall detection or the user fell and stood up again so no need to trigger an
     * emergency event.
     */
    public void checkPosture(){
        //check from the state array to see if the "fell" state became "still"
        //if so the user fell and is laid down
        //else the user fell and stood up so no need to worry.
        boolean flag = false;
        for (String state : states ){
            if(!flag){
                if(state == "fell"){flag = true;}
            }else{
                if(state == "still"){
                    //turn off the flag
                    flag = false;
                //TODO triggerEmergency function which will handle the calling emergContact or the SMS sending
                    triggerEmergency();
                }else if (state == "walking" || state =="running" || state == "tilting"){
                    //user fell and stood up no need for emergency handling just turning off the flag
                    flag = false;
                }
            }
        }
    }

    /**
     * This method will be used to trigger the actions needed to be done in case user falls.
     * Probably a phone calling functionality to the emergency contact will be added or even a message
     * to his cell phone or Messenger, WhatsApp, Viber etc.
     *
     * Currently this method makes a notification sound just for debugging purposes.
     */
    public void triggerEmergency(){
        mAccelerationLabel.setText("User fell and didn't stand up");
        Toast.makeText(this,"User fell and didn't stand up",Toast.LENGTH_LONG).show();
        try{
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(),notification);
            r.play();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void setUsersState(String setState){
        currentState = setState;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Intent intent = new Intent(this, ActivityRecognizedService.class);
        pendingIntent = PendingIntent.getService(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        activityRecognitionClient = ActivityRecognition.getClient(this);
        activityRecognitionClient.requestActivityUpdates(500,pendingIntent);
//        activityRecognitionClient.requestActivityUpdates(0,pendingIntent);
    }

    /**
     * We won't be using this.
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {}

    /**
     * Method called in case the connection to Google Play cannot be achieved.
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("ActivityDetector","Connection to Google Play Services failed.");
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
        try{
            //unbind client and stop receiving updates according his/her activity
            activityRecognitionClient.removeActivityUpdates(pendingIntent);
            Log.d("Activity Recognition","ActivityRecognitionClient Removed.");
        }catch (IllegalStateException e){
            //the client probably was not initialized or something, just ignore and exit
            Log.e("Activity Recognition","IllegalStateException caused: "+ e.getMessage());
        }

        if (mApiClient.isConnected()) {
            Intent intent2 = new Intent(this.getApplicationContext(), ActivityRecognizedService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this.getApplicationContext(), 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
            activityRecognitionClient.removeActivityUpdates(pendingIntent);
            mApiClient.disconnect();
        }
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

    public static void toastingBoom(String msg){
        Log.d("Activity Detected",msg);
    }
}
// TODO make the Toasts from fall detection stop stacking in queue!