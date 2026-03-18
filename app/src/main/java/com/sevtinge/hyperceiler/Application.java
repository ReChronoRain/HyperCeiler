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
package com.sevtinge.hyperceiler;

import static android.os.Process.killProcess;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.home.safemode.AppCrashStore;
import com.sevtinge.hyperceiler.home.task.AppInitializer;
import com.sevtinge.hyperceiler.log.LogManager;
import com.sevtinge.hyperceiler.log.XposedLogLoader;
import com.sevtinge.hyperceiler.provision.fragment.PermissionSettingsFragment;
import com.sevtinge.hyperceiler.utils.DeviceInfoBuilder;
import com.sevtinge.hyperceiler.utils.ScopeManager;

import fan.provision.OobeUtils;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class Application extends fan.app.Application
    implements XposedServiceHelper.OnServiceListener {

    private static final String TAG = "Application";
    public static boolean isModuleActivated = false;
    private static final Runnable reloadListener = () -> {};

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        AppInitializer.attach(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 应用启动阶段，预热非 UI 任务（如 Shell、语言包、权限检查）
        AppInitializer.initOnAppCreate(this);
        OobeUtils.syncHookAvailability(this);

        LogManager.init(
            this,
            DeviceInfoBuilder::build,
            () -> XposedLogLoader.syncLogsToDatabaseSync(this)
        );
        setupCrashHandler();
    }


    @Override
    public void onServiceBind(@NonNull XposedService service) {
        AndroidLog.d(TAG, "LSPosed service connected: " + service.getFrameworkName() + " v" + service.getFrameworkVersion());
        synchronized (this) {
            isModuleActivated = true;
            PermissionSettingsFragment.isModuleActive = true;
            ScopeManager.setService(service);

            SharedPreferences remote = service.getRemotePreferences(PrefsBridge.PREFS_NAME + "_remote");
            PrefsBridge.setRemotePrefs(remote);
            OobeUtils.syncHookAvailability(this);

            reloadListener.run();
        }
    }

    @Override
    public void onServiceDied(@NonNull XposedService service) {
        AndroidLog.e(TAG, "LSPosed service died.");
        synchronized (this) {
            isModuleActivated = false;
            PermissionSettingsFragment.isModuleActive = false;
            PrefsBridge.setRemotePrefs(null);
        }
    }

    private void setupCrashHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            AppCrashStore.persist(getApplicationContext(), ex);
            AndroidLog.e("Crash", "App crash captured", ex);

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, ex);
            } else {
                killProcess(Process.myPid());
                System.exit(1);
            }
        });
    }
}
