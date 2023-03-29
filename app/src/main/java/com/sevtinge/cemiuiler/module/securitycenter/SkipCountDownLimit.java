package com.sevtinge.cemiuiler.module.securitycenter;

import android.os.Handler;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

public class SkipCountDownLimit extends BaseHook {

    Class<?> mHandlerClass = null;
    Class<?> mInterceptBaseFragmentCls;
    Class<?>[] mInnerClasses;

    @Override
    public void init() {

        mInterceptBaseFragmentCls = findClassIfExists("com.miui.permcenter.privacymanager.InterceptBaseFragment");
        mInnerClasses = mInterceptBaseFragmentCls.getDeclaredClasses();


        findAndHookMethod("android.widget.TextView","setEnabled", boolean.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.args[0] = true;
            }
        });

        for (Class<?> mInnerClass : mInnerClasses) {
            if (Handler.class.isAssignableFrom(mInnerClass)) {
                mHandlerClass = mInnerClass;
                break;
            }
        }

        if (mHandlerClass != null) {
            hookAllConstructors(mHandlerClass, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    if (param.args.length == 2) {
                        param.args[1] = 0;
                    }
                }
            });

            Method[] methods = XposedHelpers.findMethodsByExactParameters(mHandlerClass, void.class, int.class);
            if (methods.length > 0) {
                hookMethod(methods[0], new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.args[0] = 0;
                    }
                });
            }
        }


        /*findAndHookMethod("android.widget.TextView", "setText", CharSequence.class, TextView.BufferType.class, boolean.class, int.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object[] mParam = param.args;
                String mText = param.args[0].toString();
                if (mParam.length != 0 && mText.startsWith("确定(")) {
                    param.args[0] = "确定";
                }
            }
        });*/
    }
}
