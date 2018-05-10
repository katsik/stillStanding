package com.sleepycookie.stillstanding.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.sleepycookie.stillstanding.data.Preferences;

import java.util.ArrayList;

/**
 * Created by geotsam on 16/02/2018.
 */

public class PermissionManager {
    public static void checkForPermissions(Context context, Activity activity) {
        int MY_PERMISSIONS_REQUEST_ALL = 1;

        boolean smsPref = Preferences.getSmsPref(context);
        boolean locationPref = Preferences.getLocationPref(context);

        String[] PERMISSIONS;
        ArrayList<String> permissions = new ArrayList<>();

        if(ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
            permissions.add(Manifest.permission.CALL_PHONE);
        }
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            permissions.add(Manifest.permission.READ_CONTACTS);
        }
        if((smsPref) &&  (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)){
            permissions.add(Manifest.permission.SEND_SMS);
        }
        if(locationPref && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if(permissions.size()>0) {
            PERMISSIONS = permissions.toArray(new String[0]);
            ActivityCompat.requestPermissions(activity, PERMISSIONS, MY_PERMISSIONS_REQUEST_ALL);
        }
    }
}
