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
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.home.safemode;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public final class AppCrashStore {

    private static final String PREFS_NAME = "app_crash_store";
    private static final String KEY_PENDING = "pending";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_TYPE = "type";
    private static final String KEY_FILE = "file";
    private static final String KEY_CLASS = "class";
    private static final String KEY_METHOD = "method";
    private static final String KEY_LINE = "line";
    private static final String KEY_STACK = "stack";
    private static final String KEY_TIME = "time";

    private AppCrashStore() {}

    public static void persist(Context context, Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        StackTraceElement first = stackTrace.length > 0 ? stackTrace[0] : null;

        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putBoolean(KEY_PENDING, true);
        editor.putString(KEY_MESSAGE, throwable.getMessage());
        editor.putString(KEY_TYPE, throwable.getClass().getName());
        editor.putString(KEY_FILE, first != null ? first.getFileName() : null);
        editor.putString(KEY_CLASS, first != null ? first.getClassName() : null);
        editor.putString(KEY_METHOD, first != null ? first.getMethodName() : null);
        editor.putInt(KEY_LINE, first != null ? first.getLineNumber() : -1);
        editor.putString(KEY_STACK, android.util.Log.getStackTraceString(throwable));
        editor.putLong(KEY_TIME, System.currentTimeMillis());
        editor.commit();
    }

    public static boolean hasPendingCrash(Context context) {
        return prefs(context).getBoolean(KEY_PENDING, false);
    }

    public static Intent createIntent(Context context) {
        SharedPreferences prefs = prefs(context);
        Intent intent = new Intent(context, ExceptionCrashActivity.class);
        intent.putExtra("crash_message", prefs.getString(KEY_MESSAGE, null));
        intent.putExtra("crash_type", prefs.getString(KEY_TYPE, null));
        intent.putExtra("crash_file", prefs.getString(KEY_FILE, null));
        intent.putExtra("crash_class", prefs.getString(KEY_CLASS, null));
        intent.putExtra("crash_method", prefs.getString(KEY_METHOD, null));
        intent.putExtra("crash_line", prefs.getInt(KEY_LINE, -1));
        intent.putExtra("crash_stack", prefs.getString(KEY_STACK, null));
        intent.putExtra("crash_time", prefs.getLong(KEY_TIME, System.currentTimeMillis()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
