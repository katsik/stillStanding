package com.sleepycookie.stillstanding.UI;

import android.arch.persistence.room.Room;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.sleepycookie.stillstanding.IncidentAdapter;
import com.sleepycookie.stillstanding.R;
import com.sleepycookie.stillstanding.data.AppDatabase;
import com.sleepycookie.stillstanding.data.Incident;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by geotsam on 30/01/2018.
 * This activity shows a list of the fall incidents.
 */

public class IncidentHistory extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        //TODO async this
        AppDatabase db = AppDatabase.getInstance(this);

        ArrayList<Incident> incidents = new ArrayList<Incident>();
        Collections.addAll(incidents, db.incidentDao().loadAllIncidents());

        IncidentAdapter adapter = new IncidentAdapter(this, incidents);

        ListView listView = (ListView) findViewById(R.id.history);

        listView.setAdapter(adapter);
    }
}
