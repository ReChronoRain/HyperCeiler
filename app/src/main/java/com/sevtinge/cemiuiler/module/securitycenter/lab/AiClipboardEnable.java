package com.sevtinge.cemiuiler.module.securitycenter.lab;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.module.securitycenter.SecurityCenterDexKit;
import com.sevtinge.cemiuiler.utils.Helpers;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.luckypray.dexkit.descriptor.member.DexClassDescriptor;

public class AiClipboardEnable extends BaseHook {

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
                    protected void before(MethodHookParam param) {
                        Object fm = Helpers.getStaticObjectFieldSilently(labUtils, "b");
                        if (fm != null) {
                            try {
                                Map<String, Integer> featMap = (Map<String, Integer>) fm;
                                featMap.put("mi_lab_ai_clipboard_enable", 0);
                                // featMap.put("mi_lab_blur_location_enable", 0);
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

        findAndHookMethod(mLab, "e", new MethodHook() {
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
                            featMap.put("mi_lab_ai_clipboard_enable", 0);
                            //featMap.put("mi_lab_blur_location_enable", 0);
                        } catch (Throwable ignore) {
                        }
                    }
                }
            }
        });

         */

        /*findAndHookMethod("com.miui.permcenter.utils.h", "<clinit>", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setBooleanField(param.thisObject, "IS_STABLE_VERSION", true);
            }
            //protected void before(MethodHookParam param) throws Throwable {
            //    param.setResult(true);
            //}
            protected void after(MethodHookParam param) throws Throwable {
                XC_MethodHook.Unhook();
            }
        });*/
    }
}


