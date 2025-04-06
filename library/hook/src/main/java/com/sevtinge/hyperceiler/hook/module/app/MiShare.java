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
package com.sevtinge.hyperceiler.hook.module.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.hook.module.base.BaseModule;
import com.sevtinge.hyperceiler.hook.module.hook.mishare.NoAutoTurnOff;
import com.sevtinge.hyperceiler.hook.module.hook.mishare.NoAutoTurnOnLocation;
import com.sevtinge.hyperceiler.hook.module.hook.mishare.UnlockTurboMode;

@HookBase(targetPackage = "com.miui.mishare.connectivity")
public class MiShare extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(NoAutoTurnOff.INSTANCE, mPrefsMap.getBoolean("disable_mishare_auto_off")); // 禁用 10 分钟自动关闭
        initHook(NoAutoTurnOnLocation.INSTANCE, mPrefsMap.getBoolean("disable_mishare_auto_on_location")); // 禁用分享时自动开启位置信息
        initHook(UnlockTurboMode.INSTANCE, mPrefsMap.getBoolean("unlock_turbo_mode")); // 解锁极速传输模式
    }
}
