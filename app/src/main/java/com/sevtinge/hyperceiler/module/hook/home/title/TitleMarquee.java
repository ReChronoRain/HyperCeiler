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

import android.text.TextUtils;
import android.widget.TextView;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class TitleMarquee extends BaseHook {
    TextView mTitle;

    @Override
    public void init() {
        Class<?> mItemIcon = findClassIfExists("com.miui.home.launcher.ItemIcon");

        findAndHookMethod(mItemIcon, "onFinishInflate", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                try {
                    mTitle = (TextView) XposedHelpers.getObjectField(param.thisObject, "mTitleView");
                } catch (Throwable t) {
                    mTitle = (TextView) XposedHelpers.getObjectField(param.thisObject, "mTitle");
                }

                mTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                mTitle.setHorizontalFadingEdgeEnabled(true);
                mTitle.setSingleLine();
                mTitle.setMarqueeRepeatLimit(-1);
                mTitle.setSelected(true);
                mTitle.setHorizontallyScrolling(true);
            }
        });
    }
}
