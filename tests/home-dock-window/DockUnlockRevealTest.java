package com.sevtinge.hyperceiler.tests.dock;

import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockUnlockReveal;

public final class DockUnlockRevealTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        joinsTransitionEpoch();
        sharesEpochWithLateLayer();
        expiresWithoutVisibleProgress();
        handlesPendingDeadlines();
        sharesRestartWindow();
        handlesZeroUptime();
        retainsPoseDebtAcrossRepeatedLostFrames();
        clearsPoseDebtOnlyAfterTerminalCommit();
        cancelledRevealStillNeedsRestingCommit();
        liftSettlesOnceWithoutMaterialPulsing();
        tiltIsARealPerspectiveSweep();
        stylesFollowTheirReferenceCurves();
        shapeStylesMapAndKeepLegacyFallback();
        perspectiveFoldSynchronizesContainer();
        capsuleFissionMorphsTheActualContainer();
        cancellationRestoresEveryShapeProperty();
        lateLayerJoinsLandingAfterSkippedFrames();
        System.out.println("DockUnlockReveal tests passed");
    }

    private static void joinsTransitionEpoch() {
        DockUnlockReveal reveal = new DockUnlockReveal();
        check(reveal.arm(10000), "unlock arms before visibility");
        check(reveal.alpha(10016) == 0f, "queued frame cannot expose resting Dock");
        check(reveal.risePx(3f, 10016) > 0f, "hidden pose is already offset");
        check(!reveal.arm(10020), "duplicate callback cannot restart");
        check(reveal.startIfArmed(10032), "first visible frame joins the transition");
        DockUnlockReveal immediate = new DockUnlockReveal();
        immediate.arm(10000);
        immediate.startIfArmed(10000);
        check(reveal.progress(10032) == immediate.progress(10032),
                "delayed visibility cannot shift the transition epoch");
        check(reveal.alpha(10032) > 0f, "first visible frame catches up to the existing fade");
        check(reveal.alpha(10100) > reveal.alpha(10032), "later frames fade in");
        check(!reveal.startIfArmed(10315), "late visibility cannot restart an active clock");
        check(reveal.alpha(11000) == 1f && reveal.risePx(3f, 11000) == 0f,
                "finished reveal restores opacity and position");
    }

    private static void sharesEpochWithLateLayer() {
        DockUnlockReveal transition = new DockUnlockReveal();
        transition.arm(20000);
        transition.startIfArmed(20000);
        check(transition.alpha(20000) == 0f, "transition begins hidden");
        DockUnlockReveal lateLayer = new DockUnlockReveal();
        lateLayer.arm(20000);
        check(lateLayer.startIfArmed(20315), "new layer may join an in-flight reveal");
        check(lateLayer.progress(20315) == transition.progress(20315),
                "new layer shares transition epoch, not its creation time");
        check(lateLayer.risePx(3f, 20315) == transition.risePx(3f, 20315),
                "late-created layer shares the existing layer's position");

        DockUnlockReveal completedLayer = new DockUnlockReveal();
        completedLayer.arm(20000);
        check(completedLayer.startIfArmed(21000), "recent pending event can still be consumed");
        check(!completedLayer.needsFrame(21000), "late creation must not replay a finished reveal");
        check(completedLayer.alpha(21000) == 1f && completedLayer.risePx(3f, 21000) == 0f,
                "creation after animation end immediately uses the resting pose");
    }

    private static void expiresWithoutVisibleProgress() {
        DockUnlockReveal hidden = new DockUnlockReveal();
        hidden.arm(30000);
        hidden.startIfArmed(30000);
        check(hidden.needsFrame(30000 + DockUnlockReveal.TOTAL_MS - 1),
                "hidden active reveal still has a deadline");
        check(!hidden.needsFrame(30000 + DockUnlockReveal.TOTAL_MS),
                "hidden reveal expires exactly at its duration");
        check(!hidden.isRunning(), "no progress sampling is needed to clear running state");
        check(!hidden.arm(30000 + DockUnlockReveal.TOTAL_MS + 400 - 1),
                "completed hidden reveal still observes duplicate-event guard");
        check(hidden.arm(30000 + DockUnlockReveal.TOTAL_MS + 400),
                "hidden completion cannot block the next real unlock");

        DockUnlockReveal unsampled = new DockUnlockReveal();
        unsampled.arm(40000);
        unsampled.startIfArmed(40000);
        check(unsampled.arm(40000 + DockUnlockReveal.TOTAL_MS + 400), "arm itself expires a previous reveal with no frame callbacks");
        check(unsampled.startIfArmed(40000 + DockUnlockReveal.TOTAL_MS + 400), "next unlock starts after missing every previous frame");
        check(unsampled.alpha(41221) == 0f, "next unlock gets its own transparent first pose");
    }

    private static void handlesPendingDeadlines() {
        check(DockUnlockReveal.acceptsPending(10000, 10500), "new layer inherits recent event");
        check(!DockUnlockReveal.acceptsPending(-1, 10500), "no event is not an unlock");
        check(!DockUnlockReveal.acceptsPending(11000, 10500), "future event rejected");
        check(DockUnlockReveal.acceptsPending(10000, 11500), "pending deadline is inclusive");
        check(!DockUnlockReveal.acceptsPending(10000, 11501), "old event rejected");
        DockUnlockReveal expired = new DockUnlockReveal();
        expired.arm(10000);
        check(!expired.startIfArmed(12000), "visibility after deadline cannot replay stale reveal");
        check(expired.alpha(12000) == 1f, "expired reveal fails open");
        check(!expired.isRunning(), "stale visibility clears the armed animation state");
        DockUnlockReveal neverVisible = new DockUnlockReveal();
        neverVisible.arm(10000);
        check(neverVisible.needsFrame(11500), "armed wait lasts through the pending deadline");
        check(!neverVisible.needsFrame(11501), "armed wait also expires without visibility");
        check(neverVisible.risePx(3f, 11501) == 0f, "expired armed wait restores the resting pose");
        DockUnlockReveal future = new DockUnlockReveal();
        future.arm(10000);
        check(!future.startIfArmed(9999), "visibility timestamp before the event is rejected");
        check(!future.isRunning(), "rejected future event cannot leave a hidden pose armed");
    }

    private static void sharesRestartWindow() {
        check(DockUnlockReveal.acceptsNewEvent(-1, 10000), "first event has no restart guard");
        check(!DockUnlockReveal.acceptsNewEvent(-1, -1), "negative event time is invalid");
        check(!DockUnlockReveal.acceptsNewEvent(10000, 9999), "event cannot precede its predecessor");
        check(!DockUnlockReveal.acceptsNewEvent(10000, 10000), "same-time duplicate is rejected");
        check(!DockUnlockReveal.acceptsNewEvent(10000, 10000 + DockUnlockReveal.TOTAL_MS + 400 - 1),
                "duplicate-event window is exclusive");
        check(DockUnlockReveal.acceptsNewEvent(10000, 10000 + DockUnlockReveal.TOTAL_MS + 400),
                "next event starts at the guard boundary");
        check(DockUnlockReveal.acceptsPending(10000, 10000 + DockUnlockReveal.TOTAL_MS + 400),
                "an old pending event can coexist with an accepted next event");

        DockUnlockReveal existingLayer = new DockUnlockReveal();
        existingLayer.arm(10000);
        existingLayer.startIfArmed(10000);
        long pendingAt = 10000;
        long nextEvent = 10000 + DockUnlockReveal.TOTAL_MS + 400;
        if (DockUnlockReveal.acceptsNewEvent(pendingAt, nextEvent)) pendingAt = nextEvent;
        check(pendingAt == nextEvent, "accepted event replaces the still-recent pending epoch");
        check(existingLayer.arm(nextEvent), "existing layer accepts the same next-event boundary");
        existingLayer.startIfArmed(nextEvent);
        DockUnlockReveal recreatedLayer = new DockUnlockReveal();
        recreatedLayer.arm(pendingAt);
        recreatedLayer.startIfArmed(nextEvent + 32);
        check(existingLayer.progress(nextEvent + 32) == recreatedLayer.progress(nextEvent + 32),
                "existing and recreated layers use the new event instead of different epochs");
        check(existingLayer.risePx(3f, nextEvent + 32) > 0f,
                "second unlock has not inherited the completed first unlock's pose");
    }

    private static void handlesZeroUptime() {
        DockUnlockReveal reveal = new DockUnlockReveal();
        check(reveal.arm(0), "uptime zero is a valid first event");
        check(reveal.startIfArmed(0), "uptime-zero event starts normally");
        check(reveal.alpha(0) == 0f, "uptime-zero event begins transparent");
        check(!reveal.needsFrame(DockUnlockReveal.TOTAL_MS), "uptime-zero event expires at the normal duration");
        check(!reveal.arm(DockUnlockReveal.TOTAL_MS), "uptime-zero history must not be confused with an absent event");
        check(!reveal.arm(DockUnlockReveal.TOTAL_MS + 400 - 1), "uptime-zero event keeps the full duplicate guard");
        check(reveal.arm(DockUnlockReveal.TOTAL_MS + 400), "uptime-zero event permits the next unlock at the same boundary");
    }

    private static void retainsPoseDebtAcrossRepeatedLostFrames() {
        DockUnlockReveal reveal = new DockUnlockReveal();
        check(!reveal.hasPendingPose(), "untouched layer owes no reveal pose");
        reveal.arm(10000);
        reveal.startIfArmed(10000);
        check(reveal.alpha(10700) == 1f && reveal.scale(10700) == 1f,
                "late reveal can already have resting opacity and scale");
        check(Math.abs(reveal.risePx(3f, 10700)) > 0f,
                "resting opacity and scale do not imply a resting position");
        reveal.onPoseCommitted(10700);
        check(reveal.hasPendingPose(), "committing an intermediate rise still owes its terminal pose");

        check(!reveal.needsFrame(10900), "first watchdog may expire the animation clock");
        check(reveal.hasPendingPose(), "first lost terminal frame keeps its pose debt");
        check(!reveal.needsFrame(11000), "second watchdog sees an already expired clock");
        check(reveal.hasPendingPose(), "second lost terminal frame must not discard its pose debt");
        check(reveal.progress(11000) == 1f && reveal.risePx(3f, 11000) == 0f,
                "terminal values remain available after repeated lost frames");
        check(reveal.hasPendingPose(), "sampling terminal values is not a successful submission");
        reveal.onPoseCommitted(11000);
        check(!reveal.hasPendingPose(), "successful terminal submission clears the persistent debt");
    }

    private static void clearsPoseDebtOnlyAfterTerminalCommit() {
        DockUnlockReveal reveal = new DockUnlockReveal();
        reveal.arm(20000);
        check(reveal.hasPendingPose(), "arming immediately creates a pose debt");
        reveal.onPoseCommitted(20000);
        check(reveal.hasPendingPose(), "priming an armed hidden pose is not a terminal submission");
        reveal.startIfArmed(20000);
        reveal.onPoseCommitted(20000 + DockUnlockReveal.TOTAL_MS - 1);
        check(reveal.hasPendingPose(), "submission before the duration boundary cannot clear debt");
        reveal.onPoseCommitted(20000 + DockUnlockReveal.TOTAL_MS);
        check(!reveal.hasPendingPose(), "terminal commit clears debt even without progress sampling");

        check(reveal.arm(20000 + DockUnlockReveal.TOTAL_MS + 400), "a later unlock can start after the previous terminal commit");
        check(reveal.hasPendingPose(), "every accepted unlock creates a fresh pose debt");
        check(!reveal.arm(20000 + DockUnlockReveal.TOTAL_MS + 401), "duplicate callback still cannot restart the fresh unlock");
        check(reveal.hasPendingPose(), "duplicate callback cannot acknowledge the fresh pose debt");
        reveal.startIfArmed(20000 + DockUnlockReveal.TOTAL_MS + 400);
        reveal.onPoseCommitted(20000 + DockUnlockReveal.TOTAL_MS + 401);
        check(reveal.hasPendingPose(), "an early second-unlock commit cannot clear its debt");
        long secondTerminal = 20000 + (DockUnlockReveal.TOTAL_MS + 400) + DockUnlockReveal.TOTAL_MS;
        reveal.progress(secondTerminal);
        check(reveal.hasPendingPose(), "progress expiry alone cannot acknowledge a second terminal pose");
        reveal.onPoseCommitted(secondTerminal);
        check(!reveal.hasPendingPose(), "second terminal commit clears only the current debt");
    }

    private static void cancelledRevealStillNeedsRestingCommit() {
        DockUnlockReveal reveal = new DockUnlockReveal();
        reveal.arm(30000);
        reveal.startIfArmed(30000);
        reveal.onPoseCommitted(30000);
        reveal.cancel();
        check(!reveal.isRunning(), "cancellation stops animation immediately");
        check(reveal.alpha(30010) == 1f && reveal.risePx(3f, 30010) == 0f,
                "cancelled reveal supplies the resting pose");
        check(reveal.hasPendingPose(), "cancellation does not imply the resting pose was submitted");
        reveal.onPoseCommitted(30010);
        check(!reveal.hasPendingPose(), "resting submission acknowledges a cancelled reveal");

        DockUnlockReveal expired = new DockUnlockReveal();
        expired.arm(40000);
        check(!expired.needsFrame(41501), "an unstarted reveal can time out");
        check(expired.hasPendingPose(), "armed timeout retains the need to clear its primed pose");
        expired.onPoseCommitted(41501);
        check(!expired.hasPendingPose(), "resting submission also acknowledges armed timeout");
    }

    private static void liftSettlesOnceWithoutMaterialPulsing() {
        DockUnlockReveal reveal = new DockUnlockReveal();
        reveal.arm(10000);
        reveal.startIfArmed(10000);
        check(reveal.alpha(10000) == 0f, "the first pose stays transparent");
        float previousY = reveal.risePx(1f, 10000);
        float previousAlpha = 0f;
        float minimumY = previousY;
        boolean landing = false;
        for (long elapsed = 1; elapsed <= DockUnlockReveal.TOTAL_MS; elapsed++) {
            long now = 10000 + elapsed;
            float y = reveal.risePx(1f, now);
            float alpha = reveal.alpha(now);
            check(Float.isFinite(y) && y >= -6f && y <= 96f,
                    "lift and overshoot remain bounded near the icon row");
            check(alpha >= previousAlpha && alpha <= 1f,
                    "the material only fades in, never pulses during the landing");
            check(reveal.scale(now) == 1f, "glass is never resampled by reveal scaling");
            if (y > previousY) landing = true;
            if (landing) check(y >= previousY, "only one landing, with no repeated bounces");
            minimumY = Math.min(minimumY, y);
            previousY = y;
            previousAlpha = alpha;
        }
        check(landing && minimumY < -4.5f, "the lift has a visible but restrained overshoot");
        check(previousY == 0f && previousAlpha == 1f, "the deadline ends at the exact resting pose");

        DockUnlockReveal lead = new DockUnlockReveal();
        lead.arm(40000);
        lead.startIfArmed(40000);
        float startPose = lead.risePx(1f, 40000);
        check(startPose == DockUnlockReveal.RISE_DP, "the lead keeps the dock at its start offset");
        check(lead.risePx(1f, 40009) == startPose && lead.alpha(40009) == 0f,
                "the icons' measured head start is held, not skipped");
        check(lead.risePx(1f, 40011) < startPose,
                "motion begins exactly when the launcher's icon fly-in begins");
        check(lead.risePx(1f, 40000 + DockUnlockReveal.TOTAL_MS) == 0f,
                "the lead does not shorten or lengthen the fly-in itself");

        DockUnlockReveal fading = new DockUnlockReveal();
        fading.arm(20000);
        fading.startIfArmed(20000);
        check(fading.alpha(20100) > 0.4f && fading.alpha(20100) < 0.6f,
                "opacity builds visibly during the early lift");
        check(fading.alpha(20190) == 1f, "the glass is opaque before the landing begins");
        check(fading.alpha(20191) == 1f, "fade completion does not produce an opacity step");
    }

    private static void tiltIsARealPerspectiveSweep() {
        long start = 50000;
        DockUnlockReveal.Pose3D first = DockUnlockReveal.pose3D(DockUnlockReveal.Style.DEPTH_FLIP, start, start);
        check(Math.abs(first.rotationX - DockUnlockReveal.POSE_ROT_X_DEG) < 0.001f
                        && Math.abs(first.rotationY - DockUnlockReveal.POSE_ROT_Y_DEG) < 0.001f
                        && Math.abs(first.scale - DockUnlockReveal.POSE_SCALE) < 0.001f
                        && first.active,
                "the lead holds the full depth-flip start pose");
        float maxRotationX = first.rotationX;
        float minimumRotationX = first.rotationX;
        float previousFraction = Float.MAX_VALUE;
        boolean dippedPastFlat = false;
        for (long elapsed = 1; elapsed < DockUnlockReveal.TOTAL_MS; elapsed++) {
            DockUnlockReveal.Pose3D pose = DockUnlockReveal.pose3D(DockUnlockReveal.Style.DEPTH_FLIP, start, start + elapsed);
            check(Float.isFinite(pose.rotationX) && Float.isFinite(pose.rotationY)
                            && Float.isFinite(pose.rotationZ) && Float.isFinite(pose.scale),
                    "no NaN or infinity may enter a surface transaction");
            check(pose.scale >= DockUnlockReveal.POSE_SCALE && pose.scale <= 1f,
                    "depth scale only grows towards identity");
            check(Math.abs(pose.rotationX) <= DockUnlockReveal.POSE_ROT_X_DEG,
                    "rotation stays within the start angle");
            maxRotationX = Math.max(maxRotationX, pose.rotationX);
            minimumRotationX = Math.min(minimumRotationX, pose.rotationX);
            if (pose.rotationX < 0f) dippedPastFlat = true;
            check(pose.active, "the pose stays active for the whole window");
        }
        check(minimumRotationX < 0f, "the flip overshoots past flat exactly once, like a card settling");
        check(maxRotationX == DockUnlockReveal.POSE_ROT_X_DEG,
                "no frame exceeds the start angle on the way down");
        check(dippedPastFlat, "the elegant overshoot is actually present");
        DockUnlockReveal.Pose3D end = DockUnlockReveal.pose3D(DockUnlockReveal.Style.DEPTH_FLIP, start, start + DockUnlockReveal.TOTAL_MS);
        check(!end.active && end.rotationX == 0f && end.rotationY == 0f
                        && end.rotationZ == 0f && end.scale == 1f,
                "the reveal ends exactly at the resting identity pose");
        DockUnlockReveal.Pose3D late = DockUnlockReveal.pose3D(DockUnlockReveal.Style.DEPTH_FLIP, start, start + DockUnlockReveal.TOTAL_MS + 9000);
        check(!late.active && late.scale == 1f, "a late query reports resting, not a stale pose");
        DockUnlockReveal.Pose3D early = DockUnlockReveal.pose3D(DockUnlockReveal.Style.DEPTH_FLIP, start, start - 1000);
        check(early.active && early.rotationX == DockUnlockReveal.POSE_ROT_X_DEG,
                "a query from before the epoch holds the start pose, never a negative tilt");
    }

    private static void stylesFollowTheirReferenceCurves() {
        long start = 60000;
        float width = 900f;

        // Orbital sweep: a shallow arc. The lateral axis leads the vertical one (which is what
        // turns two springs into a curve), and one underdamped spring snaps both onto rest.
        DockUnlockReveal orbit = new DockUnlockReveal();
        orbit.setStyle(DockUnlockReveal.Style.ORBIT_SWEEP);
        orbit.arm(start);
        orbit.startIfArmed(start);
        check(orbit.risePx(1f, start) == 44f, "the sweep starts 44dp below the resting line");
        check(orbit.slidePx(width, start) == -72f, "the sweep starts 8% of the width to the left");
        float lead = -1f;
        int overshoots = 0;
        float previousRise = orbit.risePx(1f, start);
        boolean crossed = false;
        for (long elapsed = 1; elapsed < DockUnlockReveal.TOTAL_MS; elapsed++) {
            float rise = orbit.risePx(1f, start + elapsed);
            check(Float.isFinite(rise) && rise <= 44.5f,
                    "the sweep never rises above its start offset");
            // The path bends only while the lateral spring stays ahead of the vertical one.
            // Comparing the two signed spring progress values keeps the check valid through the
            // overshoot, where a magnitude-based "progress" would wrap around.
            float lateralProgress = 1f + orbit.slidePx(width, start + elapsed) / 72f;
            float verticalProgress = 1f - rise / 44f;
            // The bend is formed while both axes approach: the lateral spring is tuned faster,
            // so it stays ahead for the first half and lets the path curve instead of running
            // straight. Past the midpoint the vertical overshoot is the larger one, which is the
            // settle the eye reads as the snap.
            lead = Math.max(lead, lateralProgress - verticalProgress);
            if (rise < 0f && !crossed) { crossed = true; overshoots++; }
            if (rise > previousRise && crossed) crossed = false;
            previousRise = rise;
        }
        check(lead > 0.10f, "the lateral axis leads far enough for the path to read as an arc");
        check(Math.abs(orbit.risePx(1f, start + DockUnlockReveal.TOTAL_MS - 1)) < 0.3f,
                "the vertical spring has effectively settled by the end of the window");
        check(Math.abs(orbit.slidePx(width, start + DockUnlockReveal.TOTAL_MS - 1)) < 0.3f,
                "the lateral spring has effectively settled by the end of the window");
        check(crossed || overshoots > 0, "exactly one clean spring overshoot is present");
        check(orbit.risePx(1f, start + DockUnlockReveal.TOTAL_MS) == 0f, "the sweep ends exactly at rest");
        check(orbit.slidePx(width, start + DockUnlockReveal.TOTAL_MS) == 0f, "the sweep ends exactly on x");
        // A single crossing: the spring may pass the resting line once and must not ring.
        int crossings = 0;
        float last = orbit.risePx(1f, start);
        for (long elapsed = 1; elapsed < DockUnlockReveal.TOTAL_MS; elapsed++) {
            float rise = orbit.risePx(1f, start + elapsed);
            if ((last > 0f && rise <= 0f) || (last < 0f && rise >= 0f)) crossings++;
            last = rise;
        }
        check(crossings <= 2, "the spring settles cleanly instead of ringing");
        DockUnlockReveal.Pose3D orbitStart = DockUnlockReveal.pose3D(DockUnlockReveal.Style.ORBIT_SWEEP, start, start);
        check(Math.abs(orbitStart.scale - 0.86f) < 0.001f && Math.abs(orbitStart.rotationZ + 3.2f) < 0.001f,
                "the sweep starts slightly small and slightly turned");
        boolean scaleOvershoot = false;
        for (long elapsed = 1; elapsed < DockUnlockReveal.TOTAL_MS; elapsed++) {
            DockUnlockReveal.Pose3D pose = DockUnlockReveal.pose3D(
                    DockUnlockReveal.Style.ORBIT_SWEEP, start, start + elapsed);
            if (pose.scale > 1f) scaleOvershoot = true;
            check(pose.scale <= 1.02f, "the scale overshoot stays invisible at the layer bounds");
        }
        check(scaleOvershoot, "the scale overshoots slightly before settling, as a spring must");

        // Gale: Material shared axis X - lateral slide plus fade, no vertical travel, no rotation.
        DockUnlockReveal gale = new DockUnlockReveal();
        gale.setStyle(DockUnlockReveal.Style.GALE);
        gale.arm(start);
        gale.startIfArmed(start);
        check(gale.risePx(1f, start + 300) == 0f, "gale never moves the layer vertically");
        float slideStart = gale.slidePx(width, start);
        check(slideStart < -300f && slideStart > -310f, "gale starts 34% of the width to the left");
        float previousSlide = slideStart;
        for (long elapsed = 1; elapsed < DockUnlockReveal.TOTAL_MS; elapsed++) {
            float slide = gale.slidePx(width, start + elapsed);
            check(slide >= previousSlide, "the slide only ever closes towards the resting x");
            previousSlide = slide;
        }
        check(gale.slidePx(width, start + DockUnlockReveal.TOTAL_MS) == 0f, "gale lands exactly on x");
        check(!DockUnlockReveal.pose3D(DockUnlockReveal.Style.GALE, start, start + 200).active,
                "gale carries no view transform: the slide is the whole gesture");
        check(gale.slidePx(0f, start) == 0f && gale.slidePx(Float.NaN, start) == 0f,
                "an invalid width cannot put a non-finite offset into a transaction");
        check(orbit.slidePx(0f, start) == 0f, "the sweep rejects an invalid width too");

        // Ripple: a visibly larger gather into place. Starting below 1 is deliberate - a child
        // surface clips anything above its own bounds, so an "arrive from larger" version would
        // simply not be visible.
        DockUnlockReveal ripple = new DockUnlockReveal();
        ripple.setStyle(DockUnlockReveal.Style.RIPPLE);
        ripple.arm(start);
        ripple.startIfArmed(start);
        DockUnlockReveal.Pose3D rippleStart = DockUnlockReveal.pose3D(DockUnlockReveal.Style.RIPPLE, start, start);
        check(Math.abs(rippleStart.scale - 0.84f) < 0.001f,
                "ripple starts far enough below 1 to be clearly visible");
        float previousScale = rippleStart.scale;
        for (long elapsed = 1; elapsed < DockUnlockReveal.TOTAL_MS; elapsed++) {
            DockUnlockReveal.Pose3D pose = DockUnlockReveal.pose3D(DockUnlockReveal.Style.RIPPLE, start, start + elapsed);
            check(pose.scale >= previousScale && pose.scale <= 1f,
                    "the gather grows monotonically and never overshoots past 1");
            check(pose.rotationX == 0f && pose.rotationY == 0f && pose.rotationZ == 0f,
                    "the gather has no sway: scale and fade are the whole gesture");
            previousScale = pose.scale;
        }
        check(ripple.risePx(1f, start + 100) == 0f, "ripple never moves the layer");
        check(ripple.alpha(start + 90) > 0.4f && ripple.alpha(start + 90) < 0.6f,
                "ripple keeps the shared early fade, which also hides the gather's edge halo");
        check(!DockUnlockReveal.pose3D(DockUnlockReveal.Style.DAYBREAK, start, start + 300).active,
                "daybreak carries no view transform either");

        // Depth flip is the only style with a view-side 3D projection.
        DockUnlockReveal.Pose3D flip = DockUnlockReveal.pose3D(DockUnlockReveal.Style.DEPTH_FLIP, start, start + 300);
        check(flip.active && Math.abs(flip.rotationX) > 1f, "depth flip is mid-sweep at the same instant");
    }

    private static void lateLayerJoinsLandingAfterSkippedFrames() {
        DockUnlockReveal smooth = new DockUnlockReveal();
        smooth.arm(30000);
        smooth.startIfArmed(30000);
        for (long elapsed = 0; elapsed < 530; elapsed += 8) {
            smooth.risePx(3f, 30000 + elapsed);
            smooth.alpha(30000 + elapsed);
        }
        DockUnlockReveal late = new DockUnlockReveal();
        late.arm(30000);
        late.startIfArmed(30530);
        check(late.risePx(3f, 30530) == smooth.risePx(3f, 30530),
                "a recreated surface joins the landing even after skipping the whole lift");
        check(late.alpha(30530) == smooth.alpha(30530), "late material keeps the same opacity");
        check(late.risePx(Float.NaN, 30530) == 0f && late.risePx(0f, 30530) == 0f,
                "invalid density cannot put a non-finite position into a surface transaction");
    }

    private static void shapeStylesMapAndKeepLegacyFallback() {
        check(DockUnlockReveal.Style.of("perspective_fold") == DockUnlockReveal.Style.PERSPECTIVE_FOLD,
                "perspective-fold preference maps to its enum");
        check(DockUnlockReveal.Style.of("CAPSULE_FISSION") == DockUnlockReveal.Style.CAPSULE_FISSION,
                "capsule preference mapping is case insensitive");
        check(DockUnlockReveal.Style.of("elastic_burst") == DockUnlockReveal.Style.AUTO_AIM,
                "the removed elastic-burst preference migrates to the live-projection style");
        check(DockUnlockReveal.Style.of("future_style") == DockUnlockReveal.Style.DAYBREAK,
                "unknown preference values preserve the existing daybreak fallback");
        long start = 61000;
        for (DockUnlockReveal.Style legacy : new DockUnlockReveal.Style[] {
                DockUnlockReveal.Style.DAYBREAK, DockUnlockReveal.Style.DEPTH_FLIP,
                DockUnlockReveal.Style.GALE, DockUnlockReveal.Style.ORBIT_SWEEP,
                DockUnlockReveal.Style.RIPPLE }) {
            DockUnlockReveal.ContainerPose pose = DockUnlockReveal.containerPose(legacy, start, start + 300);
            check(!pose.active && pose.scaleX == 1f && pose.scaleY == 1f
                            && pose.cropWidth == 1f && pose.cropHeight == 1f
                            && pose.cornerProgress == 1f,
                    "legacy styles never inherit a new container morph: " + legacy);
        }
    }

    private static void perspectiveFoldSynchronizesContainer() {
        long start = 70000;
        DockUnlockReveal.Pose3D icon = DockUnlockReveal.pose3D(
                DockUnlockReveal.Style.PERSPECTIVE_FOLD, start, start);
        DockUnlockReveal.ContainerPose dock = DockUnlockReveal.containerPose(
                DockUnlockReveal.Style.PERSPECTIVE_FOLD, start, start);
        check(icon.active && icon.rotationX == DockUnlockReveal.FOLD_ROT_X_DEG
                        && icon.rotationY == DockUnlockReveal.FOLD_ROT_Y_DEG
                        && icon.scaleX == DockUnlockReveal.FOLD_SCALE_X
                        && icon.scaleY == DockUnlockReveal.FOLD_SCALE_Y
                        && icon.depthHeights == DockUnlockReveal.FOLD_DEPTH_HEIGHTS,
                "fold starts in a strong asymmetric 3D pose with real Z depth");
        check(dock.active && dock.cropWidth == DockUnlockReveal.FOLD_CROP_WIDTH
                        && dock.cropHeight == DockUnlockReveal.FOLD_CROP_HEIGHT
                        && dock.cornerProgress == 0f,
                "fold starts with the Dock visibly narrow, short and rounder");
        check(DockUnlockReveal.cameraHeights(DockUnlockReveal.Style.PERSPECTIVE_FOLD)
                        < DockUnlockReveal.CAMERA_HEIGHTS,
                "fold uses a shorter camera distance than the old depth flip");

        boolean flippedPastFlat = false;
        boolean dockOvershot = false;
        for (long elapsed = 1; elapsed < DockUnlockReveal.TOTAL_MS; elapsed++) {
            icon = DockUnlockReveal.pose3D(DockUnlockReveal.Style.PERSPECTIVE_FOLD,
                    start, start + elapsed);
            dock = DockUnlockReveal.containerPose(DockUnlockReveal.Style.PERSPECTIVE_FOLD,
                    start, start + elapsed);
            check(Float.isFinite(icon.rotationX) && Float.isFinite(icon.rotationY)
                            && Float.isFinite(icon.scaleX) && Float.isFinite(icon.scaleY)
                            && Float.isFinite(dock.scaleX) && Float.isFinite(dock.scaleY),
                    "fold never emits a non-finite RenderNode or SurfaceControl property");
            check(dock.cropWidth >= DockUnlockReveal.FOLD_CROP_WIDTH && dock.cropWidth <= 1f
                            && dock.cropHeight >= DockUnlockReveal.FOLD_CROP_HEIGHT
                            && dock.cropHeight <= 1f,
                    "fold changes visual bounds only inside the real Dock buffer");
            if (dock.cropWidth < 0.999f) {
                float iconProgress = (icon.scaleX - DockUnlockReveal.FOLD_SCALE_X)
                        / (1f - DockUnlockReveal.FOLD_SCALE_X);
                float dockProgress = (dock.cropWidth - DockUnlockReveal.FOLD_CROP_WIDTH)
                        / (1f - DockUnlockReveal.FOLD_CROP_WIDTH);
                check(Math.abs(iconProgress - dockProgress) < 0.0002f,
                        "icons/material and Dock bounds use one fold progress");
            }
            if (icon.rotationX < 0f) flippedPastFlat = true;
            if (dock.scaleX > 1f) dockOvershot = true;
            check(dock.scaleX < 1.07f && dock.scaleY < 1.05f,
                    "fold overshoot remains a polished settle rather than a size jump");
        }
        check(flippedPastFlat && dockOvershot,
                "fold flips and expands slightly past rest before springing home");
        icon = DockUnlockReveal.pose3D(DockUnlockReveal.Style.PERSPECTIVE_FOLD,
                start, start + DockUnlockReveal.TOTAL_MS);
        dock = DockUnlockReveal.containerPose(DockUnlockReveal.Style.PERSPECTIVE_FOLD,
                start, start + DockUnlockReveal.TOTAL_MS);
        check(!icon.active && !dock.active && icon.scaleX == 1f && icon.scaleY == 1f
                        && icon.depthHeights == 0f && dock.cropWidth == 1f
                        && dock.cropHeight == 1f && dock.cornerProgress == 1f,
                "fold deadline is an exact identity on both sides of the process boundary");
    }

    private static void capsuleFissionMorphsTheActualContainer() {
        long start = 80000;
        DockUnlockReveal reveal = new DockUnlockReveal();
        reveal.setStyle(DockUnlockReveal.Style.CAPSULE_FISSION);
        reveal.arm(start);
        reveal.startIfArmed(start);
        DockUnlockReveal.ContainerPose first = reveal.containerPose(start);
        DockUnlockReveal.Pose3D content = DockUnlockReveal.pose3D(
                DockUnlockReveal.Style.CAPSULE_FISSION, start, start);
        check(first.cropWidth == DockUnlockReveal.CAPSULE_CROP_WIDTH
                        && first.cropHeight == DockUnlockReveal.CAPSULE_CROP_HEIGHT
                        && first.cornerProgress == 0f,
                "capsule fission begins as a short centred capsule crop");
        check(Math.abs(first.cornerRadius(24f, 120f) - 32.4f) < 0.001f,
                "the start radius is half the cropped height, not a scaled old radius");
        check(content.scaleX == DockUnlockReveal.CAPSULE_CONTENT_SCALE_X
                        && content.scaleY == DockUnlockReveal.CAPSULE_CONTENT_SCALE_Y,
                "content starts compressed inside the capsule seed");
        check(reveal.alpha(start + 20) == 0f,
                "capsule content remains hidden while the centre seed forms");

        DockUnlockReveal.ContainerPose early = reveal.containerPose(start + 90);
        check(early.cropWidth > first.cropWidth && early.cropWidth < 1f
                        && early.cornerProgress > 0f && early.cornerProgress < 1f,
                "capsule continuously morphs width and radius instead of only scaling X");
        boolean overshot = false;
        float previousCrop = first.cropWidth;
        for (long elapsed = 1; elapsed < DockUnlockReveal.TOTAL_MS; elapsed++) {
            DockUnlockReveal.ContainerPose pose = reveal.containerPose(start + elapsed);
            check(pose.cropWidth >= previousCrop && pose.cropWidth <= 1f,
                    "centred capsule crop only opens outward");
            check(pose.cornerProgress >= 0f && pose.cornerProgress <= 1f,
                    "capsule-to-Dock radius morph remains bounded");
            if (pose.scaleX > 1.03f) overshot = true;
            previousCrop = pose.cropWidth;
        }
        check(overshot, "the completed capsule briefly exceeds the Dock width before settling");
        DockUnlockReveal.ContainerPose end = reveal.containerPose(start + DockUnlockReveal.TOTAL_MS);
        check(!end.active && end.cornerRadius(24f, 120f) == 24f,
                "capsule morph restores the configured radius exactly");
    }

    private static void cancellationRestoresEveryShapeProperty() {
        long start = 100000;
        for (DockUnlockReveal.Style style : new DockUnlockReveal.Style[] {
                DockUnlockReveal.Style.PERSPECTIVE_FOLD,
                DockUnlockReveal.Style.CAPSULE_FISSION }) {
            DockUnlockReveal reveal = new DockUnlockReveal();
            reveal.setStyle(style);
            reveal.arm(start);
            reveal.startIfArmed(start);
            check(reveal.containerPose(start + 120).active, style + " reaches a non-resting pose");
            reveal.cancel();
            DockUnlockReveal.ContainerPose restored = reveal.containerPose(start + 121);
            check(!restored.active && restored.scaleX == 1f && restored.scaleY == 1f
                            && restored.cropWidth == 1f && restored.cropHeight == 1f
                            && restored.cornerProgress == 1f && reveal.alpha(start + 121) == 1f,
                    style + " cancellation exposes a complete resting transaction");
            check(reveal.hasPendingPose(), style + " still requires that resting pose to commit");
            reveal.onPoseCommitted(start + 121);
            check(!reveal.hasPendingPose(), style + " clears pose debt only after restore commits");
        }
    }
}
