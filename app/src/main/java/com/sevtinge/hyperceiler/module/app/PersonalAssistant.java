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
package com.sevtinge.hyperceiler.module.app;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isHyperOSVersion;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.personalassistant.BlurPersonalAssistant;
import com.sevtinge.hyperceiler.module.hook.personalassistant.BlurPersonalAssistantBackGround;
import com.sevtinge.hyperceiler.module.hook.personalassistant.DisableLiteVersion;
import com.sevtinge.hyperceiler.module.hook.personalassistant.EnableFoldWidget;
import com.sevtinge.hyperceiler.module.hook.personalassistant.SetTravelNotificationStatusBarInfoMaxWidth;
import com.sevtinge.hyperceiler.module.hook.personalassistant.UnlockWidgetCountLimit;

@HookBase(targetPackage = "com.miui.personalassistant",  isPad = false)
public class PersonalAssistant extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // initHook(new BlurOverlay(), false);
        initHook(new DisableLiteVersion(), mPrefsMap.getBoolean("personal_assistant_disable_lite_version"));
        initHook(new EnableFoldWidget(), mPrefsMap.getBoolean("personal_assistant_fold_widget_enable"));
        initHook(new UnlockWidgetCountLimit(), mPrefsMap.getBoolean("personal_assistant_unlock_widget_count_limit"));

        if (mPrefsMap.getStringAsInt("personal_assistant_value", 0) == 2) {
            initHook(BlurPersonalAssistant.INSTANCE , true);
        } else if (mPrefsMap.getStringAsInt("personal_assistant_value", 0) == 1) {
            initHook(BlurPersonalAssistantBackGround.INSTANCE, true);
        }

        initHook(new SetTravelNotificationStatusBarInfoMaxWidth(), mPrefsMap.getInt("personal_assistant_set_tv_notif_info_max_width", 60) != 60 && isHyperOSVersion(1f));
    }

}
