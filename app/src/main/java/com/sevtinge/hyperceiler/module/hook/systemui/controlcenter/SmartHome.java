package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import android.content.pm.ApplicationInfo;
import android.widget.RelativeLayout;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.Helpers;

import de.robv.android.xposed.XposedHelpers;

public class SmartHome extends BaseHook {

    private ClassLoader mPluginLoader = null;

    @Override
    public void init() {
        findAndHookMethod("com.android.systemui.shared.plugins.PluginManagerImpl", "getClassLoader", ApplicationInfo.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ApplicationInfo appInfo = (ApplicationInfo) param.args[0];
                if ("miui.systemui.plugin".equals(appInfo.packageName)) {
                    if (mPluginLoader == null) {
                        mPluginLoader = (ClassLoader) param.getResult();
                        Helpers.findAndHookMethod("miui.systemui.devicecontrols.ui.MiuiControlsUiControllerImpl", mPluginLoader, "updateOrientation", new MethodHook() {
                            @Override
                            protected void after(MethodHookParam param) throws Throwable {
                                RelativeLayout mParent = (RelativeLayout) XposedHelpers.getObjectField(param.thisObject, "parent");
                            }
                        });
                    }
                }
            }
        });
    }
}
