package com.sevtinge.cemiuiler.module.securitycenter.beauty;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;

import static com.sevtinge.cemiuiler.utils.Helpers.getPackageVersionCode;

public class BeautyLight extends BaseHook {
    @Override
    public void init() {
        int appVersionCode = getPackageVersionCode(lpparam);
        if (appVersionCode == 40000749 || appVersionCode == 40000750) {
            //hookAllMethods("p5.f", "m", XC_MethodReplacement.returnConstant(true));
            hookAllMethods("p5.f", "G", XC_MethodReplacement.returnConstant(true));
        } else if (appVersionCode == 40000754 || (appVersionCode >= 40000771 && appVersionCode <= 40000779 && appVersionCode != 40000772 && appVersionCode != 40000774 && appVersionCode != 40000777)) {
            findAndHookMethod("com.miui.gamebooster.beauty.l", "j", new BaseHook.MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } else if (appVersionCode == 40000772) {
            findAndHookMethod("n5.f", "G", new BaseHook.MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } else if (appVersionCode == 40000774) {
            findAndHookMethod("n5.f", "H", new BaseHook.MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } else if (appVersionCode == 40000777 || appVersionCode == 40000780) {
            findAndHookMethod("p5.f", "H", new BaseHook.MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } else {
            findAndHookMethod("com.miui.gamebooster.utils.o", "c", XC_MethodReplacement.returnConstant(true));
        }
        /*findAndHookMethod("com.miui.gamebooster.utils.o", "c", new BaseHook.MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });*/
    }

}

