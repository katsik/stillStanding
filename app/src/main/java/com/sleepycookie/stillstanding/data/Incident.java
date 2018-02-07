package com.sleepycookie.stillstanding.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.location.Location;

import com.sleepycookie.stillstanding.R;
import com.sleepycookie.stillstanding.utils.DateTypeConverter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by geotsam on 30/01/2018.
 */

@Entity(tableName = "incidents")
@TypeConverters({DateTypeConverter.class})
public class Incident {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "date")
    public Date date;

    @ColumnInfo(name = "info")
    public String info;

    @ColumnInfo(name = "type") // 1 = call, 2 = sms, 3 = alarm
    public int type;


    /*
    @ColumnInfo (name = "location")
    public Location location;
    */

    public Incident(Date date, String info, int type) {
        this.date = date;
        this.info = info;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getInfo() { return info; }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getIcon() {
        if (type == 1) {
            return R.drawable.ic_phone_white_24dp;
        } else if (type == 2) {
            return R.drawable.ic_message_white_24dp;
        } else {
            return R.drawable.ic_siren_white_24dp;
        }
    }

    public String getDateText(){
        DateFormat df = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss");
        return df.format(date);
    }
}
