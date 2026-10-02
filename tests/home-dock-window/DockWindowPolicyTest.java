package com.sevtinge.hyperceiler.tests.dock;

import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockWindowPolicy;

public class DockWindowPolicyTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(DockWindowPolicy.normalizeBackgroundMode(0) == 1, "migrate removed solid mode");
        check(DockWindowPolicy.normalizeBackgroundMode(1) == 1, "preserve system material ID");
        check(DockWindowPolicy.normalizeBackgroundMode(2) == 1, "migrate unsupported legacy custom mode");
        check(DockWindowPolicy.normalizeBackgroundMode(3) == 3, "preserve glass ID");
        check(DockWindowPolicy.normalizeBackgroundMode(-1) == 1, "unknown mode defaults to material");
        check(DockWindowPolicy.normalizeBackgroundMode(4) == 1, "unknown positive mode defaults to material");
        String title = "com.miui.home/com.miui.home.launcher.Launcher";
        check(DockWindowPolicy.isLauncherWindow("com.miui.home", title, 1, 0), "launcher");
        check(DockWindowPolicy.isLauncherWindow("com.miui.home", "com.miui.home/.launcher.Launcher", 2, 0), "short component");
        check(!DockWindowPolicy.isLauncherWindow("other", title, 1, 0), "owner check");
        check(!DockWindowPolicy.isLauncherWindow("com.miui.home", title, 3, 0), "starting window excluded");
        check(!DockWindowPolicy.isLauncherWindow("com.miui.home", title, 1, 1), "external display excluded");
        check(!DockWindowPolicy.isLauncherWindow("com.miui.home", "com.miui.home/.settings.MiuiHomeSettingActivity", 1, 0), "settings excluded");
        check(!DockWindowPolicy.isLauncherWindow("com.miui.home", "LauncherPreview", 1, 0), "no broad title match");
        var bounds = DockWindowPolicy.layout(1080, 2400, 3, 150, 25, 15, 30);
        check(bounds.x() == 75 && bounds.y() == 1905 && bounds.width() == 930 && bounds.height() == 450 && bounds.radius() == 90, "dp conversion");
        check(DockWindowPolicy.layout(0, 2400, 3, 150, 25, 15, 30) == null, "zero frame");
        check(DockWindowPolicy.layout(1080, 2400, Float.NaN, 150, 25, 15, 30) == null, "invalid density");
        check(DockWindowPolicy.layout(100, 100, 3, 150, 25, 15, 30) == null, "no negative crop");
        check(DockWindowPolicy.layout(1080, 2400, 3, 0, 25, 15, 30) == null, "zero height");
        for (int width : new int[]{1, 80, 1080, 2400}) {
            for (int height : new int[]{1, 80, 1080, 2400}) {
                var b = DockWindowPolicy.layout(width, height, 3, 300, -20, -30, 100);
                if (b == null) continue;
                check(b.x() >= 0 && b.y() >= 0 && b.x() + b.width() <= width && b.y() + b.height() <= height, "bounded crop");
                check(b.radius() <= Math.min(b.width(), b.height()) / 2f, "bounded radius");
            }
        }
        System.out.println("DockWindowPolicy tests passed");
    }
}
