package com.sevtinge.cemiuiler.module.hook.home.navigation;

import android.view.WindowManager;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class BackGestureAreaHeight extends BaseHook {
    @Override
    public void init() {
        findAndHookMethodSilently("com.miui.home.recents.GestureStubView",  "getGestureStubWindowParam", new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                WindowManager.LayoutParams lp = (WindowManager.LayoutParams)param.getResult();
                int pct = mPrefsMap.getInt("home_navigation_back_area_height", 60);  //记得改key
                lp.height = Math.round(lp.height / 60.0f * pct);
                param.setResult(lp);
            }
        });
    }
}
