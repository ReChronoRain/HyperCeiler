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
package com.sevtinge.hyperceiler.hook.module.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.hook.module.base.BaseModule;
import com.sevtinge.hyperceiler.hook.module.rules.notes.DisableAiWatermark;
import com.sevtinge.hyperceiler.hook.module.rules.notes.UnlockAI;

@HookBase(targetPackage = "com.miui.notes")
public class Notes extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DisableAiWatermark(), mPrefsMap.getBoolean("notes_disable_ai_watermark"));
        initHook(UnlockAI.INSTANCE, mPrefsMap.getStringAsInt("notes_unlock_ai_mode", 0) != 0);
    }
}
