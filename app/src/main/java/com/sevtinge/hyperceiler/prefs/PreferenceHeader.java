/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.prefs;

import static com.sevtinge.hyperceiler.BuildConfig.APPLICATION_ID;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getCurrentUserId;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getWhoAmI;
import static com.sevtinge.hyperceiler.utils.shell.ShellUtils.rootExecCmd;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteException;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.PackagesUtils;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import java.util.ArrayList;
import java.util.List;

public class PreferenceHeader extends XmlPreference {

    public static ArrayList<String> mUninstallApp = new ArrayList<>();
    public static ArrayList<String> mDisableOrHiddenApp = new ArrayList<>();
    public static ArrayList<String> mNoScoped = new ArrayList<>();

    public static List<String> scope = new ArrayList<String>();
    public static List<String> notInSelectedScope = new ArrayList<String>();

    private static boolean isScopeGet = false;
    private static boolean isScopeGetFailed = false;

    public PreferenceHeader(@NonNull Context context) {
        super(context);
        init(context);
    }

    public PreferenceHeader(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PreferenceHeader(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        setLayoutResource(R.layout.preference_header);
        if (!isScopeGet && !isScopeGetFailed) if (getWhoAmI().equals("root")) getScope();
        if (isUninstall(context)) {
            mUninstallApp.add(" - " + getTitle() + " (" + getSummary() + ")");
            setVisible(false);
        } else if (isDisable(context) || isHidden(context)) {
            mDisableOrHiddenApp.add(" - " + getTitle() + " (" + getSummary() + ")");
            setVisible(false);
        }
        if (!scope.contains(getSummary()) && (getSummary() != null) && isScopeGet) {
            notInSelectedScope.add((String) getSummary());
            String string = " - " + getTitle() + " (" + getSummary() + ")";
            if (!mDisableOrHiddenApp.contains(string) && !mUninstallApp.contains(string) && !mNoScoped.contains(string)) mNoScoped.add(string);
            setVisible(false);
        }
    }

    private boolean isUninstall(Context context) {
        if (getSummary() == null || "android".contentEquals(getSummary())) return false;
        return PackagesUtils.isUninstall(context, (String) getSummary());
    }

    private boolean isDisable(Context context) {
        if (getSummary() == null || "android".contentEquals(getSummary())) return false;
        return PackagesUtils.isDisable(context, (String) getSummary());
    }

    private boolean isHidden(Context context) {
        if (getSummary() == null || "android".contentEquals(getSummary())) return false;
        return PackagesUtils.isHidden(context, (String) getSummary());
    }

    @SuppressLint("Range")
    private void getScope() {
        int userId = getCurrentUserId();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            rootExecCmd("mkdir -p /data/local/tmp/HyperCeiler/cache/ && cp -r /data/adb/lspd/config /data/local/tmp/HyperCeiler/cache/ && chmod -R 777 /data/local/tmp/HyperCeiler/cache/config");

            String dbPath = "/data/local/tmp/HyperCeiler/cache/config/modules_config.db";
            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);

            if (isDatabaseLocked(db)) {
                XposedLogUtils.logW("Database locked, skip get scope.");
                isScopeGetFailed = true;
                return;
            }

            String tableName = "modules";
            String[] columns = {"mid"};
            String selection = "module_pkg_name = ?";
            String[] selectionArgs = {APPLICATION_ID};

            cursor = db.query(tableName, columns, selection, selectionArgs, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                List<String> scopeMid = new ArrayList<>();
                List<String> scopeUid = new ArrayList<>();

                do {
                    String mid = cursor.getString(cursor.getColumnIndex("mid"));

                    String scopeQuery = "SELECT app_pkg_name FROM scope WHERE mid = ?";
                    Cursor scopeCursor = db.rawQuery(scopeQuery, new String[]{mid});
                    if (scopeCursor != null && scopeCursor.moveToFirst()) {
                        do {
                            String getScope = scopeCursor.getString(scopeCursor.getColumnIndex("app_pkg_name"));
                            if ("system".equals(getScope)) getScope = "android";
                            scopeMid.add(getScope);
                        } while (scopeCursor.moveToNext());
                        scopeCursor.close();
                    }

                    String userScopeQuery = "SELECT app_pkg_name FROM scope WHERE user_id = ?";
                    Cursor userScopeCursor = db.rawQuery(userScopeQuery, new String[]{String.valueOf(userId)});
                    if (userScopeCursor != null && userScopeCursor.moveToFirst()) {
                        do {
                            String getScope = userScopeCursor.getString(userScopeCursor.getColumnIndex("app_pkg_name"));
                            if ("system".equals(getScope)) getScope = "android";
                            scopeUid.add(getScope);
                        } while (userScopeCursor.moveToNext());
                        userScopeCursor.close();
                    }

                    scope = new ArrayList<>(scopeMid);
                    scope.retainAll(scopeUid);

                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            isScopeGetFailed = true;
            XposedLogUtils.logE("Database error: ", e);
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        isScopeGet = true;
    }

    private boolean isDatabaseLocked(SQLiteDatabase db) {
        try {
            Cursor cursor = db.rawQuery("SELECT 1", null);
            if (cursor != null) {
                cursor.close();
            }
            return false;
        } catch (SQLiteDatabaseLockedException e) {
            return true;
        } catch (SQLiteException e) {
            return false;
        }
    }

}