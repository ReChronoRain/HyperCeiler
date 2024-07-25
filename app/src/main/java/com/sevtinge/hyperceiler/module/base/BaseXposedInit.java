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
package com.sevtinge.hyperceiler.module.base;

import static com.sevtinge.hyperceiler.utils.Helpers.getPackageVersionCode;
import static com.sevtinge.hyperceiler.utils.Helpers.getPackageVersionName;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getMiuiVersion;
import static com.sevtinge.hyperceiler.utils.log.LogManager.logLevelDesc;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logI;

import android.os.Process;

import androidx.annotation.CallSuper;

import com.sevtinge.hyperceiler.module.app.VariousSystemApps;
import com.sevtinge.hyperceiler.module.app.VariousThirdApps;
import com.sevtinge.hyperceiler.module.base.tool.ResourcesTool;
import com.sevtinge.hyperceiler.safe.CrashHook;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.utils.prefs.PrefsMap;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseXposedInit {

    private static final String TAG = "BaseXposedInit";
    public static boolean isSafeModeOn = false;
    public static String mModulePath = null;
    public static PrefsMap<String, Object> mPrefsMap = new PrefsMap<>();
    public static ResourcesTool mResHook;
    // public static XmlTool mXmlTool;
    public final VariousThirdApps mVariousThirdApps = new VariousThirdApps();
    public final VariousSystemApps mVariousSystemApps = new VariousSystemApps();
    private HashMap<String, DataBase.DataHelper> dataMap = null;

    @CallSuper
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        setXSharedPrefs();
        mResHook = new ResourcesTool(startupParam.modulePath);
        // mXmlTool = new XmlTool(startupParam);
        mModulePath = startupParam.modulePath;
    }

    private void setXSharedPrefs() {
        if (mPrefsMap.isEmpty()) {
            XSharedPreferences mXSharedPreferences;
            try {
                mXSharedPreferences = new XSharedPreferences(ProjectApi.mAppModulePkg, PrefsUtils.mPrefsName);
                mXSharedPreferences.makeWorldReadable();

                Map<String, ?> allPrefs = mXSharedPreferences == null ? null : mXSharedPreferences.getAll();
                if (allPrefs == null || allPrefs.isEmpty()) {
                    mXSharedPreferences = new XSharedPreferences(new File(PrefsUtils.mPrefsFile));
                    mXSharedPreferences.makeWorldReadable();
                    allPrefs = mXSharedPreferences == null ? null : mXSharedPreferences.getAll();
                    if (allPrefs == null || allPrefs.isEmpty()) {
                        logE(
                                "[UID" + Process.myUid() + "]",
                                "Cannot read module's SharedPreferences, some mods might not work!"
                        );
                    } else {
                        mPrefsMap.putAll(allPrefs);
                    }
                } else {
                    mPrefsMap.putAll(allPrefs);
                }
            } catch (Throwable t) {
                logE("setXSharedPrefs", t);
            }
        }
    }

    public void init(LoadPackageParam lpparam) {
        if (isSafeModeOn) return;
        initLog(lpparam);
        invokeInit(lpparam);
        androidCrash(lpparam);
    }


    private void invokeInit(LoadPackageParam lpparam) {
        if (dataMap == null) {
            dataMap = DataBase.get();
        }
        String mPkgName = lpparam.packageName;
        if (ProjectApi.mAppModulePkg.equals(mPkgName)) {
            ModuleActiveHook(lpparam);
            return;
        }
        if (mPkgName == null) return;
        if (isInSafeMode(mPkgName)) return;
        if (isOtherRestrictions(mPkgName)) return;
        DataBase.DataHelper helper = dataMap.get(mPkgName);
        if (helper == null) {
            mVariousThirdApps.init(lpparam);
            return;
        }
        Class<?> clazz;
        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader == null) return;
        try {
            clazz = classLoader.loadClass(helper.fullName);
        } catch (ClassNotFoundException e) {
            logE(TAG, e);
            return;
        }
        boolean isPad = helper.isPad;
        int android = helper.android;
        // 等待改写...
        // boolean skip = helper.skip;
        // if (skip) continue;
        // 需要限制安卓版本和设备取消这些注释，并删除下面的invoke方法。
        // if (!isAndroidVersion(android)) continue;
        // if (isPad() && isPad) {
        //     return invoke(lpparam, clzz);
        // } else if (isPad() && !isPad) {
        //     continue;
        // } else {
        //     return invoke(lpparam, clzz);
        // }
        invoke(lpparam, clazz);
        mVariousSystemApps.init(lpparam);
    }

    private void invoke(LoadPackageParam lpparam, Class<?> clzz) {
        Object newInstance;
        try {
            newInstance = clzz.newInstance();
            Method[] methods = clzz.getMethods();
            for (Method method : methods) {
                if ("init".equals(method.getName())) {
                    method.invoke(newInstance, lpparam);
                    return;
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private void initLog(LoadPackageParam lpparam) {
        String packageName = lpparam.packageName;
        if (Objects.equals(packageName, "android"))
            logI(packageName, "androidVersion = " + getAndroidVersion() + ", miuiVersion = " + getMiuiVersion() + ", hyperosVersion = " + getHyperOSVersion());
        else
            logI(packageName, "versionName = " + getPackageVersionName(lpparam) + ", versionCode = " + getPackageVersionCode(lpparam));
    }

    private void androidCrash(LoadPackageParam lpparam) {
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
        // switch (pkg) {
        //     case "com.lbe.security.miui" -> {
        //         return isMoreHyperOSVersion(1f);
        //     }
        // }
        return false;
    }

    public void ModuleActiveHook(LoadPackageParam lpparam) {
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
