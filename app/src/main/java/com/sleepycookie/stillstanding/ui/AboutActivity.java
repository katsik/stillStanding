package com.sleepycookie.stillstanding.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.sleepycookie.stillstanding.Person;
import com.sleepycookie.stillstanding.R;

import java.util.ArrayList;

/**
 * Created by geotsam on 23/01/2018.
 * This activity shows info about the app. Right now it shows a list with some info about us.
 */

public class AboutActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {

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
    }
}
