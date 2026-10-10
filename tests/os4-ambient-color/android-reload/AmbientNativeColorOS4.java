package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3;

/** Only the ART loader test entry: the production JNI bridge receives invalid input. */
public final class AmbientNativeColorOS4 {
    public static long loadAndCall(String path) {
        System.load(path);
        return nativeResolve(new int[0], 0, 0);
    }
    private static native long nativeResolve(int[] pixels, int width, int height);
}
