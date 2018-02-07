package com.sleepycookie.stillstanding.data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.ArrayList;


/**
 * Created by geotsam on 30/01/2018.
 */

@Dao
public interface IncidentDao {
    @Insert
    void insertIncidents(Incident... incidents);

    @Insert
    void insertIncidents(ArrayList<Incident> incidents);

    @Delete
    void deleteIncidents(Incident... incidents);

    @Query("SELECT * FROM incidents ORDER BY id DESC")
    Incident[] loadAllIncidents();

    @Query("SELECT * FROM incidents WHERE date > Date (:minDate)")
    Incident[] loadAllIncidentsMoreRecentThan(String minDate);

    @Query("SELECT * FROM incidents ORDER BY id DESC LIMIT 1")
    Incident loadLastIncident();
}
