package com.sevtinge.cemiuiler.module.screenshot;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XC_MethodReplacement;

public class UnlockMinimumCropLimit extends BaseHook {

    Class<?> mScreenCropView;

    @Override
    public void init() {
        mScreenCropView = findClassIfExists("com.miui.gallery.editor.photo.screen.crop.ScreenCropView$h");
        returnIntConstant(mScreenCropView, "a");
    }

    private void returnIntConstant(Class<?> cls, String methodName) {
        findAndHookMethod(cls, methodName, XC_MethodReplacement.returnConstant(0));
    }
}


