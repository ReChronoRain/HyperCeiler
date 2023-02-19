package com.sevtinge.cemiuiler.module.settings;

import android.content.Context;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class BluetoothRestrict extends BaseHook {

    Class<?> mLocalBluetoothAdapter;

    @Override
    public void init() {
        mLocalBluetoothAdapter = findClassIfExists("com.android.settingslib.bluetooth.LocalBluetoothAdapter");

        findAndHookMethod(mLocalBluetoothAdapter,
                "isSupportBluetoothRestrict",
                Context.class,
                XC_MethodReplacement.returnConstant(false));
    }
}
