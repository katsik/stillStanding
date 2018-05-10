package com.sleepycookie.stillstanding.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

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
