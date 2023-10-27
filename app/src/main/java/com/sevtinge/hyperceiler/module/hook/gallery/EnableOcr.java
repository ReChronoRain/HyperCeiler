package com.sevtinge.hyperceiler.module.hook.gallery;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;

public class EnableOcr extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.miui.gallery.ui.photoPage.ocr.OCRHelper", "isSupportLocalOCR", new BaseHook.MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.miui.gallery.ui.photoPage.ocr.OCRHelper", "isSupportOCR", new BaseHook.MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}



