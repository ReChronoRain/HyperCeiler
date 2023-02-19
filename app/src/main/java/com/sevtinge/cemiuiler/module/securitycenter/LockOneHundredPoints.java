package com.sevtinge.cemiuiler.module.securitycenter;

import android.view.View;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

public class LockOneHundredPoints extends BaseHook {

    Class<?> mScoreManagerCls;
    Class<?> mMainContentFrameCls;

    @Override
    public void init() {

        mScoreManagerCls = findClassIfExists("com.miui.securityscan.scanner.ScoreManager");
        mMainContentFrameCls = findClassIfExists("com.miui.securityscan.ui.main.MainContentFrame");

        findAndHookMethod(mScoreManagerCls, "B", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if(PrefsUtils.mSharedPreferences.getBoolean("prefs_key_security_center_score", false)) param.setResult(0);
            }
        });

        findAndHookMethod(mMainContentFrameCls, "onClick", View.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if(PrefsUtils.mSharedPreferences.getBoolean("prefs_key_security_center_score", false)) param.setResult(null);
            }
        });
    }
}
