/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import android.os.SystemClock;
import android.view.InputEvent;
import android.view.MotionEvent;
import java.lang.reflect.Method;

public final class TouchProbe {
    private static Object manager;
    private static Method inject;
    private static long down;

    private static String argument(String[] args, int index, String fallback) {
        return args.length > index ? args[index] : fallback;
    }

    private static float move(float start, float end, int samples, long delay) throws Exception {
        for (int i = 1; i <= samples; i++) {
            event(2, start + (end - start) * i / samples, 1250);
            SystemClock.sleep(delay);
        }
        return end;
    }

    private static float retreat(float deepest, float distance, boolean right, boolean rearm) throws Exception {
        float withdrawn = move(deepest, deepest + (right ? distance : -distance), 6, 20);
        SystemClock.sleep(450);
        if (!rearm) {
            return withdrawn;
        }
        move(withdrawn, deepest, 6, 20);
        SystemClock.sleep(650);
        return deepest;
    }

    private static void event(int action, float x, float y) throws Exception {
        MotionEvent event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0);
        event.setSource(4098);
        try {
            if (!Boolean.TRUE.equals(inject.invoke(manager, event, 2)))
                throw new IllegalStateException("Input injection rejected");
        } finally {
            event.recycle();
        }
    }
    public static void main(String[] args) throws Exception {
        boolean right = argument(args, 0, "left").equals("right");
        long hold = Long.parseLong(argument(args, 1, "1000"));
        String finish = argument(args, 2, "release");
        long step = Long.parseLong(argument(args, 3, "20"));
        float distance = Float.parseFloat(argument(args, 4, "312"));
        float retreatDistance = Float.parseFloat(argument(args, 5, Float.toString(distance - 26)));
        Class<?> type = Class.forName("android.hardware.input.InputManagerGlobal");
        manager = type.getMethod("getInstance").invoke(null);
        inject = type.getMethod("injectInputEvent", InputEvent.class, int.class);
        down = SystemClock.uptimeMillis();
        float x = right ? 1196 : 4;
        event(0, x, 1250);
        x = move(x, x + (right ? -distance : distance), 12, step);
        System.out.println("Holding at x=" + x);
        SystemClock.sleep(hold);
        if (finish.equals("retract") || finish.equals("rearm")) {
            x = retreat(x, retreatDistance, right, finish.equals("rearm"));
        }
        event(finish.equals("cancel") ? 3 : 1, x, 1250);
        System.out.println("Finished: " + finish);
    }
}
