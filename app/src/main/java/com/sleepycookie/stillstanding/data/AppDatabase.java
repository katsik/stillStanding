package com.sleepycookie.stillstanding.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

/**
 * Created by geotsam on 30/01/2018.
 */

@Database(entities = {Incident.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract IncidentDao incidentDao();
}
