package com.sevtinge.hyperceiler.libhook.rules.home.dock;

public final class XiaoheiziEntryGateTest {
    private static final String COMMAND = DockRecentsMotion.WALLPAPER_ACTION;

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        var entry = new XiaoheiziEntryGate();
        // On-device app → recents: home reset first, then the actual overview animation.
        check(entry.onCommand(COMMAND, "setTo", 1.0499999523, 100) == XiaoheiziEntryGate.Decision.IGNORE,
            "recent task prelude cannot arm");
        check(!entry.ready(230), "even a slow overview transition cannot expose a pending performance");
        check(entry.onCommand(COMMAND, "startAnim", 1.1129999161, 260) == XiaoheiziEntryGate.Decision.CANCEL,
            "overview cancels");
        check(!entry.ready(400), "no performance in overview");

        // Recents dismissal can issue the home target 27 ms before opening the selected app.
        check(entry.onCommand(COMMAND, "startAnim", 1.0499999523, 500) == XiaoheiziEntryGate.Decision.ARM,
            "home target starts confirmation");
        check(!entry.ready(527), "the app switch has a chance to cancel");
        check(entry.onCommand(COMMAND, "startAnim", 1.1969999075, 527) == XiaoheiziEntryGate.Decision.CANCEL,
            "returning to an app cancels");
        check(!entry.ready(660), "app selection must not play");

        check(entry.onCommand(COMMAND, "startAnim", 1.0499999523, 1_000) == XiaoheiziEntryGate.Decision.ARM,
            "real home return arms");
        check(!entry.ready(1_109) && entry.ready(1_110), "settled home return plays");
        entry.reset();
        check(!entry.ready(1_111), "consuming a request prevents traversal replay");
        check(entry.onCommand(COMMAND, "startAnim", 1.0, 2_000) == XiaoheiziEntryGate.Decision.ARM,
            "unscaled home preset retained");
        check(!entry.ready(3_500), "old requests cannot play on a later layout");
        check(entry.onCommand("other", "startAnim", 1.0, 3_600) == XiaoheiziEntryGate.Decision.IGNORE
            && !entry.ready(3_800), "unrelated wallpaper commands cannot play");
        System.out.println("Xiaoheizi entry gate PASS: recents prelude, app selection, true home, consumption and expiry");
    }
}
