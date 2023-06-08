package com.sevtinge.cemiuiler.module.updater;

import android.os.Build;
import android.text.TextUtils;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class VersionCodeModify extends BaseHook {

    Class<?> mApplication;

    @Override
    public void init() {

        mApplication = findClassIfExists("com.android.updater.Application");

        findAndHookMethod(mApplication, "onCreate", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                String mVersionCode = mPrefsMap.getString("various_updater_miui_version", "V14.0.22.11.26.DEV");
                if (!TextUtils.isEmpty(mVersionCode)) {
                    XposedHelpers.setStaticObjectField(Build.VERSION.class, "INCREMENTAL", mVersionCode);
                }
            }
        });
    }
}
