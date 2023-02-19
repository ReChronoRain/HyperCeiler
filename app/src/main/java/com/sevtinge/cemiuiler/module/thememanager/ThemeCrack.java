package com.sevtinge.cemiuiler.module.thememanager;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;
import com.sevtinge.cemiuiler.utils.LogUtils;

import de.robv.android.xposed.XposedHelpers;
import miui.drm.DrmManager;

public class ThemeCrack extends BaseHook {

    Class<?> mResourceCls;
    Class<?> mOnlineResourceCls;
    Class<?> mResourceCls2;

    @Override
    public void init() {

        mResourceCls = findClassIfExists("com.android.thememanager.basemodule.resource.model.Resource");
        mOnlineResourceCls = findClassIfExists("com.android.thememanager.detail.theme.model.OnlineResourceDetail");
        mResourceCls2 = findClassIfExists("com.android.thememanager.basemodule.resource.model.ResourceOnlineProperties");


        try {
            findAndHookMethod(mResourceCls, "isAuthorizedResource", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });

            findAndHookMethod(mResourceCls, "isProductBought", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });

            findAndHookMethod(mResourceCls, "setProductBought", boolean.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.args[0] = true;
                }
            });

            findAndHookMethod(mResourceCls, "setProductPrice", int.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.args[0] = 0;
                }
            });

            findAndHookMethod(mResourceCls, "getProductPrice", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(0);
                }
            });


            findAndHookMethod(mResourceCls2, "setProductPrice", int.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.args[0] = 0;
                }
            });

            findAndHookMethod(mResourceCls2, "getProductPrice", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(0);
                }
            });

            findAndHookMethod(mResourceCls2, "isProductBought", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });

            findAndHookMethod(mResourceCls2, "setProductBought", boolean.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.args[0] = true;
                }
            });

            findAndHookMethod(mOnlineResourceCls, "toResource", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    XposedHelpers.setObjectField(param.thisObject, "productPrice", 0);
                    XposedHelpers.setObjectField(param.thisObject, "bought", true);
                }
            });
        } catch (Exception e) {
            LogUtils.logXp(TAG, e);
        }
    }


    public static void initRes() {
        Helpers.hookAllMethods(DrmManager.class, "isLegal", new Helpers.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(DrmManager.DrmResult.DRM_SUCCESS);
            }
        });

        Helpers.hookAllMethods(DrmManager.class, "isPermanentRights", new Helpers.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });

        Helpers.hookAllMethods(DrmManager.class, "isRightsFileLegal", new Helpers.MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
    }
}
