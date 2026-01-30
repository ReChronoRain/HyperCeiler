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
package com.sevtinge.hyperceiler.libhook.rules.systemui.other;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class NotificationFreeform extends BaseHook {
    @Override
    public void init() {
        if (isMoreAndroidVersion(36)) {
            findAndHookMethod("com.android.systemui.statusbar.notification.row.ExpandableNotificationRowInjector", "updateMiniWindowBar", new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    setObjectField(param.getThisObject(), "canSlide", true);
                }
            });
        } else {
            findAndHookMethod("com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow", "updateMiniWindowBar", new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    setObjectField(param.getThisObject(), "mCanSlide", true);
                }
            });
        }

    }
}
