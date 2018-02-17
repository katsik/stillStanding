package com.sleepycookie.stillstanding;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.sleepycookie.stillstanding.data.AppDatabase;
import com.sleepycookie.stillstanding.data.Incident;
import com.sleepycookie.stillstanding.data.Preferences;
import com.sleepycookie.stillstanding.ui.ReadDataFromAccelerometer;

import java.util.Date;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Created by geotsam on 16/02/2018.
 */

public class Emergency {

    AppDatabase db;
    Context context;
    Resources mR;
    Toast mToast;

    public Emergency(Context context, AppDatabase db, Toast mToast){
        this.context = context;
        this.db = db;
        this.mToast = mToast;
        mR = context.getResources();
    }

    /**
     * This method is used to trigger the actions needed to be done in case the user falls.
     * Currently this method calls the saved phone number or sends an SMS (with or without location).
     */

    //TODO Handle the case where the user has set no emergency contact. Maybe play an alarm.

    public void triggerEmergency(){
        showAToast(mR.getString(R.string.toast_fall_detected));

        boolean smsPref = Preferences.getSmsPref(context);
        String smsBody = Preferences.getSmsBody(context);
        boolean locationPref = Preferences.getLocationPref(context);
        String mNumber = Preferences.getNumber(context);

        try{
            if(!smsPref){
                int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE);
                if (permissionCheck==PERMISSION_GRANTED){
                    ReadDataFromAccelerometer.setOnCall(true);
                    Intent callingIntent = new Intent(Intent.ACTION_CALL);
                    callingIntent.setData(Uri.parse("tel:" + mNumber));
                    db.incidentDao().insertIncidents(new Incident(new Date(), "Call to " + mNumber, 1, 0, 0));
                    context.startActivity(callingIntent);
                }
                else{
                    Log.d("Place emergency call", "User didn't give permission (phone)");
                }
            }else if(smsPref && !locationPref){
                int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS);
                if (permissionCheck==PERMISSION_GRANTED) {
                    StringBuffer smsBodyBuilder = new StringBuffer();
                    smsBodyBuilder.append(smsBody);

                    db.incidentDao().insertIncidents(new Incident(new Date(), "SMS to " + mNumber, 2, 0, 0));
                    SmsManager manager = SmsManager.getDefault();
                    manager.sendTextMessage(mNumber, null, smsBodyBuilder.toString(), null, null);
                    Log.d("Trigger", smsBodyBuilder.toString());

                    showAToast(mR.getString(R.string.toast_sms_sent_to, mNumber));
                }else{
                    Log.d("Place emergency call", "User didn't give permission (SMS)");
                }
            }else if(smsPref && locationPref){
                int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS);
                int permissionCheck2 = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
                if (permissionCheck==PERMISSION_GRANTED && permissionCheck2==PERMISSION_GRANTED){
                    new Emergency.LocationRetrieving().execute(new String[] {mNumber,smsBody});
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

    public class LocationRetrieving extends AsyncTask<String, Void, String[]> implements LocationListener {
        Location currentLocation;
        Double currentLatitude, currentLongitude;
//        ProgressDialog dialog = new ProgressDialog(mContext);

        @Override
        protected void onPreExecute() {
            Toast.makeText(context, mR.getString(R.string.toast_sending_sms), Toast.LENGTH_SHORT).show();
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

            smsBodyBuilder.append("\n \n" + mR.getString(R.string.sms_location_text));
            smsBodyBuilder.append("http://maps.google.com?q=");
            smsBodyBuilder.append(String.format ("%.7f", coordinates[0]).replaceAll(",", "."));
            smsBodyBuilder.append(",");
            smsBodyBuilder.append(String.format ("%.7f", coordinates[1]).replaceAll(",", "."));

            db.incidentDao().insertIncidents(new Incident(new Date(), "SMS to " + number, 2, coordinates[0], coordinates[1]));
            SmsManager manager = SmsManager.getDefault();
            manager.sendTextMessage(number, null, smsBodyBuilder.toString(), null, null);

            Toast.makeText(context, mR.getString(R.string.toast_sms_sent_to, number),Toast.LENGTH_SHORT).show();
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
