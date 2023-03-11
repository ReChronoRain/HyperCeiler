package com.sevtinge.cemiuiler.module.mms;

import android.content.Context;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XposedHelpers;

public class DisableAd extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.miui.smsextra.ui.BottomMenu", "allowMenuMode", Context.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
        findAndHookMethod("j.b.c.b0.n3", "j", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
        findAndHookConstructor("j.l.b.x.h.d", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setBooleanField(param.thisObject, "k", true);
            }
        });
    }
}
