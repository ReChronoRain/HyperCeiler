package com.sevtinge.cemiuiler.module.securitycenter.lab;

import android.content.ComponentName;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;

import java.io.File;
import java.util.Map;

import static com.sevtinge.cemiuiler.utils.Helpers.getPackageVersionCode;

public class BlurLocationEnable extends BaseHook {

    Class<?> mLab;
    Class<?> mStableVer;

    Class<?> utilCls;

    @Override
    public void init() {
        /*mLab = findClassIfExists("com.miui.permcenter.q");
        mStableVer = findClassIfExists("miui.os.Build");

        findAndHookMethod(mLab, "h", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });*/

        Helpers.findAndHookMethod("com.miui.permcenter.settings.PrivacyLabActivity", lpparam.classLoader, "onCreateFragment", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                int appVersionCode = getPackageVersionCode(lpparam);
                if (appVersionCode >= 40000749) {
                    utilCls = findClassIfExists("rb.e", lpparam.classLoader);
                } else {
                    utilCls = findClassIfExists("com.miui.permcenter.utils.h", lpparam.classLoader);
                }
                if (utilCls != null) {
                    Object fm = Helpers.getStaticObjectFieldSilently(utilCls, "b");
                    if (fm != null) {
                        try {
                            Map<String, Integer> featMap = (Map<String, Integer>) fm;
                            //featMap.put("mi_lab_ai_clipboard_enable", 0);
                            featMap.put("mi_lab_blur_location_enable", 0);
                        } catch (Throwable ignore) {
                        }
                    }
                }
            }
        });

        //findAndHookMethod(mStableVer, "IS_STABLE_VERSION", new MethodHook() {
        //    @Override
        //    protected void before(MethodHookParam param) throws Throwable {
        //        param.setResult(true);
        //    }
        //});
    }

}

