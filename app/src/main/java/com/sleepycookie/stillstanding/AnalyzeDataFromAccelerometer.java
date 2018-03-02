package com.sleepycookie.stillstanding;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.sleepycookie.stillstanding.data.Preferences;
import com.sleepycookie.stillstanding.ui.MainActivity;

import java.util.Random;

/**
 * Created by George Katsikopoulos on 2/13/2018.
 */

//TODO bind this service with readdatafromaccelerometer, measure acceleration,
//TODO if fall detected launch reddatafromaccelerometer with some kind of token,
//TODO and then handle the fact from that class just as it is now.
public class AnalyzeDataFromAccelerometer extends Service implements SensorEventListener {
    //Binder to give to the clients.
    private final IBinder mBinder = new AnalyzeDataBinder();

    public SensorManager mSensorManager;

    public double ax,ay,az;
    public double svTotalAcceleration;

    static int SAMPLES_BUFFER_SIZE = 10;
    static public double[] samples = new double[SAMPLES_BUFFER_SIZE];

    final static double GRAVITY_ACCELERATION = 9.81;

    private NotificationManager notificationManager;

    public static Boolean serviceRunning = false;

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

            if(fallDetected()){
                if(MainActivity.getActiveStatus()){
                    Intent broadcastIntent = new Intent("broadcastIntent");
                    broadcastIntent.putExtra(getString(R.string.fall_detected_key),true);
                    broadcastIntent.putExtra(getString(R.string.fall_deteciton_time_key),System.currentTimeMillis());
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                }else{
                    Intent readDataIntent = new Intent(AnalyzeDataFromAccelerometer.this, MainActivity.class);
                    readDataIntent.putExtra(getString(R.string.fall_detected_key),true);
                    readDataIntent.putExtra(getString(R.string.fall_deteciton_time_key),System.currentTimeMillis());
                    readDataIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(readDataIntent);
                }
                initSamples();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class AnalyzeDataBinder extends Binder{
        public AnalyzeDataFromAccelerometer getService(){
            //Return the instance so clients can call public methods
            return AnalyzeDataFromAccelerometer.this;
        }
    }

    @Override
    public void onCreate() {
        //make a connection with accelerometer and see if we are having a fall
        super.onCreate();
        serviceRunning = true;
    }

    //not needed probably
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if(mSensorManager != null) {
            //stop getting info from accelerometer
            stopAccelerometer();
        }
        serviceRunning = false;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        showNotification();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        notificationManager.cancelAll();
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        showNotification();
    }

    public void startAccelerometer(){
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
    }

    public void stopAccelerometer(){
        mSensorManager.unregisterListener(this);
    }

    /**
     * This method is used to detect a fall of the user.
     *
     * What we do here is the following. We already have a collection of the 10 latest acceleration
     * values. We, then, make a comparison between the newest and the oldest value of the accelerations.
     * If the difference is greater or equal than FALL_THRESHOLD * GRAVITY_ACCELERATION (9.81 [m/s^2]) then we believe
     * this indicates a fall and a true value is returned. In any other case a false value is returned.
     *
     * TL;DR
     * @return true in case a fall was detected false otherwise.
     */
    public boolean fallDetected(){
        //1. compare acceleration amplitude with lower threshold
        //2. if acceleration is less than low_threshold compare if next_acceleration_amplitude > high_threshold
        //3. if true fall detected!
        if(samples[SAMPLES_BUFFER_SIZE-1] - samples[0] >= Preferences.getFallThreshold(this) * GRAVITY_ACCELERATION){
            Log.d("fallDetected","FALL DETECTED!");
            return true;
        }
        return false;
    }

    public void initSamples(){
        for(int i=0; i < samples.length; i++){
            samples[i] = 0;
        }
    }

    /**
     * This method is used to show a notification while the service is active.
     *
     * If the user taps the notification, MainActivity is shown.
     */
    private void showNotification(){
        //add a notification
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // prepare intent which is triggered if the
        // notification is selected

        Intent notifIntent = new Intent(this, MainActivity.class);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), notifIntent, 0);

        NotificationHelper noti = new NotificationHelper(this);
        NotificationCompat.Builder nb = noti.getNotification1(getString(R.string.noti_title),
                getString(R.string.noti_body), pIntent);
        noti.notify(0, nb);
    }
}
