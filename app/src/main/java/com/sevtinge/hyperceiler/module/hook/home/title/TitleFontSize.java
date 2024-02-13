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
package com.sevtinge.hyperceiler.module.hook.home.title;

import android.annotation.SuppressLint;
import android.util.TypedValue;
import android.widget.TextView;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class TitleFontSize extends BaseHook {

    @Override
    public void init() {
        hookAllMethods("com.miui.home.launcher.common.Utilities", "adaptTitleStyleToWallpaper",
            new MethodHook() {
                @SuppressLint("DiscouragedApi")
                @Override
                protected void after(MethodHookParam param) {
                    TextView mTitle = (TextView) param.args[1];
                    if (mTitle != null && mTitle.getId() == mTitle.getResources().getIdentifier("icon_title", "id", "com.miui.home")) {
                        mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, mPrefsMap.getInt("home_title_font_size", 12));
                    }
                }
            }
        );
    }
}
