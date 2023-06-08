package com.sevtinge.cemiuiler.module.thememanager;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import miui.drm.DrmManager;

public class DisableThemeAd extends BaseHook {

    Class<?> mAdInfoCls;
    Class<?> mAdInfoResponseCls;

    @Override
    public void init() {

        mAdInfoCls = findClassIfExists("com.android.thememanager.basemodule.ad.model.AdInfo");
        mAdInfoResponseCls = findClassIfExists("com.android.thememanager.basemodule.ad.model.AdInfoResponse");

        findAndHookMethod(mAdInfoResponseCls, "isAdValid", mAdInfoCls, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });

        findAndHookMethod("com.android.thememanager.recommend.view.listview.viewholder.PureAdBannerViewHolder", "isAdValid", mAdInfoCls, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }

    public void initZygote() {

        hookAllMethods(DrmManager.class, "isSupportAd", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });

        hookAllMethods(DrmManager.class, "setSupportAd", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.args[1] = false;
            }
        });
    }
}
