package com.sevtinge.cemiuiler.module.home.layout;

import android.content.Context;
import android.os.Bundle;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class UnlockGrids extends BaseHook {

    Class<?> mDeviceConfig;

    @Override
    public void init() {

        /*
        mDeviceConfig = findClassIfExists("com.miui.home.launcher.DeviceConfig");

        hookAllMethodsSilently("com.miui.home.launcher.compat.LauncherCellCountCompatDevice", "shouldUseDeviceValue", XC_MethodReplacement.returnConstant(false));

        findAndHookMethod("com.miui.home.settings.MiuiHomeSettings", "onCreatePreferences", Bundle.class, String.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mScreenCellsConfig"), "setVisible", true);
            }
        });

        findAndHookMethod(mDeviceConfig, "loadCellsCountConfig", Context.class, boolean.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                int sCellCountY = (int) XposedHelpers.getStaticObjectField(mDeviceConfig, "sCellCountY");
                if (sCellCountY > 6) {
                    int cellHeight = (int) XposedHelpers.callStaticMethod(mDeviceConfig, "getCellHeight");
                    XposedHelpers.setStaticObjectField(mDeviceConfig, "sFolderCellHeight", cellHeight);
                }
            }
        });

        UnlockGridsRes();

         */


        hookAllMethodsSilently("com.miui.home.launcher.compat.LauncherCellCountCompatDevice", "shouldUseDeviceValue", XC_MethodReplacement.returnConstant(false));
        findAndHookMethod("com.miui.home.settings.MiuiHomeSettings", "onCreatePreferences", Bundle.class, String.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.callMethod(XposedHelpers.getObjectField(param.thisObject, "mScreenCellsConfig"), "setVisible", true);
            }
        });
        Class<?> DeviceConfigClass = XposedHelpers.findClass("com.miui.home.launcher.DeviceConfig", lpparam.classLoader);
        Helpers.findAndHookMethod(DeviceConfigClass, "loadCellsCountConfig", Context.class, boolean.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                int sCellCountY = (int) XposedHelpers.getStaticObjectField(DeviceConfigClass, "sCellCountY");
                if (sCellCountY > 6) {
                    int cellHeight = (int) XposedHelpers.callStaticMethod(DeviceConfigClass, "getCellHeight");
                    XposedHelpers.setStaticObjectField(DeviceConfigClass, "sFolderCellHeight", cellHeight);
                }
            }
        });
        findAndHookMethod("com.miui.home.launcher.ScreenUtils", "getScreenCellsSizeOptions", Context.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                ArrayList<CharSequence> arrayList = new ArrayList<>();
                int cellCountXMin = 3;
                int cellCountXMax = 16;
                int cellCountYMin = 4;
                int cellCountYMax = 18;
                while (cellCountXMin <= cellCountXMax) {
                    for (int i = cellCountYMin; i <= cellCountYMax; i++) {
                        arrayList.add(cellCountXMin + "x" + i);
                    }
                    cellCountXMin++;
                }
                param.setResult(arrayList);
            }
        });

        Helpers.findAndHookMethod("com.miui.home.launcher.compat.LauncherCellCountCompatNoWord", lpparam.classLoader, "setLoadResCellConfig", boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.args[0] = true;
            }
        });

        Helpers.hookAllMethods("com.miui.home.launcher.DeviceConfig", lpparam.classLoader, "isCellSizeChangedByTheme", new MethodHook() {
            XC_MethodHook.Unhook nowordHook;

            @Override
            protected void before(MethodHookParam param) throws Throwable {
                nowordHook = Helpers.findAndHookMethodUseUnhook("com.miui.home.launcher.common.Utilities", lpparam.classLoader, "isNoWordModel", XC_MethodReplacement.returnConstant(false));
            }

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                if (nowordHook != null) nowordHook.unhook();
                nowordHook = null;
            }
        });

        UnlockGridsRes();
    }

    public void UnlockGridsRes() {
        mResHook.setObjectReplacement("com.miui.home", "integer", "config_cell_count_x", 3);
        mResHook.setObjectReplacement("com.miui.home", "integer", "config_cell_count_y", 4);
        mResHook.setObjectReplacement("com.miui.home", "integer", "config_cell_count_x_min", 3);
        mResHook.setObjectReplacement("com.miui.home", "integer", "config_cell_count_y_min", 4);
        mResHook.setObjectReplacement("com.miui.home", "integer", "config_cell_count_x_max", 16);
        mResHook.setObjectReplacement("com.miui.home", "integer", "config_cell_count_y_max", 18);
    }
}
