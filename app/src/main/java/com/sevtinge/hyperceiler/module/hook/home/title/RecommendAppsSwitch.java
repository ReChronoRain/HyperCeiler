package com.sevtinge.hyperceiler.module.hook.home.title;

import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class RecommendAppsSwitch extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.home.launcher.Folder",
            "showRecommendAppsSwitch", boolean.class, boolean.class,
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    View mRecommendAppsSwitch = (View) XposedHelpers.getObjectField(param.thisObject,
                        "mRecommendAppsSwitch");
                    mRecommendAppsSwitch.setVisibility(View.GONE);
                }
            }
        );
    }
}
