package com.sevtinge.cemiuiler.module.home.folder;

import android.view.View;

import com.sevtinge.cemiuiler.XposedInit;
import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class FolderAutoClose extends BaseHook {

    @Override
    public void init() {
        findAndHookMethod("com.miui.home.launcher.Launcher", "launch", "com.miui.home.launcher.ShortcutInfo", View.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                if (!mPrefsMap.getBoolean("home_folder_auto_close")) return;
                boolean mHasLaunchedAppFromFolder = XposedHelpers.getBooleanField(param.thisObject, "mHasLaunchedAppFromFolder");
                if (mHasLaunchedAppFromFolder) XposedHelpers.callMethod(param.thisObject, "closeFolder");
            }
        });

        findAndHookMethodSilently("com.miui.home.launcher.common.CloseFolderStateMachine", "onPause", new MethodHook() {
            @Override
            protected void before(final MethodHookParam param) throws Throwable {
                if (XposedInit.mPrefsMap.getBoolean("home_folder_auto_close")) param.setResult(null);
            }
        });
    }
}
