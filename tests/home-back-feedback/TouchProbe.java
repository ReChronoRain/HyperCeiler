/* SPDX-License-Identifier: AGPL-3.0-or-later */
import android.os.SystemClock;
import android.view.InputEvent;
import android.view.MotionEvent;
import java.lang.reflect.Method;

public final class TouchProbe {
    private static Object manager;
    private static Method inject;
    private static long down;
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
        boolean right = args.length > 0 && args[0].equals("right");
        long hold = args.length > 1 ? Long.parseLong(args[1]) : 1000;
        String finish = args.length > 2 ? args[2] : "release";
        long step = args.length > 3 ? Long.parseLong(args[3]) : 20;
        float distance = args.length > 4 ? Float.parseFloat(args[4]) : 312;
        float retreat = args.length > 5 ? Float.parseFloat(args[5]) : distance - 26;
        Class<?> type = Class.forName("android.hardware.input.InputManagerGlobal");
        manager = type.getMethod("getInstance").invoke(null);
        inject = type.getMethod("injectInputEvent", InputEvent.class, int.class);
        down = SystemClock.uptimeMillis();
        float x = right ? 1196 : 4;
        event(0, x, 1250);
        for (int i = 1; i <= 12; i++) {
            x = right ? 1196 - i * distance / 12 : 4 + i * distance / 12;
            event(2, x, 1250);
            SystemClock.sleep(step);
        }
        System.out.println("Holding at x=" + x);
        SystemClock.sleep(hold);
        if (finish.equals("retract") || finish.equals("rearm")) {
            float deepest = x;
            for (int i = 1; i <= 6; i++) {
                x = deepest + (right ? 1 : -1) * retreat * i / 6;
                event(2, x, 1250);
                SystemClock.sleep(20);
            }
            SystemClock.sleep(450);
            if (finish.equals("rearm")) {
                float withdrawn = x;
                for (int i = 1; i <= 6; i++) {
                    x = withdrawn + (deepest - withdrawn) * i / 6;
                    event(2, x, 1250);
                    SystemClock.sleep(20);
                }
                SystemClock.sleep(650);
            }
        }
        event(finish.equals("cancel") ? 3 : 1, x, 1250);
        System.out.println("Finished: " + finish);
    }
}
