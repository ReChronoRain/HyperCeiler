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
package com.sevtinge.hyperceiler.module.hook.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class UnlockCustomActions extends BaseHook {

    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod(isMoreAndroidVersion(35) ? "com.android.systemui.media.controls.domain.pipeline.LegacyMediaDataManagerImpl$createActionsFromState$customActions$1" : "com.android.systemui.media.controls.pipeline.MediaDataManager$createActionsFromState$customActions$1",
                "invoke", Object.class
                , new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        Object INSTANCE = XposedHelpers.getStaticObjectField(
                                findClassIfExists(isMoreAndroidVersion(35) ? "com.android.systemui.statusbar.notification.NotificationSettingsManager" : "com.android.systemui.statusbar.notification.NotificationSettingsManager$Holder"),
                                isMoreAndroidVersion(35) ? "sINSTANCE" : "INSTANCE");
                        XposedHelpers.setObjectField(INSTANCE, "mHiddenCustomActionsList", new ArrayList<>());
                    }

                }
        );
    }
}
