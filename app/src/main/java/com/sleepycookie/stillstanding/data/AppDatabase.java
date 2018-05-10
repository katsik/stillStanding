package com.sleepycookie.stillstanding.data;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;

/**
 * Created by geotsam on 30/01/2018.
 */

@Database(entities = {Incident.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract IncidentDao incidentDao();

    /** The only instance */
    private static AppDatabase dbInstance;

    /**
     * Gets the singleton instance of SampleDatabase.
     *
     * @param context The context.
     * @return The singleton instance of SampleDatabase.
     */
    public static synchronized AppDatabase getInstance(Context context) {

        if (dbInstance == null) {
            dbInstance = Room
                    .databaseBuilder(context.getApplicationContext(), AppDatabase.class, "incidents-database")
                    .allowMainThreadQueries()
                    .build();
        }
        return dbInstance;
    }
}
