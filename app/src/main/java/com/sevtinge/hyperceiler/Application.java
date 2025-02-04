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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.hchen.hooktool.log.AndroidLog;
import com.sevtinge.hyperceiler.ui.app.crash.CrashHandlerDialog;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

public class Application extends android.app.Application {
    private static final String TAG = "HyperCeiler:Application";

    @Override
    protected void attachBaseContext(Context base) {
        PrefsUtils.mSharedPreferences = PrefsUtils.getSharedPrefs(base);
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupCrashHandler();
    }

    private void setupCrashHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String crashInfo = "Crash Time: " + new Date() + "\n" +
                "Stack Trace:\n" + sw;

            if (Looper.getMainLooper().getThread() == thread)
                handleMainThreadCrash(crashInfo);
            else
                new Handler(Looper.getMainLooper()).post(() -> showEmergencyDialog(crashInfo));
        });
    }

    private void handleMainThreadCrash(String crashInfo) {
        new Handler(Looper.getMainLooper()).post(() -> showEmergencyDialog(crashInfo));

        try {
            Looper.loop();
        } catch (Throwable t) {
            AndroidLog.logE(TAG, "Looper error: ", t);
        }
    }

    private void showEmergencyDialog(String crashInfo) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Intent intent = new Intent(CrashHandlerDialog.CrashHandlerBroadcastReceiver.CRASH_HANDLER);
                intent.putExtra("crashInfo", crashInfo);
                sendBroadcast(intent);
            } catch (Throwable e) {
                AndroidLog.logE(TAG, e);
            }
        });
    }
}
