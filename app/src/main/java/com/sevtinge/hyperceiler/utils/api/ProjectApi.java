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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.utils.api;

import com.sevtinge.hyperceiler.BuildConfig;

public class ProjectApi {
    public static final String mAppModulePkg = BuildConfig.APPLICATION_ID;

    public static String getBuildType() {
        switch (BuildConfig.BUILD_TYPE) {
            case "release" -> {
                return "release";
            }
            case "beta" -> {
                return "beta";
            }
            case "canary" -> {
                return "canary";
            }
            case "debug" -> {
                return "debug";
            }
            default -> {
                return "unknown";
            }
        }
    }

    public static boolean isRelease() {
        return "release".equals(BuildConfig.BUILD_TYPE);
    }

    public static boolean isBeta() {
        return "beta".equals(BuildConfig.BUILD_TYPE);
    }

    public static boolean isCanary() {
        return "canary".equals(BuildConfig.BUILD_TYPE);
    }

    public static boolean isDebug() {
        return "debug".equals(BuildConfig.BUILD_TYPE);
    }
}
