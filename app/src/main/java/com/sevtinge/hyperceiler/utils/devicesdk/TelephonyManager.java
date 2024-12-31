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
package com.sevtinge.hyperceiler.utils.devicesdk;

import com.sevtinge.hyperceiler.utils.InvokeUtils;

public class TelephonyManager {
    Object telephonyManager;
    String name = "miui.telephony.TelephonyManager";

    public TelephonyManager() {
        telephonyManager = InvokeUtils.callStaticMethod(name, "getDefault", new Class[]{});
    }

    public static TelephonyManager getDefault() {
        return new TelephonyManager();
    }

    public void setUserFiveGEnabled(boolean enabled) {
        InvokeUtils.callMethod(name, telephonyManager, "setUserFiveGEnabled", new Class[]{boolean.class}, enabled);
    }

    public boolean isUserFiveGEnabled() {
        return InvokeUtils.callMethod(name, telephonyManager, "isUserFiveGEnabled", new Class[]{});
    }

    public boolean isFiveGCapable() {
        return InvokeUtils.callMethod(name, telephonyManager, "isFiveGCapable", new Class[]{});
    }
}
