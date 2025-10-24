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
import static com.sevtinge.hyperceiler.core.BuildConfig.APP_MODULE_ID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    public static boolean isInSelectedScope(Context context, String lable, String pkg, String key, SharedPreferences sp) {
        String normLabel = lable == null ? "" : lable.trim();
        String normPkg = pkg == null ? null : pkg.trim();
        String normPkgForEntry = normPkg == null ? "" : normPkg.toLowerCase();
        String entry = " - " + normLabel + (normPkg != null ? " (" + normPkgForEntry + ")" : "");

        if (isDisable(context, pkg) || isUninstall(context, pkg) || isHidden(context, pkg)) {
            if (!mUninstallApp.contains(entry)) {
                mUninstallApp.add(entry);
            }
            return false;
        } else if (isHiddenByHyperceiler(key, sp)) {
            if (!mDisableOrHiddenApp.contains(entry)) {
                mDisableOrHiddenApp.add(entry);
            }
            return false;
        }
        if (pkg != null && !mScope.contains(pkg) && isInitScopeGet && !isScopeGetFailed) {
            if (!mNotInSelectedScope.contains(normPkgForEntry)) {
                mNotInSelectedScope.add(normPkgForEntry);
            }
            if (!mDisableOrHiddenApp.contains(entry) && !mUninstallApp.contains(entry) && !mNoScoped.contains(entry)) {
                mNoScoped.add(entry);
            }
            return false;
        }
        return true;
    }

    private static boolean isAndroidPackage(String pkg) {
        return true;
    }

    private static boolean isUninstall(Context context, String pkg) {
        return isAndroidPackage(pkg) && PackagesUtils.isUninstall(context, pkg);
    }

    private static boolean isDisable(Context context, String pkg) {
        return isAndroidPackage(pkg) && PackagesUtils.isDisable(context, pkg);
    }

    private static boolean isHidden(Context context, String pkg) {
        return isAndroidPackage(pkg) && PackagesUtils.isHidden(context, pkg);
    }

    private static boolean isHiddenByHyperceiler(String key, SharedPreferences sp) {
        return !sp.getBoolean(key + "_state", true);
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
            cursor = db.query(tableName, null, null, null, null, null, null);

            Set<String> totalScopeSet = new LinkedHashSet<>();
            if (cursor.moveToFirst()) {
                int midCol = cursor.getColumnIndex("mid");
                int modulePkgCol = cursor.getColumnIndex("module_pkg_name");

                List<String> scopeUid = new ArrayList<>();
                try {
                    scopeUid = queryList(db, "app_pkg_name", "scope", "user_id = ?", new String[]{String.valueOf(userId)}, true);
                } catch (Exception e) {
                    AndroidLogUtils.logW("PreferenceHeader", "Query scope by user_id failed: ", e);
                }

                do {
                    String mid = midCol != -1 ? cursor.getString(midCol) : null;
                    String modulePkg = modulePkgCol != -1 ? cursor.getString(modulePkgCol) : null;

                    if (modulePkg != null && !modulePkg.equals(APP_MODULE_ID)) continue;

                    Set<String> candidates = new LinkedHashSet<>();
                    if (mid != null) {
                        try {
                            candidates.addAll(queryList(db, "app_pkg_name", "scope", "mid = ?", new String[]{mid}, true));
                        } catch (Exception e) {
                            AndroidLogUtils.logW("PreferenceHeader", "Query scope by mid failed: ", e);
                        }
                    }

                    if (modulePkg != null) {
                        try {
                            candidates.addAll(queryList(db, "app_pkg_name", "scope", "module_pkg_name = ?", new String[]{modulePkg}, true));
                        } catch (Exception e) {
                            AndroidLogUtils.logW("PreferenceHeader", "Query scope by module_pkg_name failed: ", e);
                        }
                    }

                    if (!scopeUid.isEmpty()) {
                        candidates.retainAll(scopeUid);
                    }

                    totalScopeSet.addAll(candidates);
                } while (cursor.moveToNext());
            }
            mScope = new ArrayList<>(totalScopeSet);
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
