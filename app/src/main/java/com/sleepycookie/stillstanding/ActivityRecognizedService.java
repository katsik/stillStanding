package com.sleepycookie.stillstanding;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

/**
 * Created by George Katsikopoulos on 12/21/2017.
 */

public class ActivityRecognizedService extends IntentService {

    public ActivityRecognizedService(String name){
        super(name);
    }

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(ActivityRecognitionResult.hasResult(intent)){
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities(result.getProbableActivities());
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        int maxConfidence = 0;
        DetectedActivity mostProbableActivity = new DetectedActivity(DetectedActivity.UNKNOWN,100);
        for( DetectedActivity activity : probableActivities ) {
            if (activity.getConfidence() > maxConfidence){
                mostProbableActivity = activity;
                maxConfidence = mostProbableActivity.getConfidence();
            }
        }
        switch (mostProbableActivity.getType()){
            case DetectedActivity.ON_FOOT:
                ReadDataFromAccelerometer.setUsersState("walking");
                Log.d("Activity Detection","Walking");
                break;
            case DetectedActivity.RUNNING:
                ReadDataFromAccelerometer.setUsersState("running");
                Log.d("Activity Detection","Running");
                break;
            case DetectedActivity.WALKING:
                ReadDataFromAccelerometer.setUsersState("walking");
                Log.d("Activity Detection","Walking");
                break;
            case DetectedActivity.STILL:
                ReadDataFromAccelerometer.setUsersState("still");
                Log.d("Activity Detection","Still");
                break;
            case DetectedActivity.TILTING:
                ReadDataFromAccelerometer.setUsersState("tilting");
                Log.d("Activity Detection","Tilting");
                break;
            default:
                ReadDataFromAccelerometer.setUsersState("unknown");
                Log.d("Activity Detection","Unknown");
        }
    }
}
