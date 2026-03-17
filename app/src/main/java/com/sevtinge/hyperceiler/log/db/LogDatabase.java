package com.sevtinge.hyperceiler.log.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {LogEntry.class}, version = 1, exportSchema = false)
public abstract class LogDatabase extends RoomDatabase {
    public abstract LogDao logDao();
}
