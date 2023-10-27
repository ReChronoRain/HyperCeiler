package com.sevtinge.hyperceiler.module.hook.gallery;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;

public class EnablePhotoMovie extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.miui.mediaeditor.api.MediaEditorApiHelper", "isPhotoMovieAvailable", new BaseHook.MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.miui.gallery.domain.DeviceFeature", "isDeviceSupportPhotoMovie", new BaseHook.MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}

