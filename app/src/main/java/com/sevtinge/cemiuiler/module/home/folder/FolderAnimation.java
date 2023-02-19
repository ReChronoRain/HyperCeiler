package com.sevtinge.cemiuiler.module.home.folder;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FolderAnimation extends BaseHook {

    Class<?> mLauncher;
    Class<?> mSpringAnimator;

    @Override
    public void init() {

        mSpringAnimator = findClassIfExists("com.miui.home.launcher.animate.SpringAnimator");

        for (int i = 47; i <= 60; i++) {
            try {
                mLauncher = findClass("com.miui.home.launcher.Launcher$" + i);
                for (Method method : mLauncher.getDeclaredMethods()) {
                    for (Field field : mLauncher.getDeclaredFields()) {
                        if (field.getName().equals("val$folderInfo")) {

                            findAndHookMethod(mLauncher, "run", new MethodHook() {

                                @Override
                                protected void before(MethodHookParam param) throws Throwable {

                                    findAndHookMethod(mSpringAnimator, "setDampingResponse", float.class, float.class, new MethodHook() {
                                        @Override
                                        protected void before(MethodHookParam param) throws Throwable {
                                            param.args[0] = 0.5f;
                                            param.args[1] = 0.5f;
                                        }
                                    });
                                }

                                @Override
                                protected void after(MethodHookParam param) throws Throwable {
                                    findAndHookMethod(mSpringAnimator, "setDampingResponse", float.class, float.class, new MethodHook() {
                                        @Override
                                        protected void before(MethodHookParam param) throws Throwable {
                                            param.args[0] = 0.9f;
                                            param.args[1] = 0.3f;
                                        }
                                    });
                                }
                            });
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                continue;
            }
        }
    }
}
