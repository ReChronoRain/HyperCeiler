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
package com.sevtinge.hyperceiler.ui.page.settings.development;

import static com.sevtinge.hyperceiler.utils.XposedActivateHelper.isModuleActive;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getBoard;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getBrand;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getCharacteristics;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getDeviceName;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getDeviceToken;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getFingerPrint;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getLanguage;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getLocale;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getManufacturer;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getMarketName;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getModDevice;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getModelName;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getSoc;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isInternational;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isLargeUI;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getAndroidVersion;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getBuildDate;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getCurrentUserId;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getHyperOSVersion;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getSystemVersionIncremental;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getWhoAmI;
import static com.sevtinge.hyperceiler.hook.utils.log.LogManager.IS_LOGGER_ALIVE;
import static com.sevtinge.hyperceiler.hook.utils.log.LogManager.LOGGER_CHECKER_ERR_CODE;

import androidx.preference.Preference;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.common.utils.MainActivityContextHelper;
import com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt;
import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.expansion.utils.SignUtils;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.hook.utils.api.ProjectApi;

import java.util.LinkedHashMap;
import java.util.Map;

public class DevelopmentDebugInfoFragment extends SettingsPreferenceFragment {

    private Preference mDebugInfo;
    MainActivityContextHelper mainActivityContextHelper;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_development_debug_info;
    }

    @Override
    public void initPrefs() {
        mDebugInfo = findPreference("prefs_key_debug_info");
        mainActivityContextHelper = new MainActivityContextHelper(requireContext());
        if (mDebugInfo != null) {
            mDebugInfo.setTitle(getDebugInfo());
        }
    }

    public String getDebugInfo() {
        Map<String, String> propertiesModule = new LinkedHashMap<>();
        Map<String, String> propertiesDevice = new LinkedHashMap<>();
        Map<String, String> propertiesSystem = new LinkedHashMap<>();
        Map<String, String> propertiesCheck = new LinkedHashMap<>();
        try {
            propertiesModule.put("VersionName", BuildConfig.VERSION_NAME);
            propertiesModule.put("VersionCode", String.valueOf(BuildConfig.VERSION_CODE));
            propertiesModule.put("BuildTime", "CST " + BuildConfig.BUILD_TIME);
            propertiesModule.put("BuildType", BuildConfig.BUILD_TYPE);
            propertiesModule.put("BuildOsName", BuildConfig.BUILD_OS_NAME);
            propertiesModule.put("BuildUserName", BuildConfig.BUILD_USER_NAME);
            propertiesModule.put("BuildJavaVersion", BuildConfig.BUILD_JAVA_VERSION);
            propertiesModule.put("GitHash", BuildConfig.GIT_HASH);
            propertiesModule.put("GitCode", BuildConfig.GIT_CODE);
            propertiesModule.put("Debug", String.valueOf(BuildConfig.DEBUG));
            propertiesModule.put("ApplicationId", ProjectApi.mAppModulePkg);
        } catch (Exception ignored) {
        }
        try {
            propertiesDevice.put("MarketName", getMarketName());
            propertiesDevice.put("DeviceName", getDeviceName());
            propertiesDevice.put("Model", getModelName());
            propertiesDevice.put("Brand", getBrand());
            propertiesDevice.put("Manufacture", getManufacturer());
            propertiesDevice.put("Board", getBoard());
            propertiesDevice.put("Soc", getSoc());
            propertiesDevice.put("ModDevice", getModDevice());
            propertiesDevice.put("Characteristics", getCharacteristics());
            propertiesDevice.put("Pad", String.valueOf(isPad()));
            propertiesDevice.put("Large Screen", String.valueOf(isLargeUI()));
            propertiesDevice.put("FingerPrint", getFingerPrint());
            propertiesDevice.put("Locale", getLocale());
            propertiesDevice.put("Language", getLanguage());
            propertiesDevice.put("AndroidId", mainActivityContextHelper.getAndroidId());
            // propertiesDevice.put("Serial", getSerial());
            propertiesDevice.put("DeviceToken", getDeviceToken(mainActivityContextHelper.getAndroidId()));
        } catch (Exception ignored) {
        }
        try {
            propertiesSystem.put("AndroidSdkVersion", String.valueOf(getAndroidVersion()));
            propertiesSystem.put("HyperOsVersion", String.valueOf(getHyperOSVersion()));
            propertiesSystem.put("SystemVersion", getSystemVersionIncremental());
            propertiesSystem.put("InternationalBuild", String.valueOf(isInternational()));
            // propertiesSystem.put("Builder", getBuilder());
            // propertiesSystem.put("RomAuthor", getRomAuthor());
            propertiesSystem.put("BaseOs", SystemSDKKt.getBaseOs());
            propertiesSystem.put("Host", SystemSDKKt.getHost());
            propertiesSystem.put("BuildDate", getBuildDate());
        } catch (Exception ignored) {
        }
        try {
            propertiesCheck.put("Signature", SignUtils.getSHA256Signature(requireContext()));
            propertiesCheck.put("SignCheckPass", String.valueOf(SignUtils.isSignCheckPass(requireContext())));
            propertiesCheck.put("ModuleActive", String.valueOf(isModuleActive));
            // propertiesCheck.put("RootPermission", String.valueOf(ShellInit.ready()));
            propertiesCheck.put("WhoAmI", getWhoAmI());
            propertiesCheck.put("LoggerStatus", IS_LOGGER_ALIVE + ", " + LOGGER_CHECKER_ERR_CODE);
            propertiesCheck.put("CurrentUserId", String.valueOf(getCurrentUserId()));
        } catch (Exception ignored) {
        }

        StringBuilder debugInfo = new StringBuilder("Debug Info by HyperCeiler");
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
