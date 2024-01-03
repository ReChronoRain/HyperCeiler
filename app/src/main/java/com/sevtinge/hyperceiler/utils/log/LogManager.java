package com.sevtinge.hyperceiler.utils.log;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

public class LogManager {
    public static int logLevel = getLogLevel();

    public static int getLogLevel() {
        int level = Integer.parseInt(PrefsUtils.mSharedPreferences.getString("prefs_key_log_level", "2"));
        switch (BuildConfig.BUILD_TYPE) {
            case "canary", "debug" -> {
                return 4;
            }
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
