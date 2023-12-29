package com.sevtinge.hyperceiler.ui.fragment.settings.development;

import static com.sevtinge.hyperceiler.utils.Helpers.isModuleActive;
import static com.sevtinge.hyperceiler.utils.ShellUtils.checkRootPermission;
import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.getBoard;
import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.getBrand;
import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.getDeviceName;
import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.getFingerPrint;
import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.getLanguage;
import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.getLocale;
import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.getManufacture;
import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.getModelName;
import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.getSerial;
import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.getSoc;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getBaseOs;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getBuildDate;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getBuilder;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getMiuiVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getRomAuthor;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getSystemVersionIncremental;

import android.widget.TextView;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.MainActivityContextHelper;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import java.util.LinkedHashMap;
import java.util.Map;

import moralnorm.preference.Preference;

public class DevelopmentDebugInfoFragment extends SettingsPreferenceFragment {

    private Preference mDebugInfo;
    MainActivityContextHelper mainActivityContextHelper;
    TextView m;


    @Override
    public int getContentResId() {
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
            propertiesModule.put("BuildTime", "(UTC+0:00) " + BuildConfig.BUILD_TIME);
            propertiesModule.put("BuildType", BuildConfig.BUILD_TYPE);
            propertiesModule.put("GitHash", BuildConfig.GIT_HASH);
            propertiesModule.put("Debug", String.valueOf(BuildConfig.DEBUG));
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
            propertiesDevice.put("Language", getLanguage());
            propertiesDevice.put("AndroidId", mainActivityContextHelper.getAndroidId());
            propertiesDevice.put("Serial", getSerial());
        } catch (Exception ignored) {
        }
        try {
            propertiesSystem.put("AndroidVersion", String.valueOf(getAndroidVersion()));
            propertiesSystem.put("MiuiVersion", String.valueOf(getMiuiVersion()));
            propertiesSystem.put("HyperOsVersion", String.valueOf(getHyperOSVersion()));
            propertiesSystem.put("SystemVersion", getSystemVersionIncremental());
            propertiesSystem.put("Builder", getBuilder());
            propertiesSystem.put("RomAuthor", getRomAuthor());
            propertiesSystem.put("BaseOs", getBaseOs());
            propertiesSystem.put("Host", com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getHost());
            propertiesSystem.put("BuildDate", getBuildDate());
        } catch (Exception ignored) {
        }
        try {
            propertiesCheck.put("Signature", mainActivityContextHelper.getSHA256Signature());
            propertiesCheck.put("SignCheckPass", String.valueOf(mainActivityContextHelper.isSignCheckPass()));
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
