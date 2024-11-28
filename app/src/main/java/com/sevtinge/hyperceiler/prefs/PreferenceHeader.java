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
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getWhoAmI;
import static com.sevtinge.hyperceiler.utils.shell.ShellUtils.safeExecCommandWithRoot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.DatabaseHelper;
import com.sevtinge.hyperceiler.utils.PackagesUtils;

import java.util.ArrayList;
import java.util.List;

public class PreferenceHeader extends XmlPreference {

    public static ArrayList<String> mUninstallApp = new ArrayList<>();
    public static ArrayList<String> mDisableOrHiddenApp = new ArrayList<>();
    public static ArrayList<String> mNoScoped = new ArrayList<>();

    public static List<String> scope = new ArrayList<String>();
    public static List<String> notInSelectedScope = new ArrayList<String>();

    private static boolean isScopeGet = false;

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
        if (!isScopeGet) if (getWhoAmI().equals("root")) getScope();
        if (isUninstall(context)) {
            mUninstallApp.add(" - " + getTitle() + " (" + getSummary() + ")");
            setVisible(false);
        } else if (isDisable(context) || isHidden(context)) {
            mDisableOrHiddenApp.add(" - " + getTitle() + " (" + getSummary() + ")");
            setVisible(false);
        }
        if (!scope.contains(getSummary()) && (getSummary() != null) && isScopeGet) {
            notInSelectedScope.add((String) getSummary());
            mNoScoped.add(" - " + getTitle() + " (" + getSummary() + ")");
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

    private void getScope() {

        safeExecCommandWithRoot("cp -r /data/adb/lspd/config /data/data/com.sevtinge.hyperceiler/cache/\n");
        safeExecCommandWithRoot("chmod -R 777 /data/data/com.sevtinge.hyperceiler/cache/config\n");

        DatabaseHelper dbHelper = new DatabaseHelper(this.getContext(), "/data/data/com.sevtinge.hyperceiler/cache/config/modules_config.db");

        String tableName = "modules";
        String[] columns = {"mid"};
        String selection = "module_pkg_name = ?";
        String[] selectionArgs = {APPLICATION_ID};

        Cursor cursor = dbHelper.customQuery(tableName, columns, selection, selectionArgs, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String mid = cursor.getString(cursor.getColumnIndex("mid"));

                tableName = "scope";
                columns = new String[]{"app_pkg_name"};
                selection = "mid = ?";
                selectionArgs = new String[]{mid};

                cursor = dbHelper.customQuery(tableName, columns, selection, selectionArgs, null);

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        @SuppressLint("Range") String getScope = cursor.getString(cursor.getColumnIndex("app_pkg_name"));
                        if (getScope.equals("system")) getScope = "android";
                        scope.add(getScope);
                    } while (cursor.moveToNext());
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
        isScopeGet = true;
    }
}