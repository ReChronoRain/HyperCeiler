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

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class UnlockCustomActions extends BaseHook {

    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.systemui.media.controls.pipeline.MediaDataManager$createActionsFromState$customActions$1",
                "invoke", Object.class
                , new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        Object INSTANCE = XposedHelpers.getStaticObjectField(
                                findClassIfExists("com.android.systemui.statusbar.notification.NotificationSettingsManager$Holder"),
                                "INSTANCE");
                        XposedHelpers.setObjectField(INSTANCE, "mHiddenCustomActionsList", new ArrayList<>());
                    }

                }
        );
    }
}
