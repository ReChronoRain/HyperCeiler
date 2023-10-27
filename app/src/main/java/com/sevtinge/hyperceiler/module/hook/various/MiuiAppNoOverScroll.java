package com.sevtinge.hyperceiler.module.hook.various;

import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import de.robv.android.xposed.XposedHelpers;

public class MiuiAppNoOverScroll extends BaseHook {


    @Override
    public void init() {

        Class<?> mSpringBackCls = findClassIfExists("miuix.springback.view.SpringBackLayout");
        Class<?> mRemixRvCls = findClassIfExists("androidx.recyclerview.widget.RemixRecyclerView");

        try {
            MethodHook hookParam = new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    XposedHelpers.setBooleanField(param.thisObject, "mSpringBackEnable", false);
                    param.args[0] = false;
                }
            };

            if (mSpringBackCls != null) {

                hookAllConstructors(mSpringBackCls, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        XposedHelpers.setBooleanField(param.thisObject, "mSpringBackEnable", false);
                    }
                });

                findAndHookMethodSilently(mSpringBackCls, "setSpringBackEnable", boolean.class, hookParam);
            }


            if (mRemixRvCls != null) {
                hookAllConstructors(mRemixRvCls, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        ((View) param.thisObject).setOverScrollMode(View.OVER_SCROLL_NEVER);
                        XposedHelpers.setBooleanField(param.thisObject, "mSpringBackEnable", false);
                    }
                });
                findAndHookMethodSilently(mRemixRvCls, "setSpringEnabled", boolean.class, hookParam);
            }
        } catch (Exception e) {
            XposedLogUtils.logE(TAG,"TAG" + lpparam.packageName, e);
        }
    }
}
