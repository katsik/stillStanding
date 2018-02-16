package com.sleepycookie.stillstanding.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

/**
 * Created by George Katsikopoulos on 1/18/2018.
 */

public final class Preferences {

    public static final String KEY_CONTACT = "contact_pref";
    public static final String KEY_PHONE = "phone_pref";
    public static final String KEY_PHOTO = "photo_pref";
    public static final String KEY_SMS = "sms_preference";
    public static final String KEY_SMS_BODY = "sms_body_preference";
    public static final String KEY_SMS_LOCATION = "location_preference";
    public static final String COMPLETED_ONBOARDING_PREF = "onboard_key";

    public static boolean getSmsPref(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(KEY_SMS, false);
    }

    public static String getSmsBody(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(KEY_SMS_BODY, "");
    }

    public static boolean getLocationPref(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(KEY_SMS_LOCATION,false);
    }

    public static String getNumber(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(KEY_PHONE,null);
    }

    public static String getContact(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(KEY_CONTACT,null);
    }

    public static String getPhoto(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(KEY_PHOTO, null);
    }

    public static boolean getIntroPref(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(COMPLETED_ONBOARDING_PREF, false);
    }

    public static void setNumber(Context context, String number){
        SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        sharedPreferencesEditor.putString(KEY_PHONE, number);
        sharedPreferencesEditor.apply();
    }

    public static void setContact(Context context, String contact){
        SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        sharedPreferencesEditor.putString(KEY_CONTACT, contact);
        sharedPreferencesEditor.apply();
    }

    public static void setPhoto(Context context, Uri photoUri){
        SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        if (photoUri == null){
            sharedPreferencesEditor.putString(KEY_PHOTO,null);
        } else {
            sharedPreferencesEditor.putString(KEY_PHOTO, photoUri.toString());
        }
        sharedPreferencesEditor.apply();
    }

    public static void setSmsPref(Context context, boolean pref){
        SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        sharedPreferencesEditor.putBoolean(KEY_SMS, pref);
        sharedPreferencesEditor.apply();
    }

    public static void setIntroPref(Context context, boolean pref){
        SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        sharedPreferencesEditor.putBoolean(COMPLETED_ONBOARDING_PREF, pref);
        sharedPreferencesEditor.apply();
    }
}
