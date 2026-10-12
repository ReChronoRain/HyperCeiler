package com.sevtinge.hyperceiler.common.log;

import java.util.ArrayList;
import java.util.List;

public final class XposedLog {
    public static final List<String> messages = new ArrayList<>();

    private XposedLog() {
    }

    public static void d(String tag, String pkg, String msg) {
        messages.add(msg);
    }

    public static void w(String tag, String pkg, String msg) {
        messages.add(msg);
    }

    public static void w(String tag, String pkg, String msg, Throwable t) {
        messages.add(msg);
    }

    public static boolean contains(String marker) {
        for (String message : messages) {
            if (message.contains(marker)) return true;
        }
        return false;
    }

    /** 返回最后一次 finished=<值> 的 CALLBACK_HIT 日志行，供断言可见性。 */
    public static String lastCallback(boolean finished) {
        String marker = "HOOK_STATE=CALLBACK_HIT finished=" + finished;
        String last = null;
        for (String message : messages) {
            if (message.startsWith(marker)) last = message;
        }
        return last;
    }
}
