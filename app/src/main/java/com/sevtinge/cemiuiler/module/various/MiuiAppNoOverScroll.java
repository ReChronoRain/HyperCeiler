package com.sevtinge.cemiuiler.module.various;

import android.view.View;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.LogUtils;

import de.robv.android.xposed.XposedHelpers;

public class MiuiAppNoOverScroll extends BaseHook {


    @Override
    public void init() {

        Class<?> mSpringBackCls = findClassIfExists("miuix.springback.view.SpringBackLayout");
        Class<?> mRemixRvCls = findClassIfExists("androidx.recyclerview.widget.RemixRecyclerView");

        try {
            MethodHook hookParam = new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    XposedHelpers.setBooleanField(param.thisObject, "mSpringBackEnable", false);
                    param.args[0] = false;
                }
            };

            if (mSpringBackCls != null) {

                hookAllConstructors(mSpringBackCls, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        XposedHelpers.setBooleanField(param.thisObject, "mSpringBackEnable", false);
                    }
                });

                findAndHookMethodSilently(mSpringBackCls, "setSpringBackEnable", boolean.class, hookParam);
            }


            if (mRemixRvCls != null) {
                hookAllConstructors(mRemixRvCls, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        ((View)param.thisObject).setOverScrollMode(View.OVER_SCROLL_NEVER);
                        XposedHelpers.setBooleanField(param.thisObject, "mSpringBackEnable", false);
                    }
                });
                findAndHookMethodSilently(mRemixRvCls, "setSpringEnabled", boolean.class, hookParam);
            }
        } catch (Exception e) {
            LogUtils.logXp("TAG", lpparam.packageName, e);
        }
    }
}
