package com.sevtinge.hyperceiler.module.hook.home.recent;

import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class HideCleanUp extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        hookAllMethods(
            findClassIfExists("com.miui.home.recents.views.RecentsContainer"),
            "onFinishInflate",
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    View mView = (View) XposedHelpers.getObjectField(param.thisObject, "mClearAnimView");
                    mView.setVisibility(View.GONE);
                }
            }
        );
    }
}
