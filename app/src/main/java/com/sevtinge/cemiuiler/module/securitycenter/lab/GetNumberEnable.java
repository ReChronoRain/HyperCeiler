package com.sevtinge.cemiuiler.module.securitycenter.lab;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;

import static com.sevtinge.cemiuiler.utils.Helpers.getPackageVersionCode;

public class GetNumberEnable extends BaseHook {

    Class<?> mLab;
    Class<?> mStableVer;

    @Override
    public void init() {
        int appVersionCode = getPackageVersionCode(lpparam);
        if (appVersionCode >= 40000749) {
            mLab = findClassIfExists("pa.r");
        }else{
            mLab = findClassIfExists("com.miui.permcenter.q");
        }
        mStableVer = findClassIfExists("miui.os.Build");

        findAndHookMethod(mLab, "i", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });

        /*findAndHookMethod(mLab, "f", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });*/
        //findAndHookMethod(mStableVer, "IS_STABLE_VERSION", new MethodHook() {
        //    @Override
        //    protected void before(MethodHookParam param) throws Throwable {
        //        param.setResult(true);
        //    }
        //});
    }

}
