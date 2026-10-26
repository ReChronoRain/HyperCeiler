package com.sevtinge.hyperceiler.libhook.rules.systemui.navigation;

/** Original resource widths are the baseline; repeated notifications never compound. */
public final class BackGestureWidthState {
    public int left, right, appliedLeft, appliedRight;
    public BackGestureWidthState(int left, int right) { reset(left, right); }
    public void reset(int l, int r) { left = l; right = r; appliedLeft = l; appliedRight = r; }
    public static int normalize(int percent) { return percent >= 100 && percent <= 400 ? percent : 100; }
    public static int scale(int original, int percent) {
        if (original <= 0 || original > 16384) return original;
        return (int) Math.min(32768L, (long) original * normalize(percent) / 100);
    }
    public void apply(int percent) { appliedLeft = scale(left, percent); appliedRight = scale(right, percent); }
}
