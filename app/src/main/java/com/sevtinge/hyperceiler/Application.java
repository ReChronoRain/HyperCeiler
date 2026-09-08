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
import android.os.Process;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.home.banner.HomePageBannerManager;
import com.sevtinge.hyperceiler.home.safemode.AppCrashStore;
import com.sevtinge.hyperceiler.home.task.AppInitializer;
import com.sevtinge.hyperceiler.log.LogManager;
import com.sevtinge.hyperceiler.log.XposedLogLoader;
import com.sevtinge.hyperceiler.provision.fragment.PermissionSettingsFragment;
import com.sevtinge.hyperceiler.utils.DeviceInfoBuilder;
import com.sevtinge.hyperceiler.utils.FrameworkStatusManager;
import com.sevtinge.hyperceiler.utils.LSPosedScopeHelper;
import com.sevtinge.hyperceiler.utils.ScopeManager;

import fan.provision.OobeUtils;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public class Application extends fan.app.Application
    implements XposedServiceHelper.OnServiceListener {

    private static final String TAG = "Application";
    public static boolean isModuleActivated = false;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(com.sevtinge.hyperceiler.utils.LanguageHelper.wrapContext(base));
        AppInitializer.attach(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 应用启动阶段，预热非 UI 任务（如 Shell、语言包、权限检查）
        AppInitializer.initOnAppCreate(this);
        OobeUtils.syncHookAvailability(this);
        FrameworkStatusManager.init();

        LogManager.init(
            this,
            DeviceInfoBuilder::build,
            () -> XposedLogLoader.syncLogsToDatabaseSync(this)
        );
        setupCrashHandler();
    }


    @Override
    public void onServiceBind(@NonNull XposedService service) {
        synchronized (this) {
            setModuleActivated(true);
            PrefsBridge.setRemotePrefs(service.getRemotePreferences(PrefsBridge.REMOTE_PREFS_GROUP));
            OobeUtils.syncHookAvailability(this);
            FrameworkStatusManager.onServiceBound(service);
            AndroidLog.d(TAG, "XposedService connected: " + describeFrameworkStatus());
            ScopeManager.setService(service);
            LSPosedScopeHelper.reloadScope();
            refreshHomePageBanner();
        }
    }

    @Override
    public void onServiceDied(@NonNull XposedService service) {
        AndroidLog.e(TAG, "XposedService died.");
        synchronized (this) {
            setModuleActivated(false);
            PrefsBridge.setRemotePrefs(null);
            FrameworkStatusManager.onServiceDied();
            ScopeManager.clearService();
            refreshHomePageBanner();
        }
    }

    private static void setModuleActivated(boolean activated) {
        isModuleActivated = activated;
        PermissionSettingsFragment.isModuleActive = activated;
    }

    private static void refreshHomePageBanner() {
        HomePageBannerManager.invalidateCache();
        HomePageBannerManager.requestRefresh();
    }

    @NonNull
    private static String describeFrameworkStatus() {
        FrameworkStatusManager.Status status = FrameworkStatusManager.getCurrentStatus();
        String sb = valueOrUnknown(status.getFrameworkName()) +
            " v" +
            valueOrUnknown(status.getFrameworkVersion()) +
            " (API " +
            (status.getFrameworkApiVersion() >= 0 ? status.getFrameworkApiVersion() : "Unknown") +
            ")";
        return sb;
    }

    @NonNull
    private static String valueOrUnknown(String value) {
        return value == null || value.isEmpty() ? "Unknown" : value;
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
