package com.sevtinge.cemiuiler.module.thememanager;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XposedHelpers;

public class ThemeCrack extends BaseHook {

    @Override
    public void init() {
        findAndHookConstructor("com.android.thememanager.detail.theme.model.OnlineResourceDetail", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.setBooleanField(param.thisObject, "bought", true);
            }
        });
        findAndHookConstructor("com.android.thememanager.model.LargeIconElement", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.setBooleanField(param.thisObject, "hasBought", true);
            }
        });
    }

}
