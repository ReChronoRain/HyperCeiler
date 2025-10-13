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
package com.sevtinge.hyperceiler.hook.module.hook.systemframework;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.util.ArraySet;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

public class PstedClipboard extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {

        if (isMoreAndroidVersion(36)) {
            findAndHookMethod("com.android.server.clipboard.ClipboardService",
                "lambda$showAccessNotificationLocked$5",
                String.class, int.class, ArraySet.class, int.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(null);
                    }
                }
            );
        } else {
            findAndHookMethod("com.android.server.clipboard.ClipboardService",
                "lambda$showAccessNotificationLocked$4",
                String.class, int.class, ArraySet.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        param.setResult(null);
                    }
                }
            );
        }
    }
}
