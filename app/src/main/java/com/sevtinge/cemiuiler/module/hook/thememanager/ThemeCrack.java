package com.sevtinge.cemiuiler.module.hook.thememanager;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class ThemeCrack extends BaseHook {

    @Override
    public void init() {
        try {
            hookAllMethods("com.android.thememanager.basemodule.resource.model.Resource", "isAuthorizedResource", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });
        } catch (Exception e) {
            logI("Hook method com.android.thememanager.basemodule.resource.model.Resource.isAuthorizedResource failed. " + e);
        }
        try {
            hookAllMethods("com.android.thememanager.basemodule.resource.model.Resource", "isProductBought", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });
        } catch (Exception e) {
            logI("Hook method com.android.thememanager.basemodule.resource.model.Resource.isProductBought failed. " + e);
        }
        try {
            hookAllMethods("com.android.thememanager.detail.theme.model.OnlineResourceDetail", "toResource", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    XposedHelpers.setBooleanField(param.thisObject, "bought", true);
                }
            });
        } catch (Exception e) {
            logI("Hook method com.android.thememanager.detail.theme.model.OnlineResourceDetail.toResource failed. " + e);
        }
        try {
            hookAllMethods("com.miui.maml.widget.edit.MamlutilKt", "themeManagerSupportPaidWidget", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });
        } catch (Exception e) {
            logI("Hook method com.miui.maml.widget.edit.MamlutilKt.themeManagerSupportPaidWidget failed. " + e);
        }
    }

}
