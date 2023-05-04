package com.sevtinge.cemiuiler.module.securitycenter.lab;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

import java.util.Map;

import static com.sevtinge.cemiuiler.utils.Helpers.getPackageVersionCode;

public class AiClipboardEnable extends BaseHook {

    Class<?> mLab;
    Class<?> mStableVer;
    Class<?> utilCls;


    @Override
    public void init() {
        /*mLab = findClassIfExists("com.miui.permcenter.q");
        mStableVer = findClassIfExists("miui.os.Build");

        findAndHookMethod(mLab, "e", new MethodHook() {
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
                            featMap.put("mi_lab_ai_clipboard_enable", 0);
                            //featMap.put("mi_lab_blur_location_enable", 0);
                        } catch (Throwable ignore) {
                        }
                    }
                }
            }
        });

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


