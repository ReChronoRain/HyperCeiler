package com.sevtinge.cemiuiler.module.systemui;

import android.util.ArraySet;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.LogUtils;

import java.util.Set;

import de.robv.android.xposed.XposedHelpers;

public class ChargeAnimationStyle extends BaseHook {

    Class<?> mChargeAnimCls;
    Class<?> mWaveViewCls;

    int mChargeAnimationType;
    int mType;

    @Override
    public void init() {

        mChargeAnimCls = findClassIfExists("com.android.keyguard.charge.ChargeUtils");
        mWaveViewCls = findClassIfExists("com.android.keyguard.charge.wave.WaveView");

        mChargeAnimationType = mPrefsMap.getStringAsInt("system_ui_charge_animation_style", 0);

        setChargeAnimationType(mChargeAnimationType);
    }

    public void setChargeAnimationType(int value) {

        if (value == 1) {
            findAndHookMethod(mChargeAnimCls, "isChargeAnimationDisabled", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } else if (value == 4) {

            findAndHookMethod(mChargeAnimCls, "supportWaveChargeAnimation", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    StackTraceElement[] stackElement = new Throwable().getStackTrace();
                    boolean mResult = false;
                    Set<String> classTrue = new ArraySet<>(new String[]{"com.android.keyguard.charge.ChargeUtils",
                        "com.android.keyguard.charge.container.MiuiChargeContainerView"});
                    int i = 0;
                    int length = stackElement.length;

                    while (true) {
                        if (i >= length) {
                            break;
                        } else if (!classTrue.contains(stackElement[i].getClassName())) {
                            i++;
                        } else {
                            mResult = true;
                            LogUtils.logXp(TAG, stackElement[i].getClassName());
                            break;
                        }
                    }
                    param.setResult(mResult);
                }
            });

            findAndHookMethod(mWaveViewCls, "updateWaveHeight", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    XposedHelpers.setIntField(param.thisObject, "mWaveXOffset", 0);
                }
            });


                /*findAndHookMethod(mChargeAnimCls,"getChargeAnimationType", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(2);
                    }
                });

                findAndHookMethod(mWaveViewCls, "updateWaveHeight" ,new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        XposedHelpers.setIntField(param.thisObject, "mWaveXOffset", 0);
                    }
                });*/
        } else {
            switch (value) {
                case 2 -> mType = 0;
                case 3 -> mType = 1;
            }

            findAndHookMethod(mChargeAnimCls, "getChargeAnimationType", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(mType);
                }
            });
        }
    }
}
