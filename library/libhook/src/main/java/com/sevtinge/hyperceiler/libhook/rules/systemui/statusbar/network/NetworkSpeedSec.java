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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.network;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class NetworkSpeedSec extends BaseHook {

    private static final String NETWORK_SPEED_VIEW_CLASS = "com.android.systemui.statusbar.views.NetworkSpeedView";
    private static final String[] REMOVE_CHARS = {"/", "B", "s", "'", "วิ"};

    @Override
    public void init() {
        if (tryHookSingleParam()) return;
        if (tryHookDoubleParam()) return;
        XposedLog.e(TAG, getPackageName(), "Failed to hook NetworkSpeedView.setNetworkSpeed");
    }

    private boolean tryHookSingleParam() {
        try {
            findAndHookMethod(NETWORK_SPEED_VIEW_CLASS, "setNetworkSpeed", String.class,
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        param.getArgs()[0] = cleanNetworkSpeedText((String) param.getArgs()[0]);
                    }
                });
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean tryHookDoubleParam() {
        try {
            findAndHookMethod(NETWORK_SPEED_VIEW_CLASS, "setNetworkSpeed", String.class, String.class,
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        param.getArgs()[1] = cleanNetworkSpeedText((String) param.getArgs()[1]);
                    }
                });
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private String cleanNetworkSpeedText(String text) {
        if (text == null) return null;

        String result = text;
        for (String removeChar : REMOVE_CHARS) {
            result = result.replace(removeChar, "");
        }
        return result;
    }
}

