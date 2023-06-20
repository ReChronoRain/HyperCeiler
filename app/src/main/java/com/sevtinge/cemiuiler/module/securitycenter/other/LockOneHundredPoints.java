package com.sevtinge.cemiuiler.module.securitycenter.other;

import android.view.View;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.module.securitycenter.SecurityCenterDexKit;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;

public class LockOneHundredPoints extends BaseHook {

    Class<?> mScoreManagerCls;
    Class<?> mMainContentFrameCls;

    @Override
    public void init() {

        mScoreManagerCls = findClassIfExists("com.miui.securityscan.scanner.ScoreManager");
        mMainContentFrameCls = findClassIfExists("com.miui.securityscan.ui.main.MainContentFrame");

        try {
            List<DexMethodDescriptor> result = Objects.requireNonNull(SecurityCenterDexKit.mSecurityCenterResultMap.get("ScoreManager"));
            for (DexMethodDescriptor descriptor : result) {
                Method lockOneHundredPoints = descriptor.getMethodInstance(lpparam.classLoader);
                log("lock 100 points method is " + lockOneHundredPoints);
                if (lockOneHundredPoints.getReturnType() == int.class) {
                    XposedBridge.hookMethod(lockOneHundredPoints, XC_MethodReplacement.returnConstant(0));
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        /*findAndHookMethod(mScoreManagerCls, "B", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if(PrefsUtils.mSharedPreferences.getBoolean("prefs_key_security_center_score", false)) param.setResult(0);
            }
        });*/

        findAndHookMethod(mMainContentFrameCls, "onClick", View.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (PrefsUtils.mSharedPreferences.getBoolean("prefs_key_security_center_score", false))
                    param.setResult(null);
            }
        });
    }
}
