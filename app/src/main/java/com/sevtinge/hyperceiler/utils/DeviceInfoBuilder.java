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

import static com.sevtinge.hyperceiler.common.log.LogStatusManager.IS_LOGGER_ALIVE;
import static com.sevtinge.hyperceiler.common.log.LogStatusManager.formatLoggerStatusDetail;
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
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getBuildDate;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getCurrentUserId;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getHost;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getHyperOSVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getRootGroupsInfo;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getSmallVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getSystemVersionIncremental;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getXmsVersion;

import android.content.Context;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.log.LoggerHealthChecker;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.common.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.expansion.utils.SignUtils;
import com.sevtinge.hyperceiler.home.banner.HomePageBannerManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

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
            AndroidLog.w(TAG, "Failed to collect module info", e);
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
            AndroidLog.w(TAG, "Failed to collect device info", e);
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
            propertiesSystem.put("UnofficialRom", String.valueOf(HomePageBannerManager.isUnofficialRom(context)));
        } catch (Exception e) {
            AndroidLog.w(TAG, "Failed to collect system info", e);
        }

        // 检查信息
        try {
            FrameworkStatusManager.Status frameworkStatus = FrameworkStatusManager.getCurrentStatus();
            propertiesCheck.put("XposedManger", frameworkStatus.getFrameworkName() == null ? "N/A" : frameworkStatus.getFrameworkName());
            propertiesCheck.put("XposedMangerVersion", buildFrameworkVersionSummary(frameworkStatus));
            propertiesCheck.put("FrameworkCheck", buildFrameworkCheckSummary(frameworkStatus));
            propertiesCheck.put("RootGroups", getRootGroupsInfo());
            propertiesCheck.put("CurrentUserId", String.valueOf(getCurrentUserId()));
            propertiesCheck.put("ModuleActive", String.valueOf(XposedActivateHelper.isActive()));
            propertiesCheck.put("DebugModeActivate", String.valueOf(
                PrefsBridge.getBoolean("prefs_key_development_debug_mode", false)));
            if ("NOT_CHECKED".equals(LoggerHealthChecker.diagSummary)) {
                LoggerHealthChecker.isLoggerAlive();
            }
            String loggerStatus = IS_LOGGER_ALIVE + ", " + formatLoggerStatusDetail();
            propertiesCheck.put("LoggerStatus", loggerStatus);
            propertiesCheck.put("Signature", SignUtils.getSHA256Signature(context));
            propertiesCheck.put("SignCheckPass", String.valueOf(SignUtils.isSignCheckPass(context)));
        } catch (Exception e) {
            AndroidLog.w(TAG, "Failed to collect check info", e);
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

    private static String buildFrameworkVersionSummary(FrameworkStatusManager.Status status) {
        StringBuilder sb = new StringBuilder();
        sb.append(status.getFrameworkVersion() == null ? "N/A" : status.getFrameworkVersion());
        if (status.getFrameworkVersionCode() >= 0) {
            sb.append(" (");
            sb.append(status.getFrameworkVersionCode());
            sb.append(")");
        }
        if (status.getFrameworkApiVersion() >= 0) {
            sb.append(", API ");
            sb.append(status.getFrameworkApiVersion());
        }
        return sb.toString();
    }

    private static String buildFrameworkCheckSummary(FrameworkStatusManager.Status status) {
        StringBuilder sb = new StringBuilder();
        sb.append(status.getReason().name());
        sb.append(", HookAllowed=");
        sb.append(status.isHookAllowed());
        if (status.getDetail() != null && !status.getDetail().isEmpty()) {
            sb.append(", ");
            sb.append(status.getDetail());
        }
        return sb.toString();
    }
}
