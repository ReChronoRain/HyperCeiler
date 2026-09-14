package com.sevtinge.hyperceiler.tests.dock;

/* SPDX-License-Identifier: AGPL-3.0-or-later */
import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockNativeMotion;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockRecentsMotion;

public class DockNativeMotionTest {
    private static DockNativeMotion.Sample sample(long sequence, long time, int scene, double scale,
                                                   long previousSequence, long now) {
        return DockNativeMotion.validate(sequence, time, packed(scene, scale),
            1, 1, previousSequence, now);
    }
    private static long packed(int scene, double scale) {
        return (Double.doubleToRawLongBits(scale) & ~3L) | scene;
    }
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    private static void near(float actual, float expected) { check(Math.abs(actual - expected) < 0.001); }
    public static void main(String[] args) {
        long now = 1_000_000_000L;
        check(sample(1, now, 1, .99, 0, now) != null);
        check(sample(1, now, 1, .99, 1, now) == null);
        check(sample(1, now, 1, .99, 0, now - 1) == null);
        check(sample(1, now, 1, .99, 0, now + DockNativeMotion.MAX_AGE_NS + 1) == null);
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -1, 2.1}) {
            check(sample(1, now, 1, invalid, 0, now) == null);
        }
        // Scenes are packed in two bits, so 0..3 are the whole representable range and scene 3
        // (auto-aim projection) is a valid sample rather than a rejected one.
        check(sample(1, now, 3, .99, 0, now) != null);
        check(DockNativeMotion.validate(1, now,
            packed(1, .99), -1, 0, 0, now) == null);
        check(DockNativeMotion.validate(1, now,
            packed(1, .99), 0, -1, 0, now) == null);
        check(DockNativeMotion.validate(1, now,
            packed(1, .99), 1, 2, 0, now) == null);
        DockNativeMotion.Sample initialKeepalive = DockNativeMotion.validate(1, now,
            packed(0, 1), 3, 0, 0, now);
        check(initialKeepalive != null
            && !DockNativeMotion.hasPublishedProgress(null, initialKeepalive));
        DockNativeMotion.Sample published = DockNativeMotion.validate(2, now,
            packed(1, .99), 4, 1, 1, now);
        check(DockNativeMotion.hasPublishedProgress(null, published));
        DockNativeMotion.Sample entryOnly = DockNativeMotion.validate(3, now,
            packed(1, .99), 5, 1, 2, now);
        check(!DockNativeMotion.hasPublishedProgress(published, entryOnly));
        DockNativeMotion.Sample nextPublish = DockNativeMotion.validate(4, now,
            packed(1, .99), 6, 2, 3, now);
        check(DockNativeMotion.hasPublishedProgress(published, nextPublish));
        DockNativeMotion hinted = new DockNativeMotion();
        hinted.accept(sample(1, now, 0, .98, 0, now), true);
        near(hinted.progress(), .4f); // Verified overview target fills a transient native scene gap.
        hinted.accept(sample(2, now, 0, .97, 0, now), false);
        near(hinted.progress(), .6f); // An existing native recents latch survives a short scene gap.
        hinted.accept(sample(3, now, 0, 1, 0, now), false);
        near(hinted.progress(), 0);
        hinted.accept(sample(4, now, 1, .97, 0, now), false);
        hinted.accept(sample(5, now, 0, .85, 0, now), false);
        near(hinted.progress(), 0); // Folder/app scale cannot inherit an old recents latch.
        DockNativeMotion lateHint = new DockNativeMotion();
        DockNativeMotion.Sample beforeOverview = sample(1, now, 0, .98, 0, now);
        lateHint.accept(beforeOverview, false);
        near(lateHint.progress(), 0);
        check(lateHint.accept(beforeOverview, true));
        near(lateHint.progress(), .4f); // The later window hint reinterprets the same sample.
        check(!lateHint.accept(beforeOverview, true));
        DockNativeMotion motion = new DockNativeMotion();
        motion.accept(sample(1, now, 2, .96, 0, now));
        near(motion.progress(), 0); // Folder/home return cannot start a recents lift.
        motion.accept(sample(2, now, 1, .99, 0, now));
        near(motion.progress(), .2f); // Follows drag before wallpaper overview arrives.
        near(motion.offsetY(3.25f, 2000), -13);
        motion.accept(sample(3, now, 1, .95, 0, now));
        near(motion.progress(), 1);
        motion.accept(sample(4, now, 1, .945, 0, now));
        near(motion.progress(), 1.1f); // Preserve measured small native spring overshoot.
        motion.accept(sample(5, now, 1, .91, 0, now));
        near(motion.progress(), 1.2f); // Independent safety limit of 24dp.
        motion.accept(sample(6, now, 2, .98, 0, now));
        near(motion.progress(), .4f);
        check(!motion.accept(sample(5, now, 1, .95, 0, now)));
        near(motion.progress(), .4f);
        motion.accept(sample(7, now, 2, 1, 0, now));
        near(motion.progress(), 0);
        motion.accept(sample(8, now, 2, .96, 0, now));
        near(motion.progress(), 0); // Return completed: no stale latch for another scene.
        motion.accept(sample(9, now, 1, .8, 0, now));
        near(motion.progress(), 1.2f); // Large authenticated drag remains clamped, never snaps home.
        motion.reset();
        motion.accept(sample(9, now, 0, .96, 0, now));
        near(motion.progress(), 0); // Scene 0 cannot start recents by itself.
        motion.accept(sample(10, now, 1, .97, 0, now));
        motion.accept(sample(11, now, 0, .96, 0, now));
        near(motion.progress(), .8f); // Transient scene 0 during a drag preserves the position.
        motion.accept(sample(12, now, 0, 1, 0, now));
        near(motion.progress(), 0);
        motion.reset();
        motion.accept(sample(1, now, 1, .98, 0, now));
        near(motion.progress(), .4f);
        near(motion.offsetY(Float.NaN, 2000), 0);
        near(motion.offsetY(3.25f, 1), -1);
        DockRecentsMotion fallback = new DockRecentsMotion();
        fallback.resumeFrom(.4f, false, 100);
        near(fallback.progress(100), .4f);
        near(fallback.progress(1700), 0);
        System.out.println("DockNativeMotion tests passed");
    }
}
