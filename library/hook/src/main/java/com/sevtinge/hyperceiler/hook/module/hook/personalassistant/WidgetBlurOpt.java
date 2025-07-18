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

package com.sevtinge.hyperceiler.hook.module.hook.personalassistant;

import com.hchen.hooktool.utils.ResInjectTool;
import com.sevtinge.hyperceiler.hook.R;
import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

public class WidgetBlurOpt extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        ResInjectTool.setResReplacement("com.miui.personalassistant", "color", "pa_widget_blur_color_lab", R.color.pa_widget_blur_color_lab);
        ResInjectTool.setResReplacement("com.miui.personalassistant", "color", "pa_widget_blur_color_linear_light", R.color.pa_widget_blur_color_linear_light);
        ResInjectTool.setResReplacement("com.miui.personalassistant", "color", "pa_widget_blur_color_src_over", R.color.pa_widget_blur_color_src_over);
    }
}
