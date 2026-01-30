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
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;

import android.content.Context;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class DisableTransparent extends BaseHook {
    @Override
    public void init() {
        // from https://www.coolapk.com/feed/52893204?shareKey=YTA3MTRkZGJmYTJmNjVlNmI4MTY~&shareUid=1499664&shareFrom=com.coolapk.app_5.3
        if (isMoreAndroidVersion(36)) {
            findAndHookMethod("com.android.systemui.statusbar.notification.row.NotificationRowContentBinderInjectorImpl", "isTransparent", Context.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(false);
                }
            });
        } else {
            findAndHookMethod("com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector", "isTransparent", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(false);
                }
            });
        }

    }
}
