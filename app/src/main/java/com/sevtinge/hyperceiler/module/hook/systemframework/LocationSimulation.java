package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;


public class LocationSimulation extends BaseHook {

    Class<?> mTelephonyManager;

    @Override
    public void init() {
        mTelephonyManager = findClassIfExists("android.telephony.TelephonyManager");

        if (mTelephonyManager != null) {

            findAndHookMethod(mTelephonyManager, "getNetworkOperatorName", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    logI(TAG, LocationSimulation.this.lpparam.packageName, "getNetworkOperatorName：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getSimOperatorName", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    logI(TAG, LocationSimulation.this.lpparam.packageName, "getSimOperatorName：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getSimOperator", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    logI(TAG, LocationSimulation.this.lpparam.packageName, "getSimOperator：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getNetworkOperator", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    logI(TAG, LocationSimulation.this.lpparam.packageName, "getNetworkOperator：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getSimCountryIso", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    logI(TAG, LocationSimulation.this.lpparam.packageName, "getSimCountryIso：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getNetworkCountryIso", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    logI(TAG, LocationSimulation.this.lpparam.packageName, "getNetworkCountryIso：" + param.getResult());
                }
            });

            findAndHookMethod(mTelephonyManager, "getNeighboringCellInfo", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    logI(TAG, LocationSimulation.this.lpparam.packageName, "getNeighboringCellInfo：" + param.getResult());
                }
            });

        }
    }
}
