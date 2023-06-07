package com.sevtinge.cemiuiler.module.securitycenter.lab;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.module.securitycenter.SecurityCenterDexKit;
import com.sevtinge.cemiuiler.utils.Helpers;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.luckypray.dexkit.descriptor.member.DexClassDescriptor;

public class GetNumberEnable extends BaseHook {

    Class<?> mLab;
    Class<?> mStableVer;
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
                                featMap.put("mi_lab_operator_get_number_enable", 0);
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

        /*findAndHookMethod(mLab, "f", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });*/
        // findAndHookMethod(mStableVer, "IS_STABLE_VERSION", new MethodHook() {
        //    @Override
        //    protected void before(MethodHookParam param) throws Throwable {
        //        param.setResult(true);
        //    }
        //});
    }

}
