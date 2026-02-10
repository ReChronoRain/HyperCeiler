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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import androidx.annotation.NonNull;

import com.fan.common.logviewer.LogManager;
import com.fan.common.logviewer.LogViewerActivity;
import com.fan.common.logviewer.XposedLogLoader;
import com.sevtinge.hyperceiler.common.utils.LSPosedScopeHelper;
import com.sevtinge.hyperceiler.common.utils.ScopeManager;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.model.data.AppInfoCache;
import com.sevtinge.hyperceiler.safemode.ExceptionCrashActivity;
import com.sevtinge.hyperceiler.utils.DeviceInfoBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.github.libxposed.service.RemotePreferences;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class Application extends android.app.Application implements XposedServiceHelper.OnServiceListener {
    private static final String TAG = "Application";
    private static final Runnable reloadListener = () -> {};
    public static boolean isModuleActivated = false;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        PrefsUtils.mSharedPreferences = PrefsUtils.getSharedPrefs(base, true);
        XposedServiceHelper.registerListener(this);

    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化日志系统
        com.sevtinge.hyperceiler.libhook.utils.log.LogManager.init(this.getDataDir().getAbsolutePath());
        LogManager.setDeviceInfoProvider(DeviceInfoBuilder::build);
        LogManager.init(this);
        LogViewerActivity.setXposedLogLoader((context, callback) -> XposedLogLoader.loadLogs(callback));

        // 应用内 Crash 服务
        setupCrashHandler();

        // 加载图标缓存
        new Thread(() -> AppInfoCache.getInstance(this).initAllAppInfos()).start();

        LSPosedScopeHelper.init();
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

    @Override
    public void onServiceBind(@NonNull XposedService service) {
        AndroidLog.d(TAG, "LSPosed service connected: " + service.getFrameworkName() + " v" + service.getFrameworkVersion());
        synchronized (this) {
            isModuleActivated = true;
            ScopeManager.setService(service);
            PrefsUtils.remotePrefs =
                (RemotePreferences) service.getRemotePreferences(PrefsUtils.mPrefsName + "_remote");

            PrefsUtils.syncAllToRemotePrefs();
            reloadListener.run();
        }
    }

    @Override
    public void onServiceDied(@NonNull XposedService xposedService) {
        AndroidLog.e(TAG, "LSPosed service died.");
        synchronized (this) {
            isModuleActivated = false;
            PrefsUtils.remotePrefs = null;
        }
    }
}
