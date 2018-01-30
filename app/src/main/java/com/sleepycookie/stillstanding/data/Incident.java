package com.sleepycookie.stillstanding.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.location.Location;

import com.sleepycookie.stillstanding.utils.DateTypeConverter;

import java.util.Date;

/**
 * Created by geotsam on 30/01/2018.
 */

@Entity (tableName = "incidents")
@TypeConverters({DateTypeConverter.class})
public class Incident {
    @PrimaryKey (autoGenerate = true)
    @ColumnInfo (name = "id")
    public int id;

    @ColumnInfo (name = "date")
    public Date date;

    @ColumnInfo (name = "response")
    public String response;

    /*
    @ColumnInfo (name = "location")
    public Location location;
    */

    @Ignore
    public Incident(int id, Date date, String response) {
        this.id = id;
        this.date = date;
        this.response = response;
    }

    public Incident( Date date, String response) {
        this.date = date;
        this.response = response;
    }

    @Ignore
    public int getId() {
        return id;
    }

    @Ignore
    public void setId(int id) {
        this.id = id;
    }

    @Ignore
    public Date getDate() {
        return date;
    }

    @Ignore
    public void setDate(Date date) {
        this.date = date;
    }

    @Ignore
    public String getResponse() {
        return response;
    }

    @Ignore
    public void setResponse(String response) {
        this.response = response;
    }
}
