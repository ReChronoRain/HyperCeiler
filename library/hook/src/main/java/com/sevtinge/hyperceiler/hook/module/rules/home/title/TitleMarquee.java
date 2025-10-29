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
package com.sevtinge.hyperceiler.hook.module.rules.home.title;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;

import android.text.TextUtils;
import android.widget.TextView;

import com.sevtinge.hyperceiler.hook.module.base.pack.home.HomeBaseHookNew;

import de.robv.android.xposed.XposedHelpers;

public class TitleMarquee extends HomeBaseHookNew {
    TextView mTitle;
    TextView mTitleView;

    @Version(isPad = true, min = 450000000)
    private void initPadHook() {
        Class<?> mItemIcon = findClassIfExists("com.miui.home.launcher.ItemIcon");
        Class<?> mShortcutIcon = findClassIfExists("com.miui.home.launcher.ShortcutIcon");

        findAndHookMethod(mItemIcon, "onFinishInflate", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                try {
                    mTitle = (TextView) XposedHelpers.getObjectField(param.thisObject, "mTitleView");
                } catch (Throwable t) {
                    mTitle = (TextView) param.thisObject;
                }
                if (mTitle == null) return;

                mTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                mTitle.setHorizontalFadingEdgeEnabled(true);
                mTitle.setSingleLine();
                mTitle.setMarqueeRepeatLimit(-1);
                mTitle.setSelected(true);
                mTitle.setHorizontallyScrolling(true);
            }
        });

        if (mShortcutIcon != null) {
            findAndHookMethod(mShortcutIcon, "setTitle", CharSequence.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    mTitle = (TextView) param.thisObject;
                    if (mTitle == null) return;

                    mTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    mTitle.setHorizontalFadingEdgeEnabled(true);
                    mTitle.setSingleLine();
                    mTitle.setMarqueeRepeatLimit(-1);
                    mTitle.setHorizontallyScrolling(true);
                    mTitle.post(() -> mTitle.setSelected(true));
                }
            });
        }
    }

    @Override
    public void initBase() {
        if (isPad()) {
            initPadHook();
            return;
        }

        Class<?> mItemIcon = findClassIfExists("com.miui.home.ItemIcon");
        Class<?> mShortcutIcon = findClassIfExists("com.miui.home.launcher.ShortcutIcon");

        findAndHookMethod(mItemIcon, "onFinishInflate", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                mTitleView = (TextView) XposedHelpers.getObjectField(param.thisObject, "mTitleView");
                if (mTitleView == null) return;

                mTitleView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                mTitleView.setHorizontalFadingEdgeEnabled(true);
                mTitleView.setSingleLine();
                mTitleView.setMarqueeRepeatLimit(-1);
                mTitleView.setHorizontallyScrolling(true);
                mTitleView.post(() -> mTitleView.setSelected(true));
            }
        });

        if (mShortcutIcon != null) {
            findAndHookMethod(mShortcutIcon, "setTitle", CharSequence.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    mTitle = (TextView) param.thisObject;
                    if (mTitle == null) return;

                    mTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    mTitle.setHorizontalFadingEdgeEnabled(true);
                    mTitle.setSingleLine();
                    mTitle.setMarqueeRepeatLimit(-1);
                    mTitle.setHorizontallyScrolling(true);
                    mTitle.post(() -> mTitle.setSelected(true));
                }
            });
        }
    }
}
