package com.sleepycookie.stillstanding.data;

/**
 * Created by George Katsikopoulos on 1/18/2018.
 */

//TODO Evaluate if this is needed. Currently not in use.

public class StillStandingPreferences {

    public static String safetyContactName;

    public static String safetyContactNumber;

    public static String getSafetyContactName() {
        return safetyContactName;
    }

    public static String getSafetyContactNumber() {
        return safetyContactNumber;
    }

    public static void setSafetyContactName(String safetyContactName) {
        StillStandingPreferences.safetyContactName = safetyContactName;
    }

    public static void setSafetyContactNumber(String safetyContactNumber) {
        StillStandingPreferences.safetyContactNumber = safetyContactNumber;
    }
}
