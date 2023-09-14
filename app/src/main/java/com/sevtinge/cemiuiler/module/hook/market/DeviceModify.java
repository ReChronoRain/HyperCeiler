package com.sevtinge.cemiuiler.module.hook.market;

import android.os.Build;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class DeviceModify extends BaseHook {

    String mDevice;
    String mModel;
    String mManufacturer;

    @Override
    public void init() {
        if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 101) {
            // 13u
            mDevice = "ishtar";
            mModel = "2304FPN6DC";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 153) {
            // civi3
            mDevice = "yuechu";
            mModel = "23046PNC9C";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 223) {
            // f3
            mDevice = "babylon";
            mModel = "2308CPXD0C";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 190) {
            // alpha
            mDevice = "avenger";
            mModel = "MIX Alpha";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 191) {
            // alpha
            mDevice = "draco";
            mModel = "MIX Alpha";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 208) {
            // pad6m
            mDevice = "yudi";
            mModel = "2307BRPDCC";
            mManufacturer = "Xiaomi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 307) {
            // k60u
            mDevice = "corot";
            mModel = "23078RKD5C";
            mManufacturer = "Redmi";
        } else if (mPrefsMap.getStringAsInt("market_device_modify_new", 0) == 332) {
            // n12t
            mDevice = "marble";
            mModel = "23049RAD8C";
            mManufacturer = "Redmi";
        }
        findAndHookConstructor("com.xiaomi.market.MarketApp", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                XposedHelpers.setStaticObjectField(Build.class, "DEVICE", mDevice);
                XposedHelpers.setStaticObjectField(Build.class, "MODEL", mModel);
                XposedHelpers.setStaticObjectField(Build.class, "MANUFACTURER", mManufacturer);
            }
        });
    }
}
