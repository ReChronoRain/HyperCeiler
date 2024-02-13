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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;

public class QQSGridOld extends BaseHook {
    @Override
    public void init() {
        int cols = mPrefsMap.getInt("system_control_center_old_qs_grid_column", 2);
        int colsResId = switch (cols) {
            case 3 -> R.integer.quick_quick_settings_num_rows_3;
            case 4 -> R.integer.quick_quick_settings_num_rows_4;
            case 5 -> R.integer.quick_quick_settings_num_rows_5;
            case 6 -> R.integer.quick_quick_settings_num_rows_6;
            case 7 -> R.integer.quick_quick_settings_num_rows_7;
            default -> R.integer.quick_quick_settings_num_rows_5;
        };
        mResHook.setResReplacement("com.android.systemui", "integer", "quick_settings_qqs_count", colsResId);
    }
}
