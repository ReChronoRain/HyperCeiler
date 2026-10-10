package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3;
// Probe's exact JNI declaration only; production Kotlin owns mode/thread/load gating.
public final class AmbientNativeColorOS4 {
    static {System.loadLibrary("HyperCeilerAmbientOS4");}
    public static native long nativeResolve(int[] pixels,int width,int height);
}
