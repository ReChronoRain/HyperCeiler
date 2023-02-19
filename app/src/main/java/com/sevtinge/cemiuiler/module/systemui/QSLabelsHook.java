package com.sevtinge.cemiuiler.module.systemui;

import android.content.pm.ApplicationInfo;
import android.view.View;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class QSLabelsHook extends BaseHook {

    final boolean[] isHooked = {false};
    private static ClassLoader pluginLoader = null;

    Class<?> mQSController;

    @Override
    public void init() {

        findAndHookMethod("com.android.systemui.shared.plugins.PluginManagerImpl", "getClassLoader", ApplicationInfo.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ApplicationInfo appInfo = (ApplicationInfo) param.args[0];

                if ("miui.systemui.plugin".equals(appInfo.packageName) && !isHooked[0]) {
                    isHooked[0] = true;
                    if (pluginLoader == null) {
                        pluginLoader = (ClassLoader) param.getResult();
                    }

                    mQSController = XposedHelpers.findClassIfExists("miui.systemui.controlcenter.qs.tileview.StandardTileView", pluginLoader);

                    hookAllMethods(mQSController, "init", new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            if (param.args.length != 1) return;
                            View mLabelContainer = (View)XposedHelpers.getObjectField(param.thisObject, "labelContainer");
                            if (mLabelContainer != null) {
                                mLabelContainer.setVisibility(
                                        mPrefsMap.getBoolean("system_ui_qs_label") ? View.GONE : View.VISIBLE
                                );
                            }
                        }
                    });
                }
            }
        });
    }
}
