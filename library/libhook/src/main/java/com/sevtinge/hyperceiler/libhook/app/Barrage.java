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
import com.sevtinge.hyperceiler.libhook.rules.barrage.AnyBarrage;
import com.sevtinge.hyperceiler.libhook.rules.barrage.BarrageNotTouchable;
import com.sevtinge.hyperceiler.libhook.rules.barrage.CustomBarrageLength;
import com.sevtinge.hyperceiler.libhook.rules.barrage.GlobalBarrage;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

@HookBase(targetPackage = "com.xiaomi.barrage")
public class Barrage extends BaseLoad {

    @Override
    public void onPackageLoaded() {
        initHook(AnyBarrage.INSTANCE, PrefsBridge.getBoolean("barrage_any_barrage"));
        initHook(BarrageNotTouchable.INSTANCE, PrefsBridge.getBoolean("barrage_not_touchable"));
        initHook(GlobalBarrage.INSTANCE, PrefsBridge.getBoolean("barrage_global_enable"));
        initHook(CustomBarrageLength.INSTANCE, PrefsBridge.getInt("barrage_custom_barrage_length", 36) != 36);
    }
}
