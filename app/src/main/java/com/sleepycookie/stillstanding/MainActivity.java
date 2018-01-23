package com.sleepycookie.stillstanding;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton startDetection;
    Button setContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO in activity_main: Refine the UI

        //checkStoredValues();

        checkForPermissions();

        startDetection = (FloatingActionButton) findViewById(R.id.start_detection);

        startDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent readData = new Intent(MainActivity.this, ReadDataFromAccelerometer.class);
                startActivity(readData);
            }
        });

        setContact = (Button) findViewById(R.id.set_contact);

        setContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent setEmergencyContact = new Intent(MainActivity.this, PreferencesScreen.class);
                startActivity(setEmergencyContact);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_bar_menu, menu);
        return true;
    }

    void checkForPermissions(){
        int MY_PERMISSIONS_REQUEST_CALL_PHONE = 1;
        int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 2;


        //TODO check why sometimes only phone permissions are asked
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},MY_PERMISSIONS_REQUEST_CALL_PHONE);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                Intent setEmergencyContact = new Intent(MainActivity.this, PreferencesScreen.class);
                startActivity(setEmergencyContact);
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

    public void checkStoredValues(){
        SharedPreferences sharedPrefName = getSharedPreferences("PREF_NAME", Context.MODE_PRIVATE);
        String mName = sharedPrefName.getString(getString(R.string.emergency_name), null);
        SharedPreferences sharedPrefPhone = getSharedPreferences("PREF_PHONE", Context.MODE_PRIVATE);
        String mNumber = sharedPrefPhone.getString(getString(R.string.emergency_number), null);

        Log.e("name:", "n - " + mName);
        Log.e("phone:", "p - " + mNumber);
    }
}
