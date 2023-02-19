package com.sevtinge.cemiuiler.module.various;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.LogUtils;

import de.robv.android.xposed.XposedHelpers;
import miui.app.ActionBar;

public class CollapseMiuiTitle extends BaseHook {

    @Override
    public void init() {
        Class<?> abvCls = findClassIfExists("com.miui.internal.widget.AbsActionBarView");

        int opt = mPrefsMap.getStringAsInt("various_collapse_miui_title", 0);

        if (abvCls != null)
            hookAllConstructors(abvCls, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    XposedHelpers.setIntField(param.thisObject, "mExpandState", ActionBar.STATE_EXPAND);
                    XposedHelpers.setIntField(param.thisObject, "mInnerExpandState", ActionBar.STATE_COLLAPSE);
                    if (opt == 2) XposedHelpers.setBooleanField(param.thisObject, "mResizable", false);
                }
            });

        abvCls = findClassIfExists("miuix.appcompat.internal.app.widget.ActionBarView");
        if (abvCls != null)
            hookAllConstructors(abvCls, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    try {
                        setExpandState(param.thisObject, opt == 1 || opt == 3);
                        setResizable(param.thisObject, opt == 3 || opt == 4);
                    } catch (Throwable ignore) {
                        LogUtils.log(ignore);
                    }
                }
            });
    }

    private void setExpandState(Object obj, boolean state) {
        if (state) {
            XposedHelpers.callMethod(obj, "setExpandState", ActionBar.STATE_COLLAPSE);
        } else {
            XposedHelpers.callMethod(obj, "setExpandState", ActionBar.STATE_EXPAND);
        }
    }

    private void setResizable(Object obj, boolean state) {
        if (state) {
            XposedHelpers.callMethod(obj, "setResizable", false);
        } else {
            XposedHelpers.callMethod(obj, "setResizable", true);
        }
    }
}
