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

package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import static de.robv.android.xposed.XposedHelpers.getStaticBooleanField;
import static de.robv.android.xposed.XposedHelpers.setStaticBooleanField;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class RemoveNotifNumLimit extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.systemui.statusbar.notification.collection.coordinator.CountLimitCoordinator", "attach", "com.android.systemui.statusbar.notification.collection.NotifPipeline", new replaceHookedMethod() {
            @Override
            protected Object replace(MethodHookParam param) throws Throwable {
                return null;
            }
        });
        findAndHookMethod("com.android.systemui.statusbar.notification.collection.coordinator.CountLimitCoordinator$$ExternalSyntheticLambda0", "onViewBound", "com.android.systemui.statusbar.notification.collection.NotificationEntry", new replaceHookedMethod() {
            @Override
            protected Object replace(MethodHookParam param) throws Throwable {
                return null;
            }
        });
    }
}
