package com.sevtinge.hyperceiler.tests.dock;

import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockRecentsMotion;

public class DockRecentsMotionTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void close(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < 0.0001f, message + ": " + actual);
    }

    public static void main(String[] args) {
        String command = DockRecentsMotion.WALLPAPER_ACTION;
        check(DockRecentsMotion.frameTimeMillis(1_008_999_999L, 1000) == 1008, "use VSync time");
        check(DockRecentsMotion.frameTimeMillis(1_008_999_999L, 1010) == 1010, "late scene callback cannot rewind");
        check(DockRecentsMotion.frameTimeMillis(-1, -1) == 0, "invalid frame time bounded");
        check(DockRecentsMotion.ENTER_RESPONSE == .3 && DockRecentsMotion.ENTER_DAMPING == .9, "observed enter preset");
        check(DockRecentsMotion.EXIT_RESPONSE == .45 && DockRecentsMotion.EXIT_DAMPING == .95, "observed exit preset");
        check(Boolean.TRUE.equals(DockRecentsMotion.overviewTarget(command, "startAnim", 1.06f)), "float recents endpoint");
        check(Boolean.TRUE.equals(DockRecentsMotion.overviewTarget(command, "startAnim", 1.1129999160766602)), "device recents endpoint with 1.05 base");
        check(Boolean.FALSE.equals(DockRecentsMotion.overviewTarget(command, "startAnim", 1.0499999523162842)), "device home endpoint");
        for (double scale : new double[]{1.05, 1.197, 1.239}) {
            check(Boolean.FALSE.equals(DockRecentsMotion.overviewTarget(command, "startAnim", scale)), "OS4 exit scene " + scale);
            check(Boolean.FALSE.equals(DockRecentsMotion.overviewTarget(command, "setTo", scale)), "instant exit scene " + scale);
        }
        check(DockRecentsMotion.homeTarget(command, "startAnim", 1.0499999523162842), "device home refresh");
        check(DockRecentsMotion.homeTarget(command, "setTo", 1.0), "legacy home refresh");
        for (double scale : new double[]{1.06, 1.113, 1.14, 1.18, 1.197, 1.239}) {
            check(!DockRecentsMotion.homeTarget(command, "startAnim", scale), "non-home does not refresh " + scale);
        }
        check(!DockRecentsMotion.homeTarget("other", "startAnim", 1.05), "foreign command does not refresh");
        for (double scale : new double[]{1.14, 1.18, 1.197f, 1.239f}) {
            check(DockRecentsMotion.leavingHomeTarget(command, "startAnim", scale), "pause on app zoom " + scale);
            check(DockRecentsMotion.leavingHomeTarget(command, "setTo", scale), "pause on immediate app zoom " + scale);
        }
        for (double scale : new double[]{1.0, 1.05, 1.06, 1.113, Double.NaN, 1.196, 1.2}) {
            check(!DockRecentsMotion.leavingHomeTarget(command, "startAnim", scale), "do not freeze home/recents/unknown " + scale);
        }
        check(!DockRecentsMotion.leavingHomeTarget("other", "startAnim", 1.197), "foreign command cannot pause");
        check(!DockRecentsMotion.leavingHomeTarget(command, "cancelAnim", 1.197), "unknown action cannot pause");
        check(Boolean.TRUE.equals(DockRecentsMotion.overviewTarget(command, "setTo", 1.113f)), "instant recents endpoint");
        for (double scale : new double[]{1.0, 1.14, 1.18}) {
            check(Boolean.FALSE.equals(DockRecentsMotion.overviewTarget(command, "startAnim", scale)), "exit scene " + scale);
        }
        check(DockRecentsMotion.overviewTarget("other", "startAnim", 1.06) == null, "unrelated command");
        check(DockRecentsMotion.overviewTarget(command, null, 1.06) == null, "missing action");
        check(DockRecentsMotion.overviewTarget(command, "cancelAnim", 1.06) == null, "unknown semantics ignored");
        for (double scale : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1, 0, 1.03, 1.0602, 1.112, 1.114, 1.2, 2}) {
            check(DockRecentsMotion.overviewTarget(command, "startAnim", scale) == null, "invalid/unknown scale");
        }
        var motion = new DockRecentsMotion();
        close(motion.offsetY(3, 1905, 1000), 0, "initial position");
        check(!motion.isRunning(1000), "no idle frame loop");
        check(motion.setOverview(true, 1000), "enter");
        close(motion.progress(1000), 0, "no initial jump");
        // Analytic spring response at 75ms is ~0.497, old 250ms smoothstep only 0.216.
        check(motion.progress(1075) > .47f && motion.progress(1075) < .52f, "front-loaded native backdrop timing");
        close(motion.offsetY(3, 1905, 1075), -60 * motion.progress(1075), "density scaled lift");
        float duplicatePosition = motion.progress(1075);
        double duplicateVelocity = motion.velocity(1075);
        check(!motion.setOverview(true, 1075), "duplicate signal does not restart");
        close(motion.progress(1075), duplicatePosition, "duplicate position retained");
        check(Math.abs(motion.velocity(1075) - duplicateVelocity) < 1e-9, "duplicate velocity retained");
        close(motion.progress(1400), 1, "endpoint");
        close(motion.offsetY(3, 1905, 1400), -60, "20dp total lift");
        check(!motion.isRunning(1400), "frames stop at completion");
        // A completed spring must not resume its tiny theoretical oscillation on a later traversal.
        for (long now = 1400; now < 2300; now++) close(motion.progress(now), 1, "settled endpoint stays latched");
        check(motion.setOverview(false, 2400), "exit");
        close(motion.progress(2400), 1, "no exit jump");
        check(motion.progress(2475) > .70f && motion.progress(2475) < .75f, "independent slower return preset");
        close(motion.progress(3400), 0, "restored exact base position");
        motion.setOverview(true, 4000);
        float beforeCancel = motion.progress(4075);
        double speedBeforeCancel = motion.velocity(4075);
        motion.setOverview(false, 4075);
        close(motion.progress(4075), beforeCancel, "cancel is position continuous");
        check(Math.abs(motion.velocity(4075) - speedBeforeCancel) < 1e-9, "cancel retains velocity");
        float beforeReenter = motion.progress(4125);
        double speedBeforeReenter = motion.velocity(4125);
        motion.setOverview(true, 4125);
        close(motion.progress(4125), beforeReenter, "rapid reentry is position continuous");
        check(Math.abs(motion.velocity(4125) - speedBeforeReenter) < 1e-9, "reentry retains velocity");
        for (long now = 4125; now <= 5700; now++) {
            check(motion.progress(now) >= 0 && motion.progress(now) <= 1, "no overshoot");
        }
        check(!motion.isRunning(5700), "bounded completion after interruptions");
        close(motion.offsetY(3, 10, 5700), -10, "clamped above screen top");
        close(motion.offsetY(Float.NaN, 1905, 5700), 0, "invalid density");
        close(motion.offsetY(3, -1, 5700), 0, "invalid bounds");
        motion.setOverview(false, 6000);
        motion.finish();
        close(motion.progress(6000), 0, "hidden host snaps to latest target");
        check(!motion.isRunning(6000), "hidden host schedules no frames");
        check(motion.velocity(6000) == 0, "instant finish discards momentum");
        System.out.println("DockRecentsMotion tests passed");
    }
}
