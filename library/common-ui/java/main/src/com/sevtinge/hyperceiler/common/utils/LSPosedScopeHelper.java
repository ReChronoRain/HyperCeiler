/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.common.utils;

import static com.sevtinge.hyperceiler.hook.utils.SQLiteDatabaseHelper.isDatabaseLocked;
import static com.sevtinge.hyperceiler.hook.utils.SQLiteDatabaseHelper.queryList;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getCurrentUserId;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getWhoAmI;
import static com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils.rootExecCmd;
import static com.sevtinge.hyperceiler.ui.BuildConfig.APP_MODULE_ID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;

import java.util.ArrayList;
import java.util.List;

public class LSPosedScopeHelper {

    private static boolean isInitScopeGet = false;
    private static boolean isScopeGetFailed = false;

    public static ArrayList<String> mNoScoped = new ArrayList<>();
    public static ArrayList<String> mUninstallApp = new ArrayList<>();
    public static ArrayList<String> mDisableOrHiddenApp = new ArrayList<>();

    public static List<String> mScope = new ArrayList<>();
    public static List<String> mNotInSelectedScope = new ArrayList<>();

    public static void init(Context context) {
        if (getWhoAmI().equals("root")) getScope(context);
    }

    public static boolean isInSelectedScope(Context context, String lable, String pkg) {
        if (isUninstall(context, pkg)) {
            mUninstallApp.add(" - " + lable + " (" + pkg + ")");
            return false;
        } else if (isDisable(context, pkg) || isHidden(context, pkg)) {
            mDisableOrHiddenApp.add(" - " + lable + " (" + pkg + ")");
            return false;
        }
        if (pkg != null && !mScope.contains(pkg) && isInitScopeGet && !isScopeGetFailed) {
            mNotInSelectedScope.add(pkg);
            String string = " - " + lable + " (" + pkg + ")";
            if (!mDisableOrHiddenApp.contains(string) && !mUninstallApp.contains(string) && !mNoScoped.contains(string)) {
                mNoScoped.add(string);
            }
            return false;
        }
        return true;
    }

    private static boolean isAndroidPackage(String pkg) {
        return pkg == null || "android".contentEquals(pkg);
    }

    private static boolean isUninstall(Context context, String pkg) {
        return !isAndroidPackage(pkg) && PackagesUtils.isUninstall(context, pkg);
    }

    private static boolean isDisable(Context context, String pkg) {
        return isAndroidPackage(pkg) && PackagesUtils.isDisable(context, pkg);
    }

    private static boolean isHidden(Context context, String pkg) {
        return isAndroidPackage(pkg) && PackagesUtils.isHidden(context, pkg);
    }

    @SuppressLint("Range")
    private static void getScope(Context context) {
        String cachePath = context.getCacheDir().toString();
        int userId = getCurrentUserId();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            rootExecCmd("cp -r /data/adb/lspd/config " + cachePath + " && chmod -R 777 " + cachePath + "/config");

            String dbPath = cachePath + "/config/modules_config.db";
            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);

            if (isDatabaseLocked(db)) {
                AndroidLogUtils.logW("PreferenceHeader", "Database locked, skip get scope.");
                isScopeGetFailed = true;
                return;
            }

            String tableName = "modules";
            String[] columns = {"mid"};
            String selection = "module_pkg_name = ?";
            String[] selectionArgs = {APP_MODULE_ID};

            cursor = db.query(tableName, columns, selection, selectionArgs, null, null, null);

            List<String> totalScope = new ArrayList<>();
            if (cursor.moveToFirst()) {
                List<String> scopeMid;
                List<String> scopeUid;

                do {
                    String mid = cursor.getString(cursor.getColumnIndex("mid"));

                    scopeMid = queryList(db, "app_pkg_name", "scope", "mid = ?", new String[]{mid}, true);
                    scopeUid = queryList(db, "app_pkg_name", "scope", "user_id = ?", new String[]{String.valueOf(userId)}, true);

                    List<String> intersection = new ArrayList<>(scopeMid);
                    intersection.retainAll(scopeUid);

                    for (String pkg : intersection) {
                        if (!totalScope.contains(pkg)) {
                            totalScope.add(pkg);
                        }
                    }
                } while (cursor.moveToNext());
            }
            mScope = totalScope;
        } catch (Exception e) {
            isScopeGetFailed = true;
            AndroidLogUtils.logW("PreferenceHeader", "Database error: ", e);
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        isInitScopeGet = true;
    }
}
