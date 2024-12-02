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

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.notes.DisableAiWatermark;
import com.sevtinge.hyperceiler.module.hook.various.UnlockSuperClipboard;

@HookBase(targetPackage = "com.miui.notes",  isPad = false)
public class Notes extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(UnlockSuperClipboard.INSTANCE, mPrefsMap.getStringAsInt("various_super_clipboard_e", 0) != 0);
        initHook(new DisableAiWatermark(), mPrefsMap.getBoolean("notes_disable_ai_watermark"));
    }
}
