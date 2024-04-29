package com.sevtinge.hyperceiler.utils.devicesdk;

import com.sevtinge.hyperceiler.utils.InvokeUtils;

public class TelephonyManager {
    Object telephonyManager;
    String name = "miui.telephony.TelephonyManager";

    public TelephonyManager() {
        telephonyManager = InvokeUtils.invokeStaticMethod(name, "getDefault", new Class[]{});
    }

    public static TelephonyManager getDefault() {
        return new TelephonyManager();
    }

    public void setUserFiveGEnabled(boolean enabled) {
        InvokeUtils.invokeMethod(name, telephonyManager, "setUserFiveGEnabled", new Class[]{boolean.class}, enabled);
    }

    public boolean isUserFiveGEnabled() {
        return InvokeUtils.invokeMethod(name, telephonyManager, "isUserFiveGEnabled", new Class[]{});
    }

    public boolean isFiveGCapable() {
        return InvokeUtils.invokeMethod(name, telephonyManager, "isFiveGCapable", new Class[]{});
    }
}
