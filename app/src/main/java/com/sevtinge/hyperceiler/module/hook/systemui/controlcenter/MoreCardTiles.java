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

public class MoreCardTiles extends BaseHook {

    @Override
    public void init() throws NoSuchMethodException {
        if (mPrefsMap.getStringAsInt("system_ui_control_center_more_card_tiles", 0) == 1) {
            mResHook.setResReplacement("miui.systemui.plugin", "array", "card_style_tiles_mobile", R.array.card_style_tiles_less);
        } else if (mPrefsMap.getStringAsInt("system_ui_control_center_more_card_tiles", 0) == 2) {
            mResHook.setResReplacement("miui.systemui.plugin", "array", "card_style_tiles_mobile", R.array.card_style_tiles_more);
        } else if (mPrefsMap.getStringAsInt("system_ui_control_center_more_card_tiles", 0) == 3) {
            mResHook.setResReplacement("miui.systemui.plugin", "array", "card_style_tiles_mobile", R.array.card_style_tiles_most);
        }
    }
}
