package com.sleepycookie.stillstanding.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

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

        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        AppDatabase db = AppDatabase.getInstance(this);

        final ArrayList<Incident> incidents = new ArrayList<Incident>();
        Collections.addAll(incidents, db.incidentDao().loadAllIncidents());

        IncidentAdapter adapter = new IncidentAdapter(this, incidents);

        ListView listView = (ListView) findViewById(R.id.history);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                long viewId = view.getId();

                if (viewId == R.id.incident_location) {
                    Incident tempIncident = incidents.get(position);
                    StringBuffer url = new StringBuffer();
                    url.append( "http://maps.google.com?q=");
                    url.append(String.format ("%.7f", tempIncident.getLatitude()));
                    url.append(",");
                    url.append(String.format ("%.7f", tempIncident.getLongitude()));
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url.toString()));
                    startActivity(i);
                }
                else if (viewId == R.id.list_item){

                }
            }
        });
    }
}
