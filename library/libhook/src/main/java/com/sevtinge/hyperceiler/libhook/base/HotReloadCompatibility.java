package com.sevtinge.hyperceiler.libhook.base;

/** In-process resource migration compatibility, independent of launcher APK versions. */
final class HotReloadCompatibility {
    private HotReloadCompatibility() {}

    /**
     * OS4 on Android 17 reproduced resource-table native crashes after APK hot replacement.
     * This is a preflight veto, not a feature/configuration gate: normal target startup and
     * live preference delivery are unchanged. Limit it to the reproduced platform; retain
     * the existing reload transaction on other OS/SDK combinations.
     */
    static boolean requiresProcessRestart(int sdk, boolean hyperOs4) {
        return sdk == 37 && hyperOs4;
    }
}
