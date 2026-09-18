package com.sevtinge.hyperceiler.tests.dock;

import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockGlassPreset;

public class DockGlassPresetTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(DockGlassPreset.MODE == 3, "do not reuse legacy custom blur value 2");
        check(DockGlassPreset.MATERIAL_TYPE == 1, "verified native glass material type");
        check(DockGlassPreset.SMALL_BLUR_RADIUS == 36 && DockGlassPreset.BIG_BLUR_RADIUS == 500,
                "folder icon glass dual blur radii");
        check(DockGlassPreset.GLASS_ENHANCE_FLAG == 0x2000, "native glass enhancement flag");

        // false = Medium_Thin_Low (not a light wallpaper), true = Medium_Thin_High.
        float[] low = DockGlassPreset.parameters(false);
        float[] high = DockGlassPreset.parameters(true);
        for (float[] params : new float[][]{low, high}) {
            check(params.length == 42, "JNI requires exactly 42 floats");
            for (float value : params) check(Float.isFinite(value), "all parameters finite");
        }

        check(java.util.Arrays.hashCode(low) == -616200191,
                "bit-exact Medium_Thin_Low folder token, extracted from the launcher GlassToken");
        check(java.util.Arrays.hashCode(high) == -1650047496,
                "bit-exact Medium_Thin_High folder token, extracted from the launcher GlassToken");
        check(!java.util.Arrays.equals(low, high),
                "the wallpaper branch must actually change the material");

        // Fields shared by both folder variants.
        for (float[] params : new float[][]{low, high}) {
            check(params[18] == 1f, "alpha is index 18, not the edge field");
            check(params[19] == 36f, "shape edge parameter");
            check(params[21] == 100f && params[22] == 200f, "shape point parameters");
            check(params[14] == .1f, "colour mixing");
            check(params[9] == .8f && params[25] == -.4f && params[31] == 1.15f,
                    "shared low-light bounds and lighting");
        }
        check(low[32] == 2f && high[32] == 3f, "refractive index differs with the wallpaper");
        check(low[33] == 1f && high[33] == 0f, "background field differs with the wallpaper");
        check(high[0] == .5f && high[1] == 1.6f && high[3] == .3f, "high brightness curve");
        check(low[0] == .5f && low[1] == 1f && low[3] == 1f, "low brightness curve");

        for (boolean dark : new boolean[]{false, true}) {
            check((DockGlassPreset.fallbackColor(dark) >>> 24) == 0x18, "glass fallback has a light tint");
            float[] params = DockGlassPreset.parameters(dark);
            params[18] = 0f;
            check(DockGlassPreset.parameters(dark)[18] == 1f, "presets are not shared mutable arrays");
        }
        System.out.println("DockGlassPreset tests passed");
    }
}
