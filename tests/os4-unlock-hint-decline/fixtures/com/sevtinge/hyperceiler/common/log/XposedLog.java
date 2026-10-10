package com.sevtinge.hyperceiler.common.log;
import java.util.ArrayList;
import java.util.List;
public class XposedLog {
    public static final List<String> warnings = new ArrayList<>();
    public static void w(String tag, String message) { warnings.add(tag + ": " + message); }
}
