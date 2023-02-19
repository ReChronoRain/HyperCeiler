package com.sevtinge.cemiuiler.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.sevtinge.cemiuiler.utils.Helpers;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SharedPrefsProvider extends ContentProvider {

    public static final String AUTHORITY = "com.sevtinge.cemiuiler.provider.sharedprefs";
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    SharedPreferences prefs;

    static {
        uriMatcher.addURI(AUTHORITY, "string/*/", 0);
        uriMatcher.addURI(AUTHORITY, "string/*/*", 1);
        uriMatcher.addURI(AUTHORITY, "integer/*/*", 2);
        uriMatcher.addURI(AUTHORITY, "boolean/*/*", 3);
        uriMatcher.addURI(AUTHORITY, "stringset/*", 4);
        uriMatcher.addURI(AUTHORITY, "test/*", 5);
        uriMatcher.addURI(AUTHORITY, "shortcut_icon/*", 6);
    }

    @Override
    public boolean onCreate() {
        try {
            prefs = PrefsUtils.getSharedPrefs(getContext(), true);
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        List<String> parts = uri.getPathSegments();
        //Log.e("parts", String.valueOf(parts));
        MatrixCursor cursor = new MatrixCursor(new String[]{"data"});

        switch (uriMatcher.match(uri)) {
            case 0: {
                cursor.newRow().add("data", prefs.getString(parts.get(1), ""));
                return cursor;
            }
            case 1: {
                cursor.newRow().add("data", prefs.getString(parts.get(1), parts.get(2)));
                return cursor;
            }
            case 2: {
                cursor.newRow().add("data", prefs.getInt(parts.get(1), Integer.parseInt(parts.get(2))));
                return cursor;
            }
            case 3: {
                cursor.newRow().add("data", prefs.getBoolean(parts.get(1), Integer.parseInt(parts.get(2)) == 1) ? 1 : 0);
                return cursor;
            }
            case 4: {
                Set<String> strings = prefs.getStringSet(parts.get(1), new LinkedHashSet<String>());
                if (strings != null)
                    for (String str: strings) cursor.newRow().add("data", str);
                return cursor;
            }
        }
        return null;
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        if (getContext() == null) return null;

        List<String> parts = uri.getPathSegments();
        if (uriMatcher.match(uri) == 5) {
            String filename = null;
            if ("0".equals(parts.get(1))) filename = "test0.png";
            else if ("1".equals(parts.get(1))) filename = "test1.mp3";
            else if ("2".equals(parts.get(1))) filename = "test2.mp4";
            else if ("3".equals(parts.get(1)) || "5".equals(parts.get(1))) filename = "test3.txt";
            else if ("4".equals(parts.get(1))) filename = "test4.zip";

            AssetFileDescriptor afd = null;
            if (filename != null) try {
                afd = getContext().getAssets().openFd(filename);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return afd;
        } else if (uriMatcher.match(uri) == 6) {
            Context context = Helpers.getProtectedContext(getContext());
            File file = new File(context.getFilesDir() + "/shortcuts/" + parts.get(1) + "_shortcut.png");
            if (!file.exists()) return null;
            return new AssetFileDescriptor(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
        }

        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

}