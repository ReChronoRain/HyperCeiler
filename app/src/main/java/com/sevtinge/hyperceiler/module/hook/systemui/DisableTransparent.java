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
package com.sevtinge.hyperceiler.module.hook.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DisableTransparent extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        // from https://www.coolapk.com/feed/52893204?shareKey=YTA3MTRkZGJmYTJmNjVlNmI4MTY~&shareUid=1499664&shareFrom=com.coolapk.app_5.3
        String methodName;
        if (isMoreHyperOSVersion(1f)) methodName = "isTransparent";
        else methodName = "isTransparentMode";
        findAndHookMethod("com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector", methodName, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }
}
