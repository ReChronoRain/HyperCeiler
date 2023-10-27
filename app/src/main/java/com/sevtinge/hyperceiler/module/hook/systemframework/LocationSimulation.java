package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

public class LocationSimulation extends BaseHook {

    Class<?> mTelephonyManager;

    @Override
    public void init() {
        mTelephonyManager = findClassIfExists("android.telephony.TelephonyManager");

        if (mTelephonyManager != null) {

            findAndHookMethod(mTelephonyManager, "getNetworkOperatorName", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    XposedLogUtils.logI(TAG, "getNetworkOperatorName：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getSimOperatorName", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    XposedLogUtils.logI(TAG, "getSimOperatorName：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getSimOperator", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    XposedLogUtils.logI(TAG, "getSimOperator：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getNetworkOperator", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    XposedLogUtils.logI(TAG, "getNetworkOperator：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getSimCountryIso", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    XposedLogUtils.logI(TAG, "getSimCountryIso：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getNetworkCountryIso", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    XposedLogUtils.logI(TAG, "getNetworkCountryIso：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getNeighboringCellInfo", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    XposedLogUtils.logI(TAG, "getNeighboringCellInfo：" + param.getResult());
                }
            });

        }
    }
}
