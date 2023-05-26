package com.sevtinge.cemiuiler.module.systemframework;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;

public class DisablePinVerifyPer72h extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.server.locksettings.LockSettingsStrongAuth", "rescheduleStrongAuthTimeoutAlarm", long.class, int.class, XC_MethodReplacement.DO_NOTHING);
    }
}
