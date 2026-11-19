/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

public final class StateTest {
    private static int checks;
    private static void check(boolean value, String description) {
        checks++;
        if (!value)
            throw new AssertionError(description);
    }
    private static boolean sample(BackHoldState s, int action, float x, float y, long time) {
        return s.update(1, action, 1, x, y, time, 3, 1200, 7);
    }
    public static void main(String[] args) {
        BackHoldState s = new BackHoldState();
        sample(s, 0, 4, 1000, 1000);
        sample(s, 2, 180, 1000, 1150);
        check(!s.ready(1449), "hold cannot arm early");
        check(s.ready(1450), "hold timer arms without another input event");
        sample(s, 2, 184, 1004, 1460);
        check(s.ready(1460), "finger jitter retains readiness");
        sample(s, 2, 110, 1000, 1500);
        check(s.geometry(true), "armed threshold has hysteresis");
        sample(s, 2, 70, 1000, 1600);
        check(!s.geometry(true), "pulling back cancels readiness");
        sample(s, 0, 1196, 1000, 2000);
        sample(s, 2, 1000, 1100, 2200);
        check(s.ready(2500) && !s.left(), "right-side diagonal accepted when horizontal dominates");
        sample(s, 2, 1000, 1250, 2600);
        check(!s.geometry(true), "vertical drag cannot commit");
        sample(s, 0, 4, 1000, 3000);
        sample(s, 2, 340, 1000, 3250);
        sample(s, 1, 340, 1000, 3270);
        check(!s.ready(3270), "fast swipe remains ordinary back");
        sample(s, 0, 4, 1000, 4000);
        sample(s, 2, 340, 1000, 4200);
        check(!s.update(1, 2, 2, 340, 1000, 4300, 3, 1200, 7), "second finger cancels");
        check(!s.ready(4600), "cancelled gesture cannot re-arm");
        sample(s, 0, 4, 1000, 5000);
        sample(s, 2, 340, 1000, 5200);
        check(!s.update(2, 2, 1, 340, 1000, 5300, 3, 1200, 7), "receiver change cancels");
        sample(s, 0, 4, 1000, 6000);
        sample(s, 2, 340, 1000, 6200);
        check(!s.update(1, 2, 1, 340, 1000, 6300, 3, 1200, 8), "config change cancels");
        sample(s, 0, 4, 1000, 7000);
        check(!sample(s, 3, 300, 1000, 7400), "system cancellation handled");
        sample(s, 0, 4, 1000, 8000);
        check(!sample(s, 2, Float.NaN, 1000, 8100), "invalid input rejected");
        sample(s, 0, 4, 1000, 9000);
        long old = s.serial;
        sample(s, 0, 4, 1000, 10000);
        check(s.serial != old, "new gesture invalidates asynchronous target result");
        check(!sample(s, 2, 300, 1000, 9999), "nonmonotonic time rejected");
        System.out.println("Back feedback state: " + checks + " checks passed");
    }
}
