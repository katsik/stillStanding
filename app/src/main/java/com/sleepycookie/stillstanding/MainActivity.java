package com.sleepycookie.stillstanding;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button startDetection;
    Button setContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int MY_PERMISSIONS_REQUEST_CALL_PHONE = 1;
        int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 2;

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

        startDetection = (Button) findViewById(R.id.start_detection);

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
}
