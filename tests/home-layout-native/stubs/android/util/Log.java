/* SPDX-License-Identifier: AGPL-3.0-or-later */
package android.util;

/**
 * Host stub for {@code android.util.Log}.
 *
 * The endpoint's diagnostic line is a side effect only. The host test exercises the access checks
 * and the value validation, so the stub simply discards the message instead of pulling in logd.
 */
public final class Log {
    private Log() { }
    public static final int DEBUG=3,INFO=4,WARN=5,ERROR=6;
    public static int println(int priority,String tag,String message){return 0;}
    public static String getStackTraceString(Throwable error){return error.toString();}

    public static int i(String tag, String message) {
        return 0;
    }

    public static int w(String tag, String message) {
        return 0;
    }
}
