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

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.HookExpand;
import com.sevtinge.hyperceiler.module.hook.personalassistant.BlurPersonalAssistant;
import com.sevtinge.hyperceiler.module.hook.personalassistant.BlurPersonalAssistantBackGround;
import com.sevtinge.hyperceiler.module.hook.personalassistant.EnableFoldWidget;

@HookExpand(pkg = "com.miui.personalassistant", isPad = false, tarAndroid = 34)
public class PersonalAssistant extends BaseModule {

    @Override
    public void handleLoadPackage() {
        // initHook(new BlurOverlay(), false);
        initHook(new EnableFoldWidget(), mPrefsMap.getBoolean("personal_assistant_fold_widget_enable"));

        if (mPrefsMap.getStringAsInt("personal_assistant_value", 0) == 2 && !isAndroidVersion(30)) {
            initHook(BlurPersonalAssistant.INSTANCE);
        } else if (mPrefsMap.getStringAsInt("personal_assistant_value", 0) == 1 && !isAndroidVersion(30)) {
            initHook(BlurPersonalAssistantBackGround.INSTANCE);
        }
    }

}
