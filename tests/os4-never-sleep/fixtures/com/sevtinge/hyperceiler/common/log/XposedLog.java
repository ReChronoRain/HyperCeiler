package com.sevtinge.hyperceiler.common.log;
public class XposedLog {
 public static int errors;
 public static void e(String tag,String pkg,String message,Throwable error){ errors++; }
}
