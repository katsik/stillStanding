package com.sleepycookie.stillstanding;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.Random;

/**
 * Created by George Katsikopoulos on 2/13/2018.
 */

//TODO bind this service with readdatafromaccelerometer, measure acceleration,
//TODO if fall detected launch reddatafromaccelerometer with some kind of token,
//TODO and then handle the fact from that class just as it is now.
public class AnalyzeDataFromAccelerometer extends Service {
    //Binder to give to the clients.
    private final IBinder mBinder = new AnalyzeDataBinder();

    private final Random mGenerator = new Random();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class AnalyzeDataBinder extends Binder{
        AnalyzeDataFromAccelerometer getService(){
            //Return the instance so clients can call public methods
            return AnalyzeDataFromAccelerometer.this;
        }
    }

    @Override
    public void onCreate() {
        //make a connection with accelerometer and see if we are having a fall
        super.onCreate();
    }

    //not needed probably
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        //stop getting info from accelerometer
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //start tracking and storing acceleration values from accelerometer
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

}
