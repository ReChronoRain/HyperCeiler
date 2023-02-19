package com.sevtinge.cemiuiler.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.sevtinge.cemiuiler.utils.ToastHelper;

import java.io.File;

public class SQLiteHelper extends SQLiteOpenHelper {

    public static SQLiteDatabase a = null;

    public SQLiteHelper(@Nullable Context context) {
        super(context, a(context), null, 1);
        try {
            a = getWritableDatabase();
            a.execSQL("create table if not exists location(id integer primary key autoincrement,title text,lng real,lat real,offset integer,lac integer,cid integer,note text)");
        } catch (SQLiteException e) {
            ToastHelper.makeText(context, e.getMessage());
        }

    }

    private static String a(Context context) {
        String str = context.getExternalFilesDir(null) + "/location.db";
        return new File(str).exists() ? str : "location.db";
    }

    public int a(LocationData data) {
        return a.delete("location", "id = ?", new String[]{String.valueOf(data.getF())});
    }


    public long b(LocationData data) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", data.getTitle());
        contentValues.put("lat", data.getLatitude());
        contentValues.put("lng", data.getLongitude());
        contentValues.put("offset", Integer.valueOf(data.getOffset()));
        contentValues.put("lac", Integer.valueOf(data.getBaseStationCode()));
        contentValues.put("cid", Integer.valueOf(data.getRegionCode()));
        contentValues.put("note", data.getRemarks());
        return a.insert("location", null, contentValues);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists location(id integer primary key autoincrement,title text,lng real,lat real,offset integer,lac integer,cid integer,note text)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
