/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.systemui;

import android.app.NotificationManager;
import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

// 典中典给小米擦屁股
public class ZenModeFix extends BaseHook {
    String NotificationLoadClass;

    @Override
    public void init() {
        NotificationLoadClass = "com.android.systemui.statusbar.notification.policy.MiuiAlertManager";

        hookAllMethods(NotificationLoadClass, lpparam.classLoader, "buzzBeepBlink", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager.getCurrentInterruptionFilter() >= NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
                        param.setResult(null);
                    }
                }
            }
        );
    }
}
