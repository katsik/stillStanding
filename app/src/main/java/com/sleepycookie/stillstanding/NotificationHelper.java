package com.sleepycookie.stillstanding;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


/**
 * Created by geotsam on 17/02/2018.
 *
 * Helper class to manage notification channels and create notifications.
 */
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            NotificationChannel chan1 = new NotificationChannel(PRIMARY_CHANNEL,
                    getString(R.string.noti_channel_default), NotificationManager.IMPORTANCE_LOW);
            chan1.setLightColor(Color.GREEN);
            chan1.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getManager().createNotificationChannel(chan1);
        }
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
    public NotificationCompat.Builder getNotification1(String title, String body, PendingIntent pIntent) {
            return new NotificationCompat.Builder(getApplicationContext(), PRIMARY_CHANNEL)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setSmallIcon(getSmallIcon())
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setColor(getResources().getColor(R.color.colorPrimary))
                    .setContentIntent(pIntent);
    }


    /**
     * Send a notification.
     *
     * @param id The ID of the notification
     * @param notification The notification object
     */
    public void notify(int id, NotificationCompat.Builder notification) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(id, notification.build());
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