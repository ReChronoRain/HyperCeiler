package com.sevtinge.hyperceiler.common.log;

/** Host stub: the endpoint's one-line-per-outcome diagnostics are not asserted on host. */
public final class ModuleLog {
    private ModuleLog() {}

    public static void i(String tag, String message) {
        // no-op: host runs keep the console clean and assert nothing about logging
    }

    public static void w(String tag, String message) {
        // no-op: see above
    }

    public static void e(String tag, String message) {
        // no-op: see above
    }

    public static void e(String tag, String message, Throwable throwable) {
        // no-op: see above
    }
}
