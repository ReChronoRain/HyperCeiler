package com.sevtinge.hyperceiler.utils.log;

import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.mPrefsMap;

public class LogManager {
    public static int logLevel = mPrefsMap.getStringAsInt("log_level", 2);
    public static String logLevelDesc(){
        return switch (logLevel) {
            case 0 -> ("Disable");
            case 1 -> ("Error");
            case 2 -> ("Warn");
            case 3 -> ("Info");
            case 4 -> ("Debug");
            default -> ("Unknown");
        };
    }
}
