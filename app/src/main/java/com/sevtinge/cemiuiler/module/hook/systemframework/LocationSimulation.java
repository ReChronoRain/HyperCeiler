package com.sevtinge.cemiuiler.module.hook.systemframework;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.LogUtils;

public class LocationSimulation extends BaseHook {

    Class<?> mTelephonyManager;

    @Override
    public void init() {
        mTelephonyManager = findClassIfExists("android.telephony.TelephonyManager");

        if (mTelephonyManager != null) {

            findAndHookMethod(mTelephonyManager, "getNetworkOperatorName", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    LogUtils.logXp(TAG, "getNetworkOperatorName：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getSimOperatorName", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    LogUtils.logXp(TAG, "getSimOperatorName：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getSimOperator", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    LogUtils.logXp(TAG, "getSimOperator：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getNetworkOperator", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    LogUtils.logXp(TAG, "getNetworkOperator：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getSimCountryIso", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    LogUtils.logXp(TAG, "getSimCountryIso：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getNetworkCountryIso", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    LogUtils.logXp(TAG, "getNetworkCountryIso：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getNeighboringCellInfo", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    LogUtils.logXp(TAG, "getNeighboringCellInfo：" + param.getResult());
                }
            });

        }
    }
}
