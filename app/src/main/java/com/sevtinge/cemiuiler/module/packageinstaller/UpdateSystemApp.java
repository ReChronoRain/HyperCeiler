package com.sevtinge.cemiuiler.module.packageinstaller;

import android.content.pm.ApplicationInfo;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.lang.reflect.Method;
import java.util.List;

public class UpdateSystemApp extends BaseHook {

    Class<?> mClz;

    @Override
    public void init() {
        findAndHookMethod("android.os.SystemProperties", "getBoolean", String.class, boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if ("persist.sys.allow_sys_app_update".equals(param.args[0])) {
                    param.setResult(true);
                }
            }
        });

        char letterClz = 'a';

        for (int i = 0; i < 26; i++) {
            mClz = findClass("j2." + letterClz);
            if (mClz != null) {
                int length = mClz.getDeclaredMethods().length;
                if (length >= 15 && length <= 25) {
                    List<Method> methods = List.of(mClz.getDeclaredMethods());
                    for (Method method : methods) {
                        try {
                            if (method.getParameterTypes()[0] == ApplicationInfo.class) {
                                hookMethod(method, new MethodHook() {
                                    @Override
                                    protected void before(MethodHookParam param) throws Throwable {
                                        param.setResult(false);
                                    }
                                });
                                break;

                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
            }
            letterClz = (char) (letterClz + 1);
        }
    }
}
