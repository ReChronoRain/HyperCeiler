package com.sevtinge.cemiuiler.module.mediaeditor;

import static com.sevtinge.cemiuiler.utils.Helpers.getPackageVersionName;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class UnlockMinimumCropLimit extends BaseHook {

    Class<?> mScreenCropView;
    Class<?> mCrop;

    @Override
    public void init() {
        /*mScreenCropView = findClassIfExists("com.miui.gallery.editor.photo.screen.crop.ScreenCropView$h");
        returnIntConstant(mScreenCropView, "e");
        mCrop = findClassIfExists("com.miui.gallery.editor.photo.core.imports.obsoletes.Crop$r");
        returnIntConstant(mCrop, "a");
        XposedBridge.log("Cemiuiler: com.miui.mediaeditor Ver 1.0.x UnlockMinimumCropLimit hook success!");*/
        String appVersionName = getPackageVersionName(lpparam);
        log("com.miui.mediaeditor app Ver is " + appVersionName);
        try {
            mScreenCropView = findClassIfExists("com.miui.gallery.editor.photo.screen.crop.ScreenCropView$h");
            returnIntConstant(mScreenCropView, "e");
            mCrop = findClassIfExists("com.miui.gallery.editor.photo.core.imports.obsoletes.Crop$r");
            returnIntConstant(mCrop, "a");
            log("hook com.miui.mediaeditor Use abc 1");
        } catch (Exception e) {
            try {
                // mScreenCropView = findClassIfExists("com.miui.gallery.editor.photo.screen.crop.ScreenCropView$h");
                // returnIntConstant(mScreenCropView, "e");
                mCrop = findClassIfExists("com.miui.gallery.editor.photo.core.imports.obsoletes.Crop$o");
                returnIntConstant(mCrop, "a");
                log("hook com.miui.mediaeditor Use abc 2");
            } catch (Exception f) {
                mScreenCropView = findClassIfExists("com.miui.gallery.editor.photo.screen.crop.ScreenCropView$OooOOO0");
                returnIntConstant(mScreenCropView, "OooO0o0");
                mCrop = findClassIfExists("com.miui.gallery.editor.photo.core.imports.obsoletes.Crop$o00Oo0");
                returnIntConstant(mCrop, "OooO00o");
                log("hook com.miui.mediaeditor Use oO0");
            }
        }

    }

    private void returnIntConstant(Class<?> cls, String methodName) {
        findAndHookMethod(cls, methodName, XC_MethodReplacement.returnConstant(0));
    }
}




