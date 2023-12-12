package com.sevtinge.hyperceiler.ui.fragment.settings.development;

import static com.sevtinge.hyperceiler.utils.Helpers.isModuleActive;
import static com.sevtinge.hyperceiler.utils.ShellUtils.checkRootPermission;
import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.*;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.*;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.Preference;

import java.util.LinkedHashMap;
import java.util.Map;

public class DevelopmentDebugInfoFragment extends SettingsPreferenceFragment {

    private Preference mDebugInfo;

    @Override
    public int getContentResId() {
        return R.xml.prefs_development_debug_info;
    }

    @Override
    public void initPrefs() {
        mDebugInfo = findPreference("prefs_key_debug_info");
        if (mDebugInfo != null) {
            mDebugInfo.setSummary(getDebugInfo());
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
            propertiesModule.put("BuildType", BuildConfig.BUILD_TYPE);
            propertiesModule.put("ApplicationId", BuildConfig.APPLICATION_ID);
        } catch (Exception ignored) {
        }
        try {
            propertiesDevice.put("Device", getDeviceName());
            propertiesDevice.put("Model", getModelName());
            propertiesDevice.put("Brand", getBrand());
            propertiesDevice.put("Manufacture", getManufacture());
            propertiesDevice.put("Board", getBoard());
            propertiesDevice.put("Soc", getSoc());
            propertiesDevice.put("FingerPrint", getFingerPrint());
            propertiesDevice.put("Locale", getLocale());
        } catch (Exception ignored) {
        }
        try {
            propertiesSystem.put("AndroidVersion", String.valueOf(getAndroidVersion()));
            propertiesSystem.put("MiuiVersion", String.valueOf(getMiuiVersion()));
            propertiesSystem.put("HyperOsVersion", String.valueOf(getHyperOSVersion()));
            propertiesSystem.put("SystemVersionIncremental", getSystemVersionIncremental());
            propertiesSystem.put("Builder", getBuilder());
            propertiesSystem.put("RomAuthor", getRomAuthor());
            propertiesSystem.put("BaseOs", getBaseOs());
            propertiesSystem.put("Host", com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getHost());
        } catch (Exception ignored) {
        }
        try {
            propertiesCheck.put("ModuleActive", String.valueOf(isModuleActive));
            propertiesCheck.put("RootPermission", String.valueOf(checkRootPermission() == 0));
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
