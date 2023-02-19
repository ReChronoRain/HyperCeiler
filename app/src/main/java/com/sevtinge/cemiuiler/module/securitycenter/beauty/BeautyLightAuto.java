package com.sevtinge.cemiuiler.module.securitycenter.beauty;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;

public class BeautyLightAuto extends BaseHook {
        @Override
        public void init() {
            findAndHookMethod("com.miui.gamebooster.beauty.i", "i", XC_MethodReplacement.returnConstant(true));
            /*findAndHookMethod("com.miui.gamebooster.utils.i", "i", new BaseHook.MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });*/
        }
    }



