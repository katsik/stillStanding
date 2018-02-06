package com.sleepycookie.stillstanding;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.sleepycookie.stillstanding.ui.ReadDataFromAccelerometer;

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
    protected void onHandleIntent(Intent intent) {
        if(ActivityRecognitionResult.hasResult(intent)){
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
//            handleDetectedActivities(result.getProbableActivities());
            DetectedActivity mostProbableActivity = result.getMostProbableActivity();
            int confidence = mostProbableActivity.getConfidence();
            int activityType = mostProbableActivity.getType();
            /*types:
            * DetectedActivity.IN_VEHICLE
            * DetectedActivity.ON_BICYCLE
            * DetectedActivity.ON_FOOT
            * DetectedActivity.STILL
            * DetectedActivity.UNKNOWN
            * DetectedActivity.TILTING
            */
            handleDetectedActivities(mostProbableActivity);
        }
    }

    private void handleDetectedActivities(DetectedActivity probableActivity) {
//        int maxConfidence = 0;
//        DetectedActivity mostProbableActivity = new DetectedActivity(DetectedActivity.UNKNOWN,100);
//        for( DetectedActivity activity : probableActivities ) {
//            if (activity.getConfidence() > maxConfidence){
//                mostProbableActivity = activity;
//                maxConfidence = mostProbableActivity.getConfidence();
//            }
//        }
        switch (probableActivity.getType()){
            case DetectedActivity.ON_FOOT:
                ReadDataFromAccelerometer.toastingBoom("walking");
                ReadDataFromAccelerometer.setUsersState("walking");
                Log.d("Activity Detection","Walking");
                break;
            case DetectedActivity.RUNNING:
                ReadDataFromAccelerometer.toastingBoom("running");
                ReadDataFromAccelerometer.setUsersState("running");
                Log.d("Activity Detection","Running");
                break;
            case DetectedActivity.WALKING:
                ReadDataFromAccelerometer.toastingBoom("walking");
                ReadDataFromAccelerometer.setUsersState("walking");
                Log.d("Activity Detection","Walking");
                break;
            case DetectedActivity.STILL:
                ReadDataFromAccelerometer.toastingBoom("still");
                ReadDataFromAccelerometer.setUsersState("still");
                Log.d("Activity Detection","Still");
                break;
            case DetectedActivity.TILTING:
                ReadDataFromAccelerometer.toastingBoom("tilting");
                ReadDataFromAccelerometer.setUsersState("tilting");
                Log.d("Activity Detection","Tilting");
                break;
            default:
                ReadDataFromAccelerometer.toastingBoom("unknown");
                ReadDataFromAccelerometer.setUsersState("unknown");
                Log.d("Activity Detection","Unknown");
        }

    }
}
