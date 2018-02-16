package com.sleepycookie.stillstanding.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
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
import android.widget.Toast;

import com.sleepycookie.stillstanding.PickContactFragment;
import com.sleepycookie.stillstanding.R;
import com.sleepycookie.stillstanding.data.AppDatabase;
import com.sleepycookie.stillstanding.data.Incident;
import com.sleepycookie.stillstanding.data.Preferences;
import com.sleepycookie.stillstanding.utils.PermssionsManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


//TODO clean & organize this activity
public class MainActivity extends AppCompatActivity
                          implements PickContactFragment.PickContactListener {

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

        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Sets preferences (from settings UI) to the default values, unless the user has changed them.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Check if we need to display our Intro Activity
        if (!Preferences.getIntroPref(this)) {
            // The user hasn't seen the Intro yet, so show it
            startActivity(new Intent(this, IntroActivity.class));
            finish();
        }

        startDetection = findViewById(R.id.start_detection);
        phoneContactsButton = findViewById(R.id.set_contact);
        emergencyContact = findViewById(R.id.contact_name);
        emergencyNumber = findViewById(R.id.contact_phone);
        emergencyPhoto = findViewById(R.id.contact_image);
        contactCard = findViewById(R.id.card_view);

        initContactButton();
        initFAB();
    }

    @Override
    public void onPickContactPositive() {
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
            Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(contactPickerIntent, 1);
        } else {
            new Toast(getApplicationContext()).makeText(MainActivity.this, "Accept permission first", Toast.LENGTH_SHORT).show();
        }
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

    /**
     * Used to check for permissions to access contacts or call phone or send SMS
     * @param context
     *
     * @return
     * true if either one of the three permissions is NOT granted
     * false in any other case
     */
    private boolean forbiddenToCallOrReadContacts(Context context){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
            return true;
        }else if(ContextCompat.checkSelfPermission(context,Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            return true;
        }else if(ContextCompat.checkSelfPermission(context,Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED){
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

            case R.id.action_settings:
                //User chose the "Settings" item, show the "settings" UI...
                Intent seeSettings = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(seeSettings);
                return true;

            case R.id.action_feedback:
                //User chose the "Feedback" item, go to email app...
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:sleepy.cookie.studios@gmail.com")); // only email apps should handle this
                intent.putExtra(Intent.EXTRA_SUBJECT, "[Still Standing] App Feedback");

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * Method triggered after clicking "Pick a contact". It shows the user the contact list UI,
     * depending on their phone, and they can pick a desired contact. After that there is a dialog
     * that lets them pick one of the phone numbers from that contact, which is then saved for
     * future use. The dialog is shown even if there is only one number stored.
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
                        String number;

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

                        tempName = name;

                        if (allNumbers.isEmpty()){
                            Toast.makeText(this, "This contact has no phone number", Toast.LENGTH_SHORT).show();
                            phoneCur.close();
                            break;
                        }

                        //Removes duplicate numbers
                        //TODO check if it does that consistently
                        allNumbers = new ArrayList<>(new HashSet<>(allNumbers));

                        final CharSequence[] items = allNumbers.toArray(new String[allNumbers.size()]);
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getString(R.string.choose_number));
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                String selectedNumber = items[item].toString();
                                selectedNumber = selectedNumber.replace("-", "");
                                Log.v("Selected Number:", selectedNumber);

                                Preferences.setNumber(MainActivity.this, selectedNumber);
                                Preferences.setContact(MainActivity.this, tempName);

                                if (photoUri != null) {
                                    Preferences.setPhoto(MainActivity.this, photoUri);
                                }
                                else{
                                    Preferences.setPhoto(MainActivity.this, null);
                                }
                                setContactCard();
                            }
                        });
                        AlertDialog alert = builder.create();
                        if (allNumbers.size() > 0) {
                            alert.show();
                        } else {
                            String selectedNumber = phoneNumber;
                            selectedNumber = selectedNumber.replace("-", "");
                            Log.v("Sel:", selectedNumber);
                        }

                        phoneCur.close();
                    }
                    c.close();
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


    /**
     * Returns the URI of the contact's photo. It checks of there is an image associated with the
     * URI, and if there is none, it returns null.
     *
     * @param contactId
     * @return
     */
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

    /**
     * This method is responsible for showing the last incident card in the UI. It puts the last fall's
     * data every time this screen is brought back. It hides the card if the database is empty.
     */
    public void setIncidentCard(){
        incidentCard = findViewById(R.id.incident_card);

        final Incident lastIncident = AppDatabase.getInstance(this).incidentDao().loadLastIncident();

        if (lastIncident != null){
            incidentCard.setVisibility(View.VISIBLE);

            TextView incidentDate = findViewById(R.id.incident_card_date);
            incidentDate.setText(lastIncident.getDateText());

            TextView incidentInfo = findViewById(R.id.incident_card_info);
            incidentInfo.setText(lastIncident.getInfo());

            ImageView incidentImage = findViewById(R.id.incident_image);
            incidentImage.setImageResource(lastIncident.getIcon());

            ImageButton incidentLocationButton = findViewById(R.id.incident_card_location);

            if(lastIncident.hasLocation() == false){
                incidentLocationButton.setVisibility(View.GONE);
            }
            else {
                incidentLocationButton.setVisibility(View.VISIBLE);
                incidentLocationButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // The two lines below are needed to open location
                        StringBuffer url = new StringBuffer();
                        url.append( "http://maps.google.com?q=");
                        url.append(String.format ("%.7f", lastIncident.getLatitude()).replaceAll(",", "."));
                        url.append(",");
                        url.append(String.format ("%.7f", lastIncident.getLongitude()).replaceAll(",", "."));
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url.toString()));
                        startActivity(i);
                    }
                });
            }

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setContactCard();
        setIncidentCard();
        // Check if to displayed our Intro Activity
        if (Preferences.getIntroPref(this)) {
            PermssionsManager.checkForPermissions(this, this);
        }
    }

    /**
     * Shows the saved preferences or the placeholder text (if there is nothing saved) in the contact card.
     * In case there is no saved number the color of the card changes to orange to grab attention.
     */
    private void setContactCard(){

        String mName = Preferences.getContact(this);
        String mNumber = Preferences.getNumber(this);
        String mPhoto = Preferences.getPhoto(this);

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
        else {
            emergencyPhoto.setVisibility(View.GONE);
        }
    }

    private void initContactButton(){
        phoneContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // The two lines below are needed to open the contact list of  mobile
                if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
                    Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                    startActivityForResult(contactPickerIntent, 1);
                } else {
                    new Toast(getApplicationContext()).makeText(MainActivity.this, "Accept permission first", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initFAB(){
        startDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Preferences.getNumber(MainActivity.this) == null){
                    triggerDialogBox();
                }else{
                    Intent readData = new Intent(MainActivity.this, ReadDataFromAccelerometer.class);
                    startActivity(readData);
                }
            }
        });
    }
}