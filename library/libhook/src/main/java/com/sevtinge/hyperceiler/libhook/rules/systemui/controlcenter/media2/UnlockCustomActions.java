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
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media2;


import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import java.util.ArrayList;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class UnlockCustomActions extends BaseHook {

    @Override
    public void init() {

        if (isMoreAndroidVersion(36)) {
            findAndHookMethod("com.android.systemui.statusbar.notification.mediacontrol.MediaActionsInjector$getCustomAction$1",
                "run", new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        Object INSTANCE = EzxHelpUtils.getStaticObjectField(
                            findClassIfExists("com.miui.systemui.notification.NotificationSettingsManager"),
                            "sINSTANCE");
                        EzxHelpUtils.setObjectField(INSTANCE, "mHiddenCustomActionsList", new ArrayList<>());
                        EzxHelpUtils.setObjectField(INSTANCE, "mHiddenCustomActionsListLocal", new ArrayList<>());
                    }

                }
            );
        } else {
            findAndHookMethod("com.android.systemui.media.controls.domain.pipeline.LegacyMediaDataManagerImpl$createActionsFromState$customActions$1",
                "invoke", Object.class, new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        Class<?> NotificationSettingsManager;
                        // k60u
                        NotificationSettingsManager = findClassIfExists("com.miui.systemui.notification.NotificationSettingsManager");
                        if (NotificationSettingsManager == null) {
                            // other
                            NotificationSettingsManager = findClassIfExists("com.android.systemui.statusbar.notification.NotificationSettingsManager");
                        }

                        Object INSTANCE = EzxHelpUtils.getStaticObjectField(
                            NotificationSettingsManager, "sINSTANCE"
                        );
                        EzxHelpUtils.setObjectField(INSTANCE, "mHiddenCustomActionsList", new ArrayList<>());
                    }

                }
            );
        }
    }
}
