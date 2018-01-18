package com.sleepycookie.stillstanding.data;

/**
 * Created by George Katsikopoulos on 1/18/2018.
 */

public class StillStandingPreferences {

    public static String safetyContact;


    public static void setSafetyContact(String cnt){
        safetyContact = cnt;
    }

    public static String getSafetyContact(){
        return safetyContact;
    }
}
