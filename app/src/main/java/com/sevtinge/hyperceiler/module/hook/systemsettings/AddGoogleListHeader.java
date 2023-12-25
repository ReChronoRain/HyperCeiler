package com.sevtinge.hyperceiler.module.hook.systemsettings;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class AddGoogleListHeader extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Class<?> mMiuiSettings = findClassIfExists("com.android.settings.MiuiSettings");
        findAndHookMethod(mMiuiSettings, "updateHeaderList", List.class, new MethodHook(){
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                List<?> list = (List<?>) param.args[0];
                XposedHelpers.callMethod(param.thisObject, "AddGoogleSettingsHeaders", list);
            }
        });
    }
}
