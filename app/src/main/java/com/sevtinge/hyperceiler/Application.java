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
import android.os.Process;

import com.fan.common.logviewer.LogAppProxy;
import com.fan.common.logviewer.LogEntry;
import com.fan.common.logviewer.LogManager;
import com.fan.common.logviewer.LogViewerActivity;
import com.sevtinge.hyperceiler.common.utils.LSPosedScopeHelper;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.model.data.AppInfoCache;
import com.sevtinge.hyperceiler.safemode.ExceptionCrashActivity;
import com.sevtinge.hyperceiler.utils.log.XposedLogLoader;

import java.io.PrintWriter;
import java.io.StringWriter;

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
        LogAppProxy.onCreate(this);

        LogViewerActivity.setXposedLogLoader(XposedLogLoader::loadLogs);

        AndroidLogUtils.setLogListener((level, tag, message) -> {
            try {
                LogManager logManager = LogManager.getInstance();
                logManager.addLog(new LogEntry(level, "App", "[" + tag + "] " + message, tag, true));
            } catch (Throwable ignored) {
            }
        });

        new Thread(() -> AppInfoCache.getInstance(this).initAllAppInfos()).start();

        LSPosedScopeHelper.init(this);
        setupCrashHandler();
    }

    private void setupCrashHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            final String crashInfo = sw.toString();

            new Handler(Looper.getMainLooper()).post(() -> {
                Intent intent = new Intent(getApplicationContext(), ExceptionCrashActivity.class);
                intent.putExtra("crashInfo", crashInfo);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                getApplicationContext().startActivity(intent);
            });

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, ex);
            } else {
                Process.killProcess(Process.myPid());
                System.exit(1);
            }
        });
    }
}
