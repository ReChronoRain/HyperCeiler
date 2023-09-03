package com.sevtinge.cemiuiler.module.hook.personalassistant;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class PadWidgetEnable extends BaseHook {

    Class<?> m = findClassIfExists("c.h.e.k.k.c.d");

    Class<?> m2 = findClassIfExists("c.h.e.p.s");

    public enum DeviceType {
        PAD, FOLDABLE_DEVICE, PHONE
    }

    @Override
    public void init() {
        hookAllMethods(m2, "c", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.callMethod(XposedHelpers.findClassIfExists("miui.os.Build", lpparam.classLoader), "isTablet", true);
                XposedHelpers.setStaticBooleanField(XposedHelpers.findClassIfExists("miui.os.Build", lpparam.classLoader), "IS_TABLET", true);
            }
        });

        /*findAndHookMethod(m,"a", Build.class, Context.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                findAndHookMethod(m2,"c", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        XposedHelpers.setStaticBooleanField(XposedHelpers.findClassIfExists("miui.os.Build",lpparam.classLoader),"IS_TABLET", true);
                    }
                });
            }
        });*/
    }
}
