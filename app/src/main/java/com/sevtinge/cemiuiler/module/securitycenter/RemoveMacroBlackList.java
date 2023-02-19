package com.sevtinge.cemiuiler.module.securitycenter;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.lang.reflect.Field;

public class RemoveMacroBlackList extends BaseHook {

    Class<?> m;

    @Override
    public void init() {
        char letter = 'a';

        for (int i = 0; i < 26; i++) {
            m = findClass("com.miui.gamebooster.utils." + letter + "0");
            if (m != null) {
                int length = m.getDeclaredMethods().length;
                if (length >= 10 && length <= 15) {
                    Field[] fields = m.getFields();
                    if (fields.length == 0 && m.getDeclaredFields().length >= 2) {
                        findAndHookMethod(m,"c", String.class, new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) throws Throwable {
                                param.setResult(false);
                            }
                        });
                    }
                    continue;
                }
            }
            letter = (char) (letter + 1);
        }
    }
}
