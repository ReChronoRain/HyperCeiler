package com.sevtinge.hyperceiler.module.hook.various;

import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.BlurUtils;

import de.robv.android.xposed.XposedHelpers;

public class DialogBlur extends BaseHook {

    Class<?> mDialogCls = findClassIfExists("miuix.appcompat.app.AlertController");

    @Override
    public void init() {
        hookAllMethods(mDialogCls, "installContent", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {

                View mParentPanel = (View) XposedHelpers.getObjectField(param.thisObject, "mParentPanel");

                if (mParentPanel != null) {
                    /*new BlurUtils(mParentPanel);*/
                    new BlurUtils(mParentPanel, "default");
                }
            }
        });

        hookAllMethods(mDialogCls, "dismiss", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                super.after(param);
                View mParentPanel = (View) XposedHelpers.getObjectField(param.thisObject, "mParentPanel");
                mParentPanel.setVisibility(View.INVISIBLE);
            }
        });
    }
}
