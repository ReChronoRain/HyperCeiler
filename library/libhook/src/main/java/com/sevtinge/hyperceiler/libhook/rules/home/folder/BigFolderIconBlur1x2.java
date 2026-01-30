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
package com.sevtinge.hyperceiler.libhook.rules.home.folder;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.BlurUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class BigFolderIconBlur1x2 extends BaseHook {

    Class<?> mFolderIcon1x2;

    boolean isShowEditPanel;
    Class<?> mLauncher;
    Class<?> mFolderInfo;
    Class<?> mFolderIcon;
    Class<?> mLauncherState;
    Class<?> mDragView;

    @Override
    public void init() {
        if (isPad()) {
            mFolderIcon1x2 = findClassIfExists("com.miui.home.launcher.folder.FolderIcon2x2_4");
        } else {
            mFolderIcon1x2 = findClassIfExists("com.miui.home.launcher.folder.FolderIcon1x2");
        }

        mLauncher = findClassIfExists("com.miui.home.launcher.Launcher");
        mFolderInfo = findClassIfExists("com.miui.home.launcher.FolderInfo");
        mFolderIcon = findClassIfExists("com.miui.home.launcher.FolderIcon");
        mLauncherState = findClassIfExists("com.miui.home.launcher.LauncherState");
        mDragView = findClassIfExists("com.miui.home.launcher.DragView");

        IMethodHook mBigFolderIconBlur = new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                int mFolderWidth = DisplayUtils.dp2px(mPrefsMap.getInt("home_big_folder_icon_bg_width_1x2", 62));
                int mFolderHeight = DisplayUtils.dp2px(mPrefsMap.getInt("home_big_folder_icon_bg_height_1x2", 145));
                ImageView mIconImageView = (ImageView) getObjectField(param.getThisObject(), "mIconImageView");
                FrameLayout mIconContainer = (FrameLayout) mIconImageView.getParent();
                FrameLayout mDockBlur = (FrameLayout) EzxHelpUtils.getAdditionalInstanceField(param.getThisObject(), "mDockBlur");
                FrameLayout view = new FrameLayout(mIconImageView.getContext());

                mIconImageView.setVisibility(View.GONE);
                mDockBlur.addView(view);
                new BlurUtils(mDockBlur, "home_big_folder_icon_bg_1x2_custom");

                mIconContainer.addView(mDockBlur, 0);
                FrameLayout.LayoutParams lp1 = (FrameLayout.LayoutParams) mDockBlur.getLayoutParams();
                lp1.gravity = Gravity.CENTER;
                lp1.height = mFolderHeight;
                lp1.width = mFolderWidth;
                findAndHookMethod(mLauncher, "showEditPanel", boolean.class, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        isShowEditPanel = (boolean) param.getArgs()[0];
                        if (isShowEditPanel) {
                            mDockBlur.setVisibility(View.GONE);
                            mIconImageView.setVisibility(View.VISIBLE);
                        } else {
                            mDockBlur.setVisibility(View.VISIBLE);
                            mIconImageView.setVisibility(View.GONE);
                        }
                    }
                });

                findAndHookMethod(mLauncher, "openFolder", mFolderInfo, View.class, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        mDockBlur.setVisibility(View.GONE);
                    }
                });

                findAndHookMethod(mLauncher, "closeFolder", boolean.class, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        if (!isShowEditPanel) mDockBlur.setVisibility(View.VISIBLE);
                    }
                });

                findAndHookMethod(mLauncher, "onStateSetStart", mLauncherState, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        Boolean mLauncherState = param.getArgs()[0].getClass().getSimpleName().equals("LauncherState");
                        Boolean mNormalState = param.getArgs()[0].getClass().getSimpleName().equals("NormalState");

                        if (mLauncherState || mNormalState) {
                            mDockBlur.setVisibility(View.VISIBLE);
                        } else {
                            mDockBlur.setVisibility(View.GONE);
                        }
                    }
                });
            }
        };

        findAndHookMethod(mFolderIcon1x2, "onFinishInflate", mBigFolderIconBlur);
    }
}
