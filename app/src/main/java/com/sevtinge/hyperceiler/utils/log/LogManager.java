package com.sevtinge.hyperceiler.utils.log;

import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.mPrefsMap;

import com.sevtinge.hyperceiler.BuildConfig;

public class LogManager {
    public static int logLevel = getLogLevel();

    public static int getLogLevel() {
        int level = mPrefsMap.getStringAsInt("log_level", 2);
        switch (BuildConfig.BUILD_TYPE) {
            case "canary" -> {
                return level == 0 ? 3 : 4;
            }
            /*case "debug" -> {
                return 4;
            }*/
            default -> {
                return level;
            }
        }
    }

    public static String logLevelDesc() {
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
