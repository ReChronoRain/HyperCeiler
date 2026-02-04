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
package com.sevtinge.hyperceiler.utils;

import static com.sevtinge.hyperceiler.Application.isModuleActivated;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.getBrand;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.getDeviceName;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.getDeviceToken;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.getFingerPrint;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.getLanguage;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.getLocale;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.getMarketName;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.getModDevice;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.getModelName;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.getSoc;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isInternational;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Module.scanModules;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getBuildDate;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getCurrentUserId;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getHost;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getHyperOSVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getRootGroupsInfo;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getSmallVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getSystemVersionIncremental;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getXmsVersion;
import static com.sevtinge.hyperceiler.libhook.utils.log.LogManager.IS_LOGGER_ALIVE;
import static com.sevtinge.hyperceiler.libhook.utils.log.LoggerHealthChecker.LOGGER_CHECKER_ERR_CODE;

import android.content.Context;
import android.util.Log;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.common.utils.MainActivityContextHelper;
import com.sevtinge.hyperceiler.expansion.utils.SignUtils;
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper;
import com.sevtinge.hyperceiler.libhook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.main.banner.HomePageBannerHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kotlin.text.Charsets;

/**
 * 设备信息构建器
 */
public class DeviceInfoBuilder {

    private static final String TAG = "DeviceInfoBuilder";

    public static String build(Context context) {
        Map<String, String> propertiesModule = new LinkedHashMap<>();
        Map<String, String> propertiesDevice = new LinkedHashMap<>();
        Map<String, String> propertiesSystem = new LinkedHashMap<>();
        Map<String, String> propertiesCheck = new LinkedHashMap<>();

        try {
            propertiesModule.put("ApplicationId", ProjectApi.mAppModulePkg);
            propertiesModule.put("VersionName", BuildConfig.VERSION_NAME);
            propertiesModule.put("VersionCode", String.valueOf(BuildConfig.VERSION_CODE));
            propertiesModule.put("BuildTime", "CST " + BuildConfig.BUILD_TIME);
            propertiesModule.put("BuildType", BuildConfig.BUILD_TYPE);
            propertiesModule.put("BuildOsName", BuildConfig.BUILD_OS_NAME);
            propertiesModule.put("BuildJavaVersion", BuildConfig.BUILD_JAVA_VERSION);
            propertiesModule.put("GitBranch", BuildConfig.GIT_BRANCH);
            propertiesModule.put("GitCode", BuildConfig.GIT_CODE);
            propertiesModule.put("GitHash", BuildConfig.GIT_HASH);
        } catch (Exception e) {
            Log.w(TAG, "Failed to get module info", e);
        }

        try {
            propertiesDevice.put("MarketName", getMarketName());
            propertiesDevice.put("DeviceName", getDeviceName());
            propertiesDevice.put("Model", getModelName());
            propertiesDevice.put("Brand", getBrand());
            propertiesDevice.put("Soc", getSoc());
            propertiesDevice.put("ModDevice", getModDevice());
            propertiesDevice.put("isPad", String.valueOf(isPad()));
            propertiesDevice.put("FingerPrint", getFingerPrint());
            propertiesDevice.put("Locale", getLocale());
            propertiesDevice.put("Language", getLanguage());
            propertiesDevice.put("DeviceToken", getDeviceToken(MainActivityContextHelper.getAndroidId(context)));
        } catch (Exception e) {
            Log.w(TAG, "Failed to get device info", e);
        }

        try {
            propertiesSystem.put("AndroidSdkVersion", String.valueOf(getAndroidVersion()));
            propertiesSystem.put("HyperOsVersion", String.valueOf(getHyperOSVersion()));
            propertiesSystem.put("HyperOsSmallVersion", String.valueOf(getSmallVersion()));
            propertiesSystem.put("SOTAVersion", getXmsVersion());
            propertiesSystem.put("SystemVersion", getSystemVersionIncremental());
            propertiesSystem.put("InternationalBuild", String.valueOf(isInternational()));
            propertiesSystem.put("Host", getHost());
            propertiesSystem.put("BuildDate", getBuildDate());
            propertiesSystem.put("UnofficialRom", String.valueOf(HomePageBannerHelper.getIsUnofficialRom(context)));
        } catch (Exception e) {
            Log.w(TAG, "Failed to get system info", e);
        }

        // 检查信息
        try {
            List<DeviceHelper.Module.ModuleInfo> module = scanModules("/data/adb/modules", Charsets.UTF_8);

            if (module != null && !module.isEmpty()) {
                propertiesCheck.put("XposedManger", module.get(0).extractName());
                propertiesCheck.put("XposedMangerVersion", module.get(0).formattedVersion());
            } else {
                propertiesCheck.put("XposedManger", "N/A");
                propertiesCheck.put("XposedMangerVersion", "N/A");
            }
            propertiesCheck.put("RootGroups", getRootGroupsInfo());
            propertiesCheck.put("CurrentUserId", String.valueOf(getCurrentUserId()));
            propertiesCheck.put("ModuleActive", String.valueOf(isModuleActivated));
            propertiesCheck.put("DebugModeActivate", String.valueOf(
                PrefsUtils.getSharedBoolPrefs(context, "prefs_key_development_debug_mode", false)));
            propertiesCheck.put("LoggerStatus", IS_LOGGER_ALIVE + ", " + LOGGER_CHECKER_ERR_CODE);
            propertiesCheck.put("Signature", SignUtils.getSHA256Signature(context));
            propertiesCheck.put("SignCheckPass", String.valueOf(SignUtils.isSignCheckPass(context)));
        } catch (Exception e) {
            Log.w(TAG, "Failed to get check info", e);
        }

        StringBuilder debugInfo = new StringBuilder("Debug Info by HyperCeiler");
        debugInfo.append("\nGenerated: CST ").append(
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        debugInfo.append("\n");

        for (Map.Entry<String, String> entry : propertiesModule.entrySet()) {
            debugInfo.append("\n").append(entry.getKey()).append(" = ").append(entry.getValue());
        }
        debugInfo.append("\n");
        for (Map.Entry<String, String> entry : propertiesDevice.entrySet()) {
            debugInfo.append("\n").append(entry.getKey()).append(" = ").append(entry.getValue());
        }
        debugInfo.append("\n");
        for (Map.Entry<String, String> entry : propertiesSystem.entrySet()) {
            debugInfo.append("\n").append(entry.getKey()).append(" = ").append(entry.getValue());
        }
        debugInfo.append("\n");
        for (Map.Entry<String, String> entry : propertiesCheck.entrySet()) {
            debugInfo.append("\n").append(entry.getKey()).append(" = ").append(entry.getValue());
        }

        return debugInfo.toString();
    }
}
