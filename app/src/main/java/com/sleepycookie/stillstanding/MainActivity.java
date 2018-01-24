package com.sleepycookie.stillstanding;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.sleepycookie.stillstanding.data.StillStandingPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton startDetection;
    ImageButton phoneContactsButton;
    TextView emergencyNumber;
    TextView emergencyContact;
    ImageView emergencyPhoto;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO in activity_main: Refine the UI

        checkForPermissions();

        startDetection = (FloatingActionButton) findViewById(R.id.start_detection);

        startDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent readData = new Intent(MainActivity.this, ReadDataFromAccelerometer.class);
                startActivity(readData);
            }
        });

        phoneContactsButton = (ImageButton) findViewById(R.id.set_contact);
        emergencyContact = (TextView) findViewById(R.id.contact_name);
        emergencyNumber = (TextView) findViewById(R.id.contact_phone);
        emergencyPhoto = (ImageView) findViewById(R.id.contact_image);

        /**
         * Show the saved preferences or the placeholder text if there is nothing saved.
         */

        SharedPreferences sharedPrefName = getSharedPreferences("PREF_NAME", Context.MODE_PRIVATE);
        String mName = sharedPrefName.getString(getString(R.string.emergency_name), null);
        SharedPreferences sharedPrefPhone = getSharedPreferences("PREF_PHONE", Context.MODE_PRIVATE);
        String mNumber = sharedPrefPhone.getString(getString(R.string.emergency_number), null);
        SharedPreferences sharedPrefPhoto = getSharedPreferences("PREF_PHOTO", Context.MODE_PRIVATE);
        String mPhoto = sharedPrefPhoto.getString(getString(R.string.emergency_photo), null);

        if (mName != null) emergencyContact.setText(mName);
        if (mNumber != null) emergencyNumber.setText(mNumber);
        if (mPhoto != null) {
            emergencyPhoto.setVisibility(View.VISIBLE);
            emergencyPhoto.setImageURI(Uri.parse(mPhoto));
        }
        else emergencyPhoto.setVisibility(View.GONE);

        phoneContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // The below two lines is needed to open the contact list of  mobile
                Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(contactPickerIntent, 1);

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_bar_menu, menu);
        return true;
    }

    void checkForPermissions() {
        int MY_PERMISSIONS_REQUEST_ALL = 1;

        String[] PERMISSIONS = {Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE};

        if (forbiddenToCallOrReadContacts(this)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, MY_PERMISSIONS_REQUEST_ALL);
        }
    }

    /**
     * Used to check for permissions to access contacts or call phone
     * @param context
     *
     * @return
     * true if either one of the two permissions is NOT granted
     * false in any other case
     */
    private boolean forbiddenToCallOrReadContacts(Context context){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
            return true;
        }else if(ContextCompat.checkSelfPermission(context,Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                // User chose the "About" item, show the app "about" UI...
                Intent seeInfo = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(seeInfo);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * This method prints in the log the stored values for emergency contact/phone.
     * FOR DEBUGGING PURPOSES
     */

    public void checkStoredValues() {
        SharedPreferences sharedPrefName = getSharedPreferences("PREF_NAME", Context.MODE_PRIVATE);
        String mName = sharedPrefName.getString(getString(R.string.emergency_name), null);
        SharedPreferences sharedPrefPhone = getSharedPreferences("PREF_PHONE", Context.MODE_PRIVATE);
        String mNumber = sharedPrefPhone.getString(getString(R.string.emergency_number), null);

        Log.e("name:", "n - " + mName);
        Log.e("phone:", "p - " + mNumber);
    }

    /**
     * Method triggered after clicking "Pick a contact". It shows the user the contact list UI,
     * depending on their phone, and they can pick a desired contact. After that there is a dialog
     * that lets them pick one of the phone numbers from that contact, which is then saved for
     * future use. The dialog is shown even if there is only one number stored.
     * Issues:
     * - contacts with no phone number are shown in the list
     * This piece of code was found in parts on stackoverflow.
     *
     * @param reqCode
     * @param resultCode
     * @param data
     */

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (1):
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor c = getContentResolver().query(contactData, null, null, null, null);
                    if (c.moveToFirst()) {
                        String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                        String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                        String no = "";

                        Cursor phoneCur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);


                        //Gets contact photo URI
                        Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long
                                .parseLong(id));
                        final Uri photoUri = getPhoto(Long.parseLong(id));


                        String phoneNumber = "";

                        //Removes duplicate numbers
                        //TODO check if it does that consistently
                        List<String> allNumbers = new ArrayList<String>();
                        int phoneIdx = 0;
                        //TODO check why some contacts show no numbers while they have.
                        while (phoneCur.moveToNext()) {
                            no = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            int type = phoneCur.getInt(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                            switch (type) {
                                case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                                    // do something with the Home number here...
                                    Log.v("Home", name + ": " + no);
                                    no = removeClutter(no);
                                    allNumbers.add(no);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                                    // do something with the Mobile number here...
                                    Log.v("Mobile", name + ": " + no);
                                    no = removeClutter(no);
                                    allNumbers.add(no);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                                    // do something with the Work number here...
                                    Log.v("Work", name + ": " + no);
                                    no = removeClutter(no);
                                    allNumbers.add(no);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
                                    // do something with the Work number here...
                                    Log.v("Other", name + ": " + no);
                                    no = removeClutter(no);
                                    allNumbers.add(no);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM:
                                    // do something with the Work number here...
                                    Log.v("Custom", name + ": " + no);
                                    no = removeClutter(no);
                                    allNumbers.add(no);
                                    break;
                            }
                        }

                        StillStandingPreferences.setSafetyContactName(name);


                        //removes duplicates from list
                        allNumbers = new ArrayList<>(new HashSet<>(allNumbers));

                        final CharSequence[] items = allNumbers.toArray(new String[allNumbers.size()]);
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Choose a number");
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                String selectedNumber = items[item].toString();
                                selectedNumber = selectedNumber.replace("-", "");
                                Log.v("Selected Number:", selectedNumber);

                                SharedPreferences sharedPrefName = getSharedPreferences("PREF_NAME", Context.MODE_PRIVATE);
                                SharedPreferences sharedPrefPhone = getSharedPreferences("PREF_PHONE", Context.MODE_PRIVATE);
                                SharedPreferences sharedPrefPhoto = getSharedPreferences("PREF_PHOTO", Context.MODE_PRIVATE);

                                SharedPreferences.Editor editorN = sharedPrefName.edit();
                                SharedPreferences.Editor editorP = sharedPrefPhone.edit();
                                SharedPreferences.Editor editorPhoto = sharedPrefPhoto.edit();

                                editorP.putString(getString(R.string.emergency_number), selectedNumber);
                                editorP.commit();
                                editorN.putString(getString(R.string.emergency_name), StillStandingPreferences.getSafetyContactName());
                                editorN.commit();

                                emergencyNumber.setText(selectedNumber);
                                emergencyContact.setText(StillStandingPreferences.getSafetyContactName());

                                if (photoUri != null) {
                                    editorPhoto.putString(getString(R.string.emergency_photo), photoUri.toString());
                                    editorPhoto.commit();
                                    emergencyPhoto.setVisibility(View.VISIBLE);
                                    emergencyPhoto.setImageURI(photoUri);
                                    Log.e("Photo URI", photoUri.toString());
                                }
                                else{
                                    emergencyPhoto.setVisibility(View.GONE);
                                    editorPhoto.putString(getString(R.string.emergency_photo), null);
                                    editorPhoto.commit();
                                }
                            }
                        });
                        AlertDialog alert = builder.create();
                        if (allNumbers.size() > 0) {
                            alert.show();
                        } else {
                            String selectedNumber = phoneNumber.toString();
                            selectedNumber = selectedNumber.replace("-", "");
                            Log.e("Sel:", selectedNumber);
                        }

                        if (phoneNumber.length() == 0) {
                            //no numbers found actions
                            Log.v("Phone Number", "None");
                        }
                    }
                }
                break;
        }
    }

    /**
     * Removes all space, "-", "/" characters from input string
     *
     * @param input
     * @return
     */
    private static String removeClutter(String input) {
        input = input.replaceAll("-", "");
        input = input.replaceAll("/","");
        return input.replaceAll(" ", "");
    }

    public Uri getPhoto(long contactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);

        //TODO get high quality image if available
        //Thumbnail photo uri
        Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = getContentResolver().query(photoUri,
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
}