package com.sleepycookie.stillstanding.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.widget.Toast;

/**
 * Created by geotsam on 20/02/2018.
 */

public final class MainHelper {

    /**
     * Removes all space, "-", "/" characters from input string
     *
     * @param input
     * @return
     */
    public static String removeClutter(String input) {
        input = input.replaceAll("-", "");
        input = input.replaceAll("/","");
        return input.replaceAll(" ", "");
    }

    /**
     * Returns the URI of the contact's photo. It checks of there is an image associated with the
     * URI, and if there is none, it returns null.
     *
     * @param contactId
     * @return
     */
    public static Uri getPhotoUri(long contactId, Context context) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);

        // TODO get high quality image if available
        // Thumbnail photo uri
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] {ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    return photoUri;
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }

    /**
     * Use this to prevent multiple Toasts spamming the UI
     *
     * @param message
     */
    public static void showAToast(String message, Context context, Toast mToast){
        if(mToast != null){
            mToast.cancel();
        }
        mToast = Toast.makeText(context,message,Toast.LENGTH_LONG);
        mToast.show();
    }
}
