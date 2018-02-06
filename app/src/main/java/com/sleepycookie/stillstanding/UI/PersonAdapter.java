package com.sleepycookie.stillstanding.UI;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sleepycookie.stillstanding.Person;
import com.sleepycookie.stillstanding.R;

import java.util.ArrayList;

/**
 * Created by geotsam on 23/01/2018.
 */

public class PersonAdapter extends ArrayAdapter<Person> {
    public PersonAdapter(Activity context, ArrayList<Person> people){
        super(context, 0, people);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView=convertView;
        if(listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_person, parent, false);
        }

        Person currentPerson = getItem(position);

        ImageView imageView = listItemView.findViewById(R.id.person_image);
        imageView.setImageResource(currentPerson.getImageID());

        TextView nameView = listItemView.findViewById(R.id.person_name);
        nameView.setText(currentPerson.getName());

        TextView mailView = listItemView.findViewById(R.id.person_mail);
        mailView.setText(currentPerson.getMail());

        TextView websiteView = listItemView.findViewById(R.id.person_website);
        websiteView.setText(currentPerson.getWebsite());

        return listItemView;
    }
}
