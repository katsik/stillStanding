package com.sleepycookie.stillstanding.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.sleepycookie.stillstanding.R;

/**
 * Created by geotsam on 01/02/2018.
 */

public class Alarm {
    private static SoundPool pool = null;
    private static int id = -1;

    @SuppressWarnings("deprecation")
    public static void siren(Context context) {
        if (null == pool) {
            pool = new SoundPool(5, AudioManager.STREAM_ALARM, 0);
        }
        if (-1 == id) {
            id = pool.load(context.getApplicationContext(), R.raw.alarm, 1);
        }
        loudest(context);
        pool.play(id, 1.0f, 1.0f, 1, 3, 1.0f);
    }

    public static void loudest(Context context) {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int loudest = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        manager.setStreamVolume(AudioManager.STREAM_ALARM, loudest, 0);
    }

//    public static void call(Context context) {
//        String contact = Contact.get(context);
//        if (contact != null && !"".equals(contact)) {
//            Toast.makeText(context, "Calling guardian's phone number for help", Toast.LENGTH_SHORT).show();
//            Intent call = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contact));
//            call.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(call);
//            Telephony.handsfree(context);
//        } else {
//            Toast.makeText(context, "Please enter guardian's phone number in the settings", Toast.LENGTH_SHORT).show();
//            siren(context);
//        }
//    }
}
