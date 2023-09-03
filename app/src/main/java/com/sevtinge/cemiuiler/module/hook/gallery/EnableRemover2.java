package com.sevtinge.cemiuiler.module.hook.gallery;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;

public class EnableRemover2 extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.miui.gallery.editor.photo.app.remover2.sdk.Remover2CheckHelper", "isRemover2Support", new BaseHook.MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
