package com.sevtinge.cemiuiler.module.clock;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.io.File;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.sevtinge.cemiuiler.utils.Helpers.getPackageVersionCode;

public class EnableHourGlass extends BaseHook {
    @Override
    public void init() {
        int appVersionCode = getPackageVersionCode(lpparam);
            hookAllMethods("com.android.deskclock.util.Util", "isHourGlassEnable", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    try {
                        param.setResult(true);
                    } catch(Throwable e) {
                        XposedBridge.log("Cemiuiler: Your clock versionCode is " + appVersionCode);
                        XposedBridge.log("Cemiuiler: Please revert to a supported version yourself");
                        XposedBridge.log(e);
                    }
                }
            });
    }
}
