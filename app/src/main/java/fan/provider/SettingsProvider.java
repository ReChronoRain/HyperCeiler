package fan.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class SettingsProvider extends ContentProvider {
    private static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
    private SQLiteOpenHelper helper;

    static {
        matcher.addURI(Settings.AUTHORITY, "global", 1);
        matcher.addURI(Settings.AUTHORITY, "secure", 2);
        matcher.addURI(Settings.AUTHORITY, "system", 3);

        // 匹配具体 Key 级：content://fan.provider.settings/global/*
        // 注意：这里匹配成功后依然返回同样的 code (1, 2, 3)，因为逻辑上它们还是指向同一张表
        matcher.addURI(Settings.AUTHORITY, "global/*", 1);
        matcher.addURI(Settings.AUTHORITY, "secure/*", 2);
        matcher.addURI(Settings.AUTHORITY, "system/*", 3);
    }

    @Override
    public boolean onCreate() {
        helper = new SQLiteOpenHelper(getContext(), "settings.db", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE global (_id INTEGER PRIMARY KEY, name TEXT UNIQUE, value TEXT)");
                db.execSQL("CREATE TABLE secure (_id INTEGER PRIMARY KEY, name TEXT UNIQUE, value TEXT)");
                db.execSQL("CREATE TABLE system (_id INTEGER PRIMARY KEY, name TEXT UNIQUE, value TEXT)");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int old, int n) {
            }
        };
        return true;
    }

    private void checkPerm(String p) {
        if (getContext().checkCallingOrSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("No permission to access FanSettings: " + p);
        }
    }

    @Override
    public Cursor query(Uri u, String[] p, String s, String[] a, String so) {
        checkPerm(Settings.PERMISSION_READ);
        return helper.getReadableDatabase().query(getTable(u), p, s, a, null, null, so);
    }

    @Override
    public Uri insert(Uri u, ContentValues v) {
        checkPerm(Settings.PERMISSION_WRITE);
        helper.getWritableDatabase().insertWithOnConflict(getTable(u), null, v, SQLiteDatabase.CONFLICT_REPLACE);
        getContext().getContentResolver().notifyChange(u, null);
        getContext().getContentResolver().notifyChange(Uri.withAppendedPath(u, v.getAsString("name")), null);
        return u;
    }

    private String getTable(Uri u) {
        int match = matcher.match(u);
        return switch (match) {
            case 1 -> "global";
            case 2 -> "secure";
            case 3 -> "system";
            // 打印出问题的 URI 方便调试
            default -> throw new IllegalArgumentException("Unknown or Invalid URI: " + u);
        };
    }

    @Override
    public int delete(Uri u, String s, String[] a) {
        checkPerm(Settings.PERMISSION_WRITE);
        return 0;
    }

    @Override
    public int update(Uri u, ContentValues v, String s, String[] a) {
        return 0;
    }

    @Override
    public String getType(Uri u) {
        return null;
    }
}
