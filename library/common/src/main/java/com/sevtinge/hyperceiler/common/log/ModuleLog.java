/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.common.log;

import android.util.Log;

/** Direct diagnostics retain their level and become identifiable across hooked processes. */
public final class ModuleLog {
    public static final String PREFIX = "HyperCeiler.Raw.";
    public static final int DEBUG = Log.DEBUG, INFO = Log.INFO, WARN = Log.WARN, ERROR = Log.ERROR;

    private ModuleLog() {}

    public static int println(int priority, String tag, String message) {
        String original = tag == null ? "Unknown" : tag;
        return Log.println(priority, original.startsWith("HyperCeiler") ? original : PREFIX + original, message);
    }

    private static int write(int priority, String tag, String message, Throwable error) {
        return println(priority, tag, (message == null ? "" : message)
            + (error == null ? "" : "\n" + Log.getStackTraceString(error)));
    }

    public static int d(String tag, String message) { return write(DEBUG, tag, message, null); }
    public static int d(String tag, String message, Throwable error) { return write(DEBUG, tag, message, error); }
    public static int i(String tag, String message) { return write(INFO, tag, message, null); }
    public static int i(String tag, String message, Throwable error) { return write(INFO, tag, message, error); }
    public static int w(String tag, String message) { return write(WARN, tag, message, null); }
    public static int w(String tag, String message, Throwable error) { return write(WARN, tag, message, error); }
    public static int w(String tag, Throwable error) { return write(WARN, tag, "", error); }
    public static int e(String tag, String message) { return write(ERROR, tag, message, null); }
    public static int e(String tag, String message, Throwable error) { return write(ERROR, tag, message, error); }
    public static String getStackTraceString(Throwable error) { return Log.getStackTraceString(error); }
}
