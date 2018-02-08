package com.sleepycookie.stillstanding.utils;

/**
 * Created by George Katsikopoulos on 2/7/2018.
 */

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import com.sleepycookie.stillstanding.ui.ReadDataFromAccelerometer;

public class SingleShotLocationProvider {

    public static interface LocationCallback {
        public void onNewLocationAvailable(GPSCoordinates location);
    }

    /** calls back to calling thread, note this is for low grain: if you want higher precision, swap the
    *   contents of the else and if. Also be sure to check gps permission/settings are allowed.
    *   call usually takes <10ms
    *   taken from https://stackoverflow.com/questions/29657948/get-the-current-location-fast-and-once-in-android
    */

    public static void requestSingleUpdate(final Context context, final LocationCallback callback) {
        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (ContextCompat.checkSelfPermission(ReadDataFromAccelerometer.context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            if (isNetworkEnabled) {
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                locationManager.requestSingleUpdate(criteria, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        callback.onNewLocationAvailable(new GPSCoordinates(location.getLatitude(), location.getLongitude()));
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }
                }, null);
            } else {
                boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                if (isGPSEnabled) {
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_FINE);
                    locationManager.requestSingleUpdate(criteria, new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            callback.onNewLocationAvailable(new GPSCoordinates(location.getLatitude(), location.getLongitude()));
                        }

                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                        }
                    }, null);
                }
            }
        }
    }


    // consider returning Location instead of this dummy wrapper class
    public static class GPSCoordinates {
        public double longitude = -1;
        public double latitude = -1;

        public GPSCoordinates(float theLatitude, float theLongitude) {
            longitude = ((double) theLongitude);
            latitude = ((double) theLatitude);
        }

        public GPSCoordinates(double theLatitude, double theLongitude) {
            longitude = theLongitude;
            latitude = theLatitude;
        }
    }
}
