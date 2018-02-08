package com.sleepycookie.stillstanding;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * Created by geotsam on 05/02/2018.
 */

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    public static final String KEY_SMS = "sms_preference";
    public static final String KEY_SMS_BODY = "sms_body_preference";
    public static final String KEY_SMS_LOCATION = "location_preference";

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals(KEY_SMS)) {
            Preference smsPref = findPreference(key);
        }

        if (key.equals(KEY_SMS_BODY)){
            Preference smsBodyPref = findPreference(key);
        }

        if(key.equals(KEY_SMS_LOCATION)){
            Preference locationPref = findPreference(key);
        }
    }
}
