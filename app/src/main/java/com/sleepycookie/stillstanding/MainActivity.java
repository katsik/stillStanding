package com.sleepycookie.stillstanding;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent readData = new Intent(this,ReadDataFromAccelerometer.class);
        startActivity(readData);
    }
}
