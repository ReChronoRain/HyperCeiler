package com.sevtinge.hyperceiler.module.hook.personalassistant;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class EnableFoldWidget extends BaseHook {

    Class<?> c;
    Class<?> m2;
    Class<?> ga;

    @Override
    public void init() {

        c = findClassIfExists("b.z.g");

        m2 = findClassIfExists("c.h.e.i.b");

        ga = findClassIfExists("c.h.e.p.ga");

        findAndHookMethod(c, "a", Context.class, String.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.setStaticObjectField(m2, "a", "fold");
            }
        });

    }
}
