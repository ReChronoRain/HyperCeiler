/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.utils.log;

import static com.sevtinge.hyperceiler.module.base.tool.HookTool.mPrefsMap;

import com.sevtinge.hyperceiler.BuildConfig;

public class LogManager {
    public static final int logLevel = getLogLevel();

    public static int getLogLevel() {
        int level = mPrefsMap.getStringAsInt("log_level", 2);
        if (BuildConfig.BUILD_TYPE.equals("canary")) {
            return level == 0 ? 3 : 4;
        }
        return level;
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
