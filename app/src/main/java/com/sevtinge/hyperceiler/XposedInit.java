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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler;

import static com.sevtinge.hyperceiler.utils.Helpers.getPackageVersionCode;
import static com.sevtinge.hyperceiler.utils.Helpers.getPackageVersionName;
import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getMiuiVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMiuiVersion;
import static com.sevtinge.hyperceiler.utils.log.LogManager.logLevelDesc;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logI;
import static com.sevtinge.hyperceiler.utils.prefs.PrefsUtils.mPrefsMap;

import android.os.Process;

import com.github.kyuubiran.ezxhelper.EzXHelper;
import com.hchen.hooktool.HCInit;
import com.sevtinge.hyperceiler.module.app.VariousThirdApps;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.DataBase;
import com.sevtinge.hyperceiler.module.base.tool.ResourcesTool;
import com.sevtinge.hyperceiler.module.hook.systemframework.AllowManageAllNotifications;
import com.sevtinge.hyperceiler.module.hook.systemframework.AllowUninstall;
import com.sevtinge.hyperceiler.module.hook.systemframework.BackgroundBlurDrawable;
import com.sevtinge.hyperceiler.module.hook.systemframework.CleanOpenMenu;
import com.sevtinge.hyperceiler.module.hook.systemframework.CleanShareMenu;
import com.sevtinge.hyperceiler.module.hook.systemframework.ScreenRotation;
import com.sevtinge.hyperceiler.module.hook.systemframework.ToastBlur;
import com.sevtinge.hyperceiler.module.hook.systemframework.UnlockAlwaysOnDisplay;
import com.sevtinge.hyperceiler.module.hook.systemframework.network.FlightModeHotSpot;
import com.sevtinge.hyperceiler.module.hook.systemsettings.VolumeSeparateControlForSettings;
import com.sevtinge.hyperceiler.module.skip.SystemFrameworkForCorePatch;
import com.sevtinge.hyperceiler.safe.CrashHook;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.utils.log.LogManager;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedInit implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    private static final String TAG = "HyperCeiler";
    public static boolean isSafeModeOn = false;
    public static String mModulePath = null;
    public static ResourcesTool mResHook;

    // public static XmlTool mXmlTool;
    public final VariousThirdApps mVariousThirdApps = new VariousThirdApps();

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        mResHook = new ResourcesTool(startupParam.modulePath);
        mModulePath = startupParam.modulePath;
        // mXmlTool = new XmlTool(startupParam);
        setXSharedPrefs();

        EzXHelper.initZygote(startupParam);
        EzXHelper.setLogTag(TAG);
        EzXHelper.setToastTag(TAG);
        HCInit.initBasicData(new HCInit.BasicData()
                .setModulePackageName(BuildConfig.APPLICATION_ID)
                .setLogLevel(LogManager.getLogLevel())
                .setTag("HyperCeiler")
        );
        HCInit.initStartupParam(startupParam);

        if (mPrefsMap.getBoolean("system_framework_screen_all_rotations")) ScreenRotation.initRes();
        if (mPrefsMap.getBoolean("system_framework_clean_share_menu")) CleanShareMenu.initRes();
        if (mPrefsMap.getBoolean("system_framework_clean_open_menu")) CleanOpenMenu.initRes();
        if (mPrefsMap.getBoolean("system_framework_volume_separate_control"))
            VolumeSeparateControlForSettings.initRes();

        if (startupParam != null) {
            new BackgroundBlurDrawable().initZygote(startupParam);
            new SystemFrameworkForCorePatch().initZygote(startupParam);

            if (mPrefsMap.getBoolean("system_framework_allow_uninstall"))
                new AllowUninstall().initZygote(startupParam);
            if (mPrefsMap.getBoolean("system_framework_allow_manage_all_notifications"))
                new AllowManageAllNotifications().initZygote(startupParam);
            if (mPrefsMap.getBoolean("system_framework_background_blur_toast"))
                new ToastBlur().initZygote(startupParam);
            if (mPrefsMap.getBoolean("aod_unlock_always_on_display_hyper"))
                new UnlockAlwaysOnDisplay().initZygote(startupParam);
        }

    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        EzXHelper.initHandleLoadPackage(lpparam);
        EzXHelper.setLogTag(TAG);
        EzXHelper.setToastTag(TAG);
        // load CorePatch
        new SystemFrameworkForCorePatch().handleLoadPackage(lpparam);

        // load Module hook apps
        init(lpparam);
        if (mPrefsMap.getBoolean("system_framework_network_flightmode_hotspot"))
            new FlightModeHotSpot().handleLoadPackage(lpparam);
    }

    private void setXSharedPrefs() {
        if (mPrefsMap.isEmpty()) {
            XSharedPreferences mXSharedPreferences;
            try {
                mXSharedPreferences = new XSharedPreferences(ProjectApi.mAppModulePkg, PrefsUtils.mPrefsName);
                mXSharedPreferences.makeWorldReadable();
                Map<String, ?> allPrefs = mXSharedPreferences.getAll();

                if (allPrefs != null && !allPrefs.isEmpty()) {
                    mPrefsMap.putAll(allPrefs);
                } else {
                    mXSharedPreferences = new XSharedPreferences(new File(PrefsUtils.mPrefsFile));
                    mXSharedPreferences.makeWorldReadable();
                    allPrefs = mXSharedPreferences.getAll();

                    if (allPrefs != null && !allPrefs.isEmpty()) {
                        mPrefsMap.putAll(allPrefs);
                    } else {
                        logE("[UID" + Process.myUid() + "]", "Cannot read SharedPreferences, some mods might not work!");
                    }
                }
            } catch (Throwable t) {
                logE("setXSharedPrefs", t);
            }
        }
    }

    private void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (isSafeModeOn) return;
        String packageName = lpparam.packageName;
        if (Objects.equals(packageName, "android"))
            logI(packageName, "androidVersion = " + getAndroidVersion() + ", miuiVersion = " + getMiuiVersion() + ", hyperosVersion = " + getHyperOSVersion());
        else
            logI(packageName, "versionName = " + getPackageVersionName(lpparam) + ", versionCode = " + getPackageVersionCode(lpparam));

        invokeInit(lpparam);
        androidCrashEventHook(lpparam);
    }

    private void invokeInit(XC_LoadPackage.LoadPackageParam lpparam) {
        String mPkgName = lpparam.packageName;
        if (mPkgName == null) return;

        if (ProjectApi.mAppModulePkg.equals(mPkgName)) {
            moduleActiveHook(lpparam);
            return;
        }

        if (isInSafeMode(mPkgName) || isOtherRestrictions(mPkgName)) return;

        HashMap<String, DataBase> dataMap = DataBase.get();
        if (dataMap.values().stream().noneMatch(dataBase -> dataBase.mTargetPackage.equals(mPkgName))) {
            mVariousThirdApps.init(lpparam);
            return;
        }

        dataMap.forEach(new BiConsumer<String, DataBase>() {
            @Override
            public void accept(String s, DataBase dataBase) {
                if (!mPkgName.equals(dataBase.mTargetPackage))
                    return;
                if (!(dataBase.mTargetSdk == -1) && !isAndroidVersion(dataBase.mTargetSdk))
                    return;
                if (!(dataBase.mTargetOSVersion == -1F) && !(isHyperOSVersion(dataBase.mTargetOSVersion) || isMiuiVersion(dataBase.mTargetOSVersion)))
                    return;
                if (dataBase.isPad != isPad())
                    return;

                try {
                    Class<?> clazz = getClass().getClassLoader().loadClass(s);
                    BaseModule module = (BaseModule) clazz.getDeclaredConstructor().newInstance();
                    module.init(lpparam);
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                         InstantiationException | InvocationTargetException e) {
                    logE(TAG, e);
                }
            }
        });
    }

    private void androidCrashEventHook(XC_LoadPackage.LoadPackageParam lpparam) {
        if ("android".equals(lpparam.packageName)) {
            XposedBridge.log("[HyperCeiler][I]: Log level is " + logLevelDesc());
            try {
                new CrashHook(lpparam);
            } catch (Exception e) {
                logE(TAG, e);
            }
        }
    }

    private boolean isInSafeMode(String pkg) {
        switch (pkg) {
            case "com.android.systemui" -> {
                return isSystemUIModuleEnable();
            }
            case "com.miui.home" -> {
                return isHomeModuleEnable();
            }
            case "com.miui.securitycenter" -> {
                return isSecurityCenterModuleEnable();
            }
        }
        return false;
    }

    private boolean isOtherRestrictions(String pkg) {
        switch (pkg) {
            case "com.google.android.webview", "com.miui.contentcatcher",
                 "com.miui.catcherpatch" -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public void moduleActiveHook(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> mHelpers = XposedHelpers.findClassIfExists(ProjectApi.mAppModulePkg + ".utils.Helpers", lpparam.classLoader);

        XposedHelpers.setStaticBooleanField(mHelpers, "isModuleActive", true);
        XposedHelpers.setStaticIntField(mHelpers, "XposedVersion", XposedBridge.getXposedVersion());
        XposedBridge.log("[HyperCeiler][I]: Log level is " + logLevelDesc());
    }


    private boolean isSafeModeEnable(String key) {
        return mPrefsMap.getBoolean(key);
    }

    private boolean isSystemUIModuleEnable() {
        return isSafeModeEnable("system_ui_safe_mode_enable");
    }

    private boolean isHomeModuleEnable() {
        return isSafeModeEnable("home_safe_mode_enable");
    }

    private boolean isSecurityCenterModuleEnable() {
        return isSafeModeEnable("security_center_safe_mode_enable");
    }
}
