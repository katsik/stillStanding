package com.sleepycookie.stillstanding;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;

public class ReadDataFromAccelerometer extends AppCompatActivity implements SensorEventListener,
                                                            GoogleApiClient.ConnectionCallbacks,
                                                            GoogleApiClient.OnConnectionFailedListener{

    private SensorManager mSensorManager;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_data_from_accelerometer);
/*        sensor provides data according to relationship :
                linear acceleration = acceleration - acceleration due to gravity
 */
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
//            mAccelerationLabel.setText(Double.toString(svTotalAcceleration));
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
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //TODO ?
    }

    public boolean fallDetected(){
        //1. compare acceleration amplitude with lower threshold
        //2. if acceleration is less than low_threshold compare if next_acceleration_amplitude > high_threshold
        //3. if true fall detected!

        //setting as the highest threshold acceptable the 2.5*9.81 [m/s^2]
        if (samples[BUFFER_SIZE-1] >= 2.5 * GRAVITY_ACCELERATION){
            for (int i =0; i<=BUFFER_SIZE-2;i++){
                //lowest threshold acceptable is 0.6 * 9.81 [m/s^2]
                if (samples[i] <= 0.6 * GRAVITY_ACCELERATION){
                    // Fall detected because currently acceleration hit high threshold
                    // and previously had hit the low threshold.
                    return true;
                }
            }
        }

        return false;
    }

    public void checkPosture(){
        //check from the state array to see if the "fall" state became "still"
        //if so the user fell and is laid down
        //else the user fell and stood up so no need to worry.
    }

    public static void setUsersState(String setState){
        currentState = setState;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Intent intent = new Intent(this, ActivityRecognizedService.class);
        pendingIntent = PendingIntent.getService(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        activityRecognitionClient = ActivityRecognition.getClient(this);
        activityRecognitionClient.requestActivityUpdates(0,pendingIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("ActivityDetector","Connection to Google Play Services failed.");
    }

    @Override
    protected void onStop() {
        super.onStop();
        try{
            activityRecognitionClient.removeActivityUpdates(pendingIntent);
            Log.d("Activity Recognition","ActivityRecognitionClient Removed.");
        }catch (IllegalStateException e){
            //the client probably was not initialized or something, just ignore and exit
            Log.e("Activity Recognition","IllegalStateException caused: "+ e.getMessage());
        }
    }
}
