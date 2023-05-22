package com.sevtinge.cemiuiler.module.securitycenter.lab;

import android.content.ComponentName;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.module.securitycenter.SecurityCenterDexKit;
import com.sevtinge.cemiuiler.utils.Helpers;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;
import io.luckypray.dexkit.descriptor.member.DexClassDescriptor;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.sevtinge.cemiuiler.utils.Helpers.getPackageVersionCode;

public class BlurLocationEnable extends BaseHook {

    Class<?> mLab;
    Class<?> mStableVer;

    Class<?> utilCls;
    Class<?> labUtils;

    @Override
    public void init() {
        try {
            List<DexClassDescriptor> result = Objects.requireNonNull(SecurityCenterDexKit.mSecurityCenterResultClassMap.get("LabUtils"));
            for (DexClassDescriptor descriptor : result) {
                labUtils = descriptor.getClassInstance(lpparam.classLoader);
                log("labUtils class is " + labUtils);
                findAndHookMethod("com.miui.permcenter.settings.PrivacyLabActivity", "onCreateFragment", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        Object fm = Helpers.getStaticObjectFieldSilently(labUtils, "b");
                        if (fm != null) {
                            try {
                                Map<String, Integer> featMap = (Map<String, Integer>) fm;
                                featMap.put("mi_lab_blur_location_enable", 0);
                                //featMap.put("mi_lab_blur_location_enable", 0);
                            } catch (Throwable ignore) {
                            }
                        }
                    }
                });
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        /*mLab = findClassIfExists("com.miui.permcenter.q");
        mStableVer = findClassIfExists("miui.os.Build");

        findAndHookMethod(mLab, "h", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });*/

        /*
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

         */

        //findAndHookMethod(mStableVer, "IS_STABLE_VERSION", new MethodHook() {
        //    @Override
        //    protected void before(MethodHookParam param) throws Throwable {
        //        param.setResult(true);
        //    }
        //});
    }

}

