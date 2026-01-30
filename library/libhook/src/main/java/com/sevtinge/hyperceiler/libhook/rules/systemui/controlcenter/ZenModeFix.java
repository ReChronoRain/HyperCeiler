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
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter;

import android.app.NotificationManager;
import android.content.Context;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

// 典中典给小米擦屁股
public class ZenModeFix extends BaseHook {
    String NotificationLoadClass;

    @Override
    public void init() {
        NotificationLoadClass = "com.android.systemui.statusbar.notification.policy.MiuiAlertManager";

        hookAllMethods(NotificationLoadClass, "buzzBeepBlink", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    Context mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                    NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager.getCurrentInterruptionFilter() >= NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
                        param.setResult(null);
                    }
                }
            }
        );
    }
}
