package com.sevtinge.cemiuiler.module.home.folder;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class FolderBlur extends BaseHook {

    Class<?> mLauncher;
    Class<?> mBlurUtils;
    Class<?> mFolderInfo;
    Class<?> mLauncherState;
    Class<?> mCancelShortcutMenuReason;

    boolean isFolderShowing;
    boolean isShowEditPanel;

    @Override
    public void init() {

        mLauncher = findClassIfExists("com.miui.home.launcher.Launcher");
        mBlurUtils = findClassIfExists("com.miui.home.launcher.common.BlurUtils");
        mFolderInfo = findClassIfExists("com.miui.home.launcher.FolderInfo");
        mLauncherState = findClassIfExists("com.miui.home.launcher.LauncherState");
        mCancelShortcutMenuReason = findClassIfExists("com.miui.home.launcher.shortcuts.CancelShortcutMenuReason");

        /*findAndHookMethod(mBlurUtils, "isUserBlurWhenOpenFolder", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });*/

        if (mBlurUtils != null) {
            findAndHookMethod(mLauncher, "onCreate", Bundle.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {

                    isFolderShowing = false;
                    isShowEditPanel = false;

                    Activity activity = (Activity) param.thisObject;

                    findAndHookMethod(mLauncher, "isFolderShowing", new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) throws Throwable {
                            isFolderShowing = (boolean) param.getResult();
                        }
                    });

                    findAndHookMethod(mLauncher, "showEditPanel", boolean.class, new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) throws Throwable {
                            isShowEditPanel = (boolean) param.args[0];
                        }
                    });

                    findAndHookMethod(mLauncher, "openFolder", mFolderInfo, View.class, new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) throws Throwable {
                            XposedHelpers.callStaticMethod(mBlurUtils, "fastBlur", 1f, activity.getWindow(), true);
                        }
                    });

                    findAndHookMethod(mLauncher, "closeFolder", boolean.class, new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) throws Throwable {
                            if (isShowEditPanel) {
                                XposedHelpers.callStaticMethod(mBlurUtils, "fastBlur", 1f, activity.getWindow(), true, 0L);
                            } else {
                                XposedHelpers.callStaticMethod(mBlurUtils, "fastBlur", 0f, activity.getWindow(), true, 300L);
                            }
                        }
                    });

                    findAndHookMethod(mLauncher, "cancelShortcutMenu", int.class, mCancelShortcutMenuReason, new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) throws Throwable {
                            if (isFolderShowing) XposedHelpers.callStaticMethod(mBlurUtils, "fastBlur", 1f, activity.getWindow(), true, 0L);
                        }
                    });

                    findAndHookMethod(mBlurUtils, "fastBlurWhenStartOpenOrCloseApp", boolean.class, mLauncher, new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) throws Throwable {
                            if (isFolderShowing) {
                                param.setResult(XposedHelpers.callStaticMethod(mBlurUtils, "fastBlur", 1.0f, activity.getWindow(), true, 0L));
                            } else if (isShowEditPanel) {
                                param.setResult(XposedHelpers.callStaticMethod(mBlurUtils, "fastBlur", 1.0f, activity.getWindow(), true, 0L));
                            }
                        }
                    });

                    findAndHookMethod(mBlurUtils, "fastBlurWhenFinishOpenOrCloseApp", mLauncher, new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) throws Throwable {
                            if (isFolderShowing) {
                                param.setResult(XposedHelpers.callStaticMethod(mBlurUtils, "fastBlur", 1.0f, activity.getWindow(), true, 0L));
                            } else if (isShowEditPanel) {
                                param.setResult(XposedHelpers.callStaticMethod(mBlurUtils, "fastBlur", 1.0f, activity.getWindow(), true, 0L));
                            }
                        }
                    });


                    findAndHookMethod(mBlurUtils, "fastBlurWhenExitRecents", mLauncher, mLauncherState, boolean.class, new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) throws Throwable {
                            if (isFolderShowing) {
                                param.setResult(XposedHelpers.callStaticMethod(mBlurUtils, "fastBlur", 1.0f, activity.getWindow(), true, 0L));
                            } else if (isShowEditPanel) {
                                param.setResult(XposedHelpers.callStaticMethod(mBlurUtils, "fastBlur", 1.0f, activity.getWindow(), true, 0L));
                            }
                        }
                    });

                    findAndHookMethod(mBlurUtils, "onGesturePerformAppToHome", new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) throws Throwable {
                            if (isFolderShowing) {
                                param.setResult(XposedHelpers.callStaticMethod(mBlurUtils, "fastBlur", 1.0f, activity.getWindow(), true, 300L));
                            }
                        }
                    });
                }
            });
        }
    }
}
