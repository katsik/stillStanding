package com.sleepycookie.stillstanding.ui;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.sleepycookie.stillstanding.R;
import com.sleepycookie.stillstanding.data.Incident;

import java.util.ArrayList;

/**
 * Created by geotsam on 30/01/2018.
 */

public class IncidentAdapter extends ArrayAdapter<Incident>{
    public IncidentAdapter(Activity context, ArrayList<Incident> incidents){
        super(context, 0, incidents);
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull final ViewGroup parent) {
        View listItemView=convertView;
        if(listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_incident, parent, false);
        }

        Incident currentIncident = getItem(position);

        TextView idView = listItemView.findViewById(R.id.incident_id);
        idView.setText("Incident " + currentIncident.getId());

        TextView dateView = listItemView.findViewById(R.id.incident_date);
        dateView.setText(currentIncident.getDateText());

        TextView infoView = listItemView.findViewById(R.id.incident_info);
        infoView.setText(currentIncident.getInfo());

        ImageView imageView = listItemView.findViewById(R.id.incident_item_image);
        imageView.setImageResource(currentIncident.getIcon());

        ImageButton imageButton = listItemView.findViewById(R.id.incident_location);
        if(currentIncident.hasLocation() == false) {
            imageButton.setVisibility(View.GONE);
        }
        else{
            imageButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    ((ListView) parent).performItemClick(v, position, 0); // Let the event be handled in onItemClick()
                }
            });
        }

        return listItemView;
    }
}
