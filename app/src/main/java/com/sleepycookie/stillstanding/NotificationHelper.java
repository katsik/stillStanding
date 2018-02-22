package com.sleepycookie.stillstanding;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;


/**
 * Created by geotsam on 17/02/2018.
 */

/**
 * Helper class to manage notification channels, and create notifications.
 */
@TargetApi(Build.VERSION_CODES.O)
class NotificationHelper extends ContextWrapper {
    private NotificationManager manager;
    public static final String PRIMARY_CHANNEL = "default";

    /**
     * Registers notification channels, which can be used later by individual notifications.
     *
     * @param ctx The application context
     */
    public NotificationHelper(Context ctx) {
        super(ctx);

        NotificationChannel chan1 = new NotificationChannel(PRIMARY_CHANNEL,
                getString(R.string.noti_channel_default), NotificationManager.IMPORTANCE_DEFAULT);
        chan1.setLightColor(Color.GREEN);
        chan1.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        getManager().createNotificationChannel(chan1);
    }

    /**
     * Get a notification of type 1
     *
     * Provide the builder rather than the notification it's self as useful for making notification
     * changes.
     *
     * @param title the title of the notification
     * @param body the body text for the notification
     * @return the builder as it keeps a reference to the notification (since API 24)
     */
    public Notification.Builder getNotification1(String title, String body, PendingIntent pIntent) {
        if(Integer.valueOf(Build.VERSION.SDK_INT) < 26){
            return new Notification.Builder(getApplicationContext())
                    .setContentTitle(title)
                    .setContentText(body)
                    .setSmallIcon(getSmallIcon())
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentIntent(pIntent);
        } else {
            return new Notification.Builder(getApplicationContext(), PRIMARY_CHANNEL)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setSmallIcon(getSmallIcon())
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setContentIntent(pIntent);
        }
    }


    /**
     * Send a notification.
     *
     * @param id The ID of the notification
     * @param notification The notification object
     */
    public void notify(int id, Notification.Builder notification) {
        getManager().notify(id, notification.build());
    }

    /**
     * Get the small icon for this app
     *
     * @return The small icon resource id
     */
    private int getSmallIcon() {
        return R.drawable.ic_accessibility_white_24dp;
    }

    /**
     * Get the notification manager.
     *
     * Utility method as this helper works with it a lot.
     *
     * @return The system service NotificationManager
     */
    private NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }
}