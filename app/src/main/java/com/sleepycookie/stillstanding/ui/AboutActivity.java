package com.sleepycookie.stillstanding.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.sleepycookie.stillstanding.Person;
import com.sleepycookie.stillstanding.R;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by geotsam on 23/01/2018.
 * This activity shows info about the app. Right now it shows a list with some info about us.
 */

public class AboutActivity extends AppCompatActivity {
    public int pass;
    protected void onCreate(Bundle savedInstanceState) {
        pass = 0;
        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        final ArrayList<Person> people = new ArrayList<Person>();
        people.add(new Person("Giorgos Tsamis",
                "geotsam@gmail.com",
                "github.com/geotsam",
                R.drawable.ic_tsamis));
        people.add(new Person("Giorgos Katsikopoulos",
                "giorgoskatsikopoulos@gmail.com",
                "github.com/katsik",
                R.drawable.ic_katsikopoulos));
        people.add(new Person("Stathis Bozikas",
                "stathis.bozikas@gmail.com",
                "github.com/AnonymousHyena",
                R.drawable.ic_bozikas));

        PersonAdapter adapter = new PersonAdapter(this, people);

        ListView listView = findViewById(R.id.list);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                pass = (pass * 10) + position+1;
                if (pass == 1312){
                    Calendar rightNow = Calendar.getInstance();
                    int month = rightNow.get(Calendar.MONTH);
                    if ((month>8) && (month <12)){
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.true_fall_detection_y), Toast.LENGTH_LONG)
                                .show();
                    }
                    else{
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.true_fall_detection_n), Toast.LENGTH_LONG)
                                .show();
                    }
                }
            }
        });

    }
}
