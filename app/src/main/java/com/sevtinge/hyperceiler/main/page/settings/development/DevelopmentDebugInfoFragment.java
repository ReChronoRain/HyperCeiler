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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.main.page.settings.development;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getBrand;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getDeviceName;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getDeviceToken;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getFingerPrint;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getLanguage;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getLocale;
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
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getRootGroupsInfo;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getSmallVersion;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getSystemVersionIncremental;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.scanModules;
import static com.sevtinge.hyperceiler.hook.utils.log.LogManager.IS_LOGGER_ALIVE;
import static com.sevtinge.hyperceiler.hook.utils.log.LogManager.LOGGER_CHECKER_ERR_CODE;
import static com.sevtinge.hyperceiler.utils.XposedActivateHelper.isModuleActive;

import androidx.preference.Preference;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.utils.MainActivityContextHelper;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.expansion.utils.SignUtils;
import com.sevtinge.hyperceiler.hook.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.hook.utils.devicesdk.ModuleInfo;
import com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import kotlin.text.Charsets;

public class DevelopmentDebugInfoFragment extends SettingsPreferenceFragment {

    private Preference mDebugInfo;
    private ExecutorService mExecutor;
    private Future<?> mFuture;

    private volatile String mCachedDebugInfo;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_development_debug_info;
    }

    @Override
    public void initPrefs() {
        mDebugInfo = findPreference("prefs_key_debug_info");
        if (mDebugInfo != null) {
            mDebugInfo.setTitle("Loading...");
            loadDebugInfoAsync();
        }
    }

    private void loadDebugInfoAsync() {
        if (mCachedDebugInfo != null) {
            if (mDebugInfo != null) {
                mDebugInfo.setTitle(mCachedDebugInfo);
            }
            return;
        }

        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "HyperCeiler-DebugInfo-Worker");
                t.setDaemon(true);
                return t;
            });
        }


        if (mFuture != null && !mFuture.isDone()) {
            mFuture.cancel(true);
        }

        mFuture = mExecutor.submit(() -> {
            try {
                final String info = buildDebugInfo();
                mCachedDebugInfo = info;
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (mDebugInfo != null) {
                            mDebugInfo.setTitle(info);
                        }
                    });
                }
            } catch (Exception e) {

                String fallback = "Failed to load debug info";
                mCachedDebugInfo = fallback;
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (mDebugInfo != null) {
                            mDebugInfo.setTitle(fallback);
                        }
                    });
                }
            }
        });
    }

    @NotNull
    private String buildDebugInfo() {
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
            // propertiesModule.put("Debug", String.valueOf(BuildConfig.DEBUG));
        } catch (Exception ignored) {
        }
        try {
            propertiesDevice.put("MarketName", getMarketName());
            propertiesDevice.put("DeviceName", getDeviceName());
            propertiesDevice.put("Model", getModelName());
            propertiesDevice.put("Brand", getBrand());
            // propertiesDevice.put("Manufacture", getManufacturer());
            // propertiesDevice.put("Board", getBoard());
            propertiesDevice.put("Soc", getSoc());
            propertiesDevice.put("ModDevice", getModDevice());
            propertiesDevice.put("isPad", String.valueOf(isPad()));
            propertiesDevice.put("LargeScreen", String.valueOf(isLargeUI()));
            propertiesDevice.put("FingerPrint", getFingerPrint());
            propertiesDevice.put("Locale", getLocale());
            propertiesDevice.put("Language", getLanguage());
            // propertiesDevice.put("AndroidId", MainActivityContextHelper.getAndroidId(requireContext()));
            // propertiesDevice.put("Serial", getSerial());
            // device token generation uses AndroidId internally; safe to call off UI thread.
            propertiesDevice.put("DeviceToken", getDeviceToken(MainActivityContextHelper.getAndroidId(requireContext())));
        } catch (Exception ignored) {
        }
        try {
            propertiesSystem.put("AndroidSdkVersion", String.valueOf(getAndroidVersion()));
            propertiesSystem.put("HyperOsVersion", String.valueOf(getHyperOSVersion()));
            propertiesSystem.put("HyperOsSmallVersion", String.valueOf(getSmallVersion()));
            propertiesSystem.put("SystemVersion", getSystemVersionIncremental());
            propertiesSystem.put("InternationalBuild", String.valueOf(isInternational()));
            propertiesSystem.put("Host", SystemSDKKt.getHost());
            propertiesSystem.put("BuildDate", getBuildDate());
            propertiesSystem.put("UnofficialRom", String.valueOf(com.sevtinge.hyperceiler.main.banner.HomePageBannerHelper.getIsUnofficialRom(requireContext())));
            // propertiesSystem.put("Builder", getBuilder());
            // propertiesSystem.put("RomAuthor", getRomAuthor());
            // propertiesSystem.put("BaseOs", SystemSDKKt.getBaseOs());
        } catch (Exception ignored) {
        }
        try {
            List<ModuleInfo> module = scanModules("/data/adb/modules", Charsets.UTF_8);

            if (!module.isEmpty()) {
                propertiesCheck.put("XposedManger", module.getFirst().extractName());
                propertiesCheck.put("XposedMangerVersion", module.getFirst().formattedVersion());
            } else {
                propertiesCheck.put("XposedManger", "N/A");
                propertiesCheck.put("XposedMangerVersion", "N/A");
            }
            // propertiesCheck.put("RootPermission", String.valueOf(ShellInit.ready()));
            // propertiesCheck.put("WhoAmI", getWhoAmI());
            propertiesCheck.put("RootGroups", getRootGroupsInfo());
            propertiesCheck.put("CurrentUserId", String.valueOf(getCurrentUserId()));
            propertiesCheck.put("ModuleActive", String.valueOf(isModuleActive));
            propertiesCheck.put("DebugModeActivate", String.valueOf(PrefsUtils.getSharedBoolPrefs(requireContext(), "prefs_key_development_debug_mode", false)));
            propertiesCheck.put("LoggerStatus", IS_LOGGER_ALIVE + ", " + LOGGER_CHECKER_ERR_CODE);
            propertiesCheck.put("Signature", SignUtils.getSHA256Signature(requireContext()));
            propertiesCheck.put("SignCheckPass", String.valueOf(SignUtils.isSignCheckPass(requireContext())));
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mFuture != null && !mFuture.isDone()) {
                mFuture.cancel(true);
            }
            if (mExecutor != null && !mExecutor.isShutdown()) {
                mExecutor.shutdownNow();
            }
        } catch (Exception ignored) {
        }
    }
}
