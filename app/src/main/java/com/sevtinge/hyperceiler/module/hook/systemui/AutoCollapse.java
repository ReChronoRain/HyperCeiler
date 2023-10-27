package com.sevtinge.hyperceiler.module.hook.systemui;

import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.Helpers;

import de.robv.android.xposed.XposedHelpers;

public class AutoCollapse extends BaseHook {
    @Override
    public void init() {
        Helpers.findAndHookMethod("com.android.systemui.qs.tileimpl.QSTileImpl", lpparam.classLoader, "click", View.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                Object mState = XposedHelpers.callMethod(param.thisObject, "getState");
                int state = XposedHelpers.getIntField(mState, "state");
                if (state != 0) {
                    String tileSpec = (String) XposedHelpers.callMethod(param.thisObject, "getTileSpec");
                    if (!"edit".equals(tileSpec)) {
                        Object mHost = XposedHelpers.getObjectField(param.thisObject, "mHost");
                        XposedHelpers.callMethod(mHost, "collapsePanels");
                    }
                }
            }
        });
    }
}
