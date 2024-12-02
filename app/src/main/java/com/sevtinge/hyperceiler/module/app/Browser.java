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
import com.sevtinge.hyperceiler.module.hook.browser.DebugMode;
import com.sevtinge.hyperceiler.module.hook.browser.DisableReadFiles;
import com.sevtinge.hyperceiler.module.hook.browser.EnableDebugEnvironment;
import com.sevtinge.hyperceiler.module.hook.various.UnlockSuperClipboard;

@HookBase(targetPackage = "com.android.browser", isPad = false)
public class Browser extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new DebugMode(), mPrefsMap.getBoolean("browser_debug_mode"));
        initHook(new DisableReadFiles(), mPrefsMap.getBoolean("browser_disable_blacklist"));
        initHook(new EnableDebugEnvironment(), mPrefsMap.getBoolean("browser_enable_debug_environment"));
        initHook(UnlockSuperClipboard.INSTANCE, mPrefsMap.getStringAsInt("various_super_clipboard_e", 0) != 0);
    }
}
