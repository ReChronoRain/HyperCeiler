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
package com.sevtinge.hyperceiler.libhook.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.personalassistant.DisableLiteVersion;
import com.sevtinge.hyperceiler.libhook.rules.personalassistant.UnlockWidgetCountLimit;
import com.sevtinge.hyperceiler.libhook.rules.personalassistant.WidgetBlurOpt;

@HookBase(targetPackage = "com.miui.personalassistant")
public class PersonalAssistant extends BaseLoad {

    public PersonalAssistant() {
        super(true);
    }

    @Override
    public void onPackageLoaded() {
        // initHook(new BlurOverlay(), false);
        initHook(new DisableLiteVersion(), mPrefsMap.getBoolean("personal_assistant_disable_lite_version"));
        initHook(new UnlockWidgetCountLimit(), mPrefsMap.getBoolean("personal_assistant_unlock_widget_count_limit"));

        initHook(new WidgetBlurOpt(), mPrefsMap.getBoolean("personal_assistant_widget_widget_blur_opt"));
    }

}
