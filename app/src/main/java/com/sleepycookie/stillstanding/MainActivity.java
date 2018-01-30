package com.sleepycookie.stillstanding;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.arch.persistence.room.Room;
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

import com.sleepycookie.stillstanding.data.AppDatabase;
import com.sleepycookie.stillstanding.data.Incident;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


//TODO clean & organize this activity
public class MainActivity extends AppCompatActivity
                          implements PickContactFragment.PickContactListener{

    FloatingActionButton startDetection;
    ImageButton phoneContactsButton;
    TextView emergencyNumber;
    TextView emergencyContact;
    ImageView emergencyPhoto;
    android.support.v7.widget.CardView contactCard;
    android.support.v7.widget.CardView incidentCard;
    String tempName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkForPermissions();

        startDetection = (FloatingActionButton) findViewById(R.id.start_detection);
        phoneContactsButton = (ImageButton) findViewById(R.id.set_contact);
        emergencyContact = (TextView) findViewById(R.id.contact_name);
        emergencyNumber = (TextView) findViewById(R.id.contact_phone);
        emergencyPhoto = (ImageView) findViewById(R.id.contact_image);
        contactCard = (android.support.v7.widget.CardView) findViewById(R.id.card_view);

        /**
         * Show the saved preferences or the placeholder text if there is nothing saved.
         * In case there is no saved number the color of the card changes to orange to grab attention.
         */

        SharedPreferences sharedPrefName = getSharedPreferences("PREF_NAME", Context.MODE_PRIVATE);
        String mName = sharedPrefName.getString(getString(R.string.emergency_name), null);
        SharedPreferences sharedPrefPhone = getSharedPreferences("PREF_PHONE", Context.MODE_PRIVATE);
        String mNumber = sharedPrefPhone.getString(getString(R.string.emergency_number), null);
        SharedPreferences sharedPrefPhoto = getSharedPreferences("PREF_PHOTO", Context.MODE_PRIVATE);
        String mPhoto = sharedPrefPhoto.getString(getString(R.string.emergency_photo), null);

        if (mName != null) emergencyContact.setText(mName);
        if (mNumber != null) {
            emergencyNumber.setText(mNumber);
            contactCard.setCardBackgroundColor(getResources().getColor(R.color.white));
            phoneContactsButton.setImageResource(R.drawable.ic_edit_black_24dp);
        }
        else{
            contactCard.setCardBackgroundColor(getResources().getColor(R.color.atterntionColor));
            phoneContactsButton.setImageResource(R.drawable.ic_person_add_black_24dp);
        }
        if (mPhoto != null) {
            emergencyPhoto.setVisibility(View.VISIBLE);
            emergencyPhoto.setImageURI(Uri.parse(mPhoto));
        }
        else emergencyPhoto.setVisibility(View.GONE);

        phoneContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // The two lines below are needed to open the contact list of  mobile
                Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(contactPickerIntent, 1);

            }
        });

        startDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getEmergencyNumber() == null){
                    triggerDialogBox();
                }else{
                    Intent readData = new Intent(MainActivity.this, ReadDataFromAccelerometer.class);
                    startActivity(readData);
                }
            }
        });

        setIncidentCard();
    }


    public String getEmergencyNumber(){
        SharedPreferences sharedPreferences = getSharedPreferences("PREF_PHONE", Context.MODE_PRIVATE);
        return sharedPreferences.getString(getString(R.string.emergency_number),null);
    }

    @Override
    public void onPickContactPositive() {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(contactPickerIntent, 1);
    }

    @Override
    public void onPickContactNegative() {
        //TODO not sure if any useful but added it anyway :)
    }

    public void triggerDialogBox(){
        PickContactFragment pickContact = new PickContactFragment();
        pickContact.show(getSupportFragmentManager(),"PickContactFragment");
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

            case R.id.action_history:
                // User chose the "History" item, show the app "history" UI...
                Intent seeHistory = new Intent(MainActivity.this, IncidentHistory.class);
                startActivity(seeHistory);
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

        Log.d("name:", "n - " + mName);
        Log.d("phone:", "p - " + mNumber);
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
                        String number = "";

                        Cursor phoneCur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);


                        //Gets contact photo URI
                        Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long
                                .parseLong(id));
                        final Uri photoUri = getPhotoUri(Long.parseLong(id));

                        String phoneNumber = "";

                        List<String> allNumbers = new ArrayList<String>();

                        while (phoneCur.moveToNext()) {
                            number = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            int type = phoneCur.getInt(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                            switch (type) {
                                case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                                    Log.v("Home", name + ": " + number);
                                    number = removeClutter(number);
                                    allNumbers.add(number);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                                    Log.v("Mobile", name + ": " + number);
                                    number = removeClutter(number);
                                    allNumbers.add(number);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                                    Log.v("Work", name + ": " + number);
                                    number = removeClutter(number);
                                    allNumbers.add(number);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
                                    Log.v("Other", name + ": " + number);
                                    number = removeClutter(number);
                                    allNumbers.add(number);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM:
                                    Log.v("Custom", name + ": " + number);
                                    number = removeClutter(number);
                                    allNumbers.add(number);
                                    break;
                            }
                        }

                        tempName=name;

                        //Removes duplicate numbers
                        //TODO check if it does that consistently
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
                                editorN.putString(getString(R.string.emergency_name), tempName);
                                editorN.commit();

                                emergencyNumber.setText(selectedNumber);
                                emergencyContact.setText(tempName);
                                contactCard.setCardBackgroundColor(getResources().getColor(R.color.white));
                                phoneContactsButton.setImageResource(R.drawable.ic_edit_black_24dp);


                                if (photoUri != null) {
                                    editorPhoto.putString(getString(R.string.emergency_photo), photoUri.toString());
                                    editorPhoto.commit();
                                    emergencyPhoto.setVisibility(View.VISIBLE);
                                    emergencyPhoto.setImageURI(photoUri);
                                    Log.v("Photo URI", photoUri.toString());
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
                            Log.v("Sel:", selectedNumber);
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

    public Uri getPhotoUri(long contactId) {
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

    //TODO run this on onResume() maybe? (when you get back from the readData activity it does not refresh)
    public void setIncidentCard(){
        incidentCard = (android.support.v7.widget.CardView) findViewById(R.id.incident_card);


        //TODO async this
        AppDatabase.Builder builder = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "database-name");
        builder.allowMainThreadQueries();
        AppDatabase db = (AppDatabase) builder.build();

        Incident lastIncident = db.incidentDao().loadLastIncident();

        if (lastIncident != null){
            incidentCard.setVisibility(View.VISIBLE);
            DateFormat df = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss");

            TextView incidentDate = (TextView) findViewById(R.id.incident_card_date);
            incidentDate.setText(df.format(lastIncident.getDate()));

            TextView incidentResponse = (TextView) findViewById(R.id.incident_card_response);
            incidentResponse.setText(lastIncident.getResponse());
        }
    }
}