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
package com.sevtinge.hyperceiler.libhook.rules.home.title;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;

import android.text.TextUtils;
import android.widget.TextView;

import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class TitleMarquee extends HomeBaseHookNew {
    TextView mTitle;
    TextView mTitleView;

    @Version(isPad = true, min = 450000000)
    private void initPadHook() {
        Class<?> mItemIcon = findClassIfExists("com.miui.home.launcher.ItemIcon");
        Class<?> mShortcutIcon = findClassIfExists("com.miui.home.launcher.ShortcutIcon");

        findAndHookMethod(mItemIcon, "onFinishInflate", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                try {
                    mTitle = (TextView) getObjectField(param.getThisObject(), "mTitleView");
                } catch (Throwable t) {
                    mTitle = (TextView) param.getThisObject();
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
            findAndHookMethod(mShortcutIcon, "setTitle", CharSequence.class, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    mTitle = (TextView) param.getThisObject();
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

        findAndHookMethod(mItemIcon, "onFinishInflate", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                mTitleView = (TextView) getObjectField(param.getThisObject(), "mTitleView");
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
            findAndHookMethod(mShortcutIcon, "setTitle", CharSequence.class, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    mTitle = (TextView) param.getThisObject();
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
