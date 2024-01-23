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
package com.sevtinge.hyperceiler.module.hook.home.folder;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;

import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.BlurUtils;
import com.sevtinge.hyperceiler.utils.DisplayUtils;

import de.robv.android.xposed.XposedHelpers;

public class BigFolderIconBlur2x1 extends BaseHook {

    Class<?> mFolderIcon2x1;

    boolean isShowEditPanel;
    Class<?> mLauncher;
    Class<?> mFolderInfo;
    Class<?> mFolderIcon;
    Class<?> mLauncherState;
    Class<?> mDragView;

    @Override
    public void init() {
        if (isPad()) {
            mFolderIcon2x1 = findClassIfExists("com.miui.home.launcher.folder.FolderIcon4x2_8");
        } else {
            mFolderIcon2x1 = findClassIfExists("com.miui.home.launcher.folder.FolderIcon2x1");
        }

        mLauncher = findClassIfExists("com.miui.home.launcher.Launcher");
        mFolderInfo = findClassIfExists("com.miui.home.launcher.FolderInfo");
        mFolderIcon = findClassIfExists("com.miui.home.launcher.FolderIcon");
        mLauncherState = findClassIfExists("com.miui.home.launcher.LauncherState");
        mDragView = findClassIfExists("com.miui.home.launcher.DragView");

        MethodHook mBigFolderIconBlur = new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                int mFolderWidth = DisplayUtils.dip2px(mContext, mPrefsMap.getInt("home_big_folder_icon_bg_width_2x1", 145));
                int mFolderHeight = DisplayUtils.dip2px(mContext, mPrefsMap.getInt("home_big_folder_icon_bg_height_2x1", 62));
                ImageView mIconImageView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mIconImageView");
                FrameLayout mIconContainer = (FrameLayout) mIconImageView.getParent();
                FrameLayout mDockBlur = (FrameLayout) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mDockBlur");
                FrameLayout view = new FrameLayout(mIconImageView.getContext());

                mIconImageView.setVisibility(View.GONE);
                mDockBlur.addView(view);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    new BlurUtils(mDockBlur, "home_big_folder_icon_bg_2x1_custom");
                }

                mIconContainer.addView(mDockBlur, 0);
                FrameLayout.LayoutParams lp1 = (FrameLayout.LayoutParams) mDockBlur.getLayoutParams();
                lp1.gravity = Gravity.CENTER;
                if (isPad()) {
                    lp1.width = mFolderWidth * 2;
                    lp1.height = mFolderHeight * 2;
                }else {
                    lp1.width = mFolderWidth;
                    lp1.height = mFolderHeight;
                }
                findAndHookMethod(mLauncher, "showEditPanel", boolean.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        isShowEditPanel = (boolean) param.args[0];
                        if (isShowEditPanel) {
                            mDockBlur.setVisibility(View.GONE);
                            mIconImageView.setVisibility(View.VISIBLE);
                        } else {
                            mDockBlur.setVisibility(View.VISIBLE);
                            mIconImageView.setVisibility(View.GONE);
                        }
                    }
                });

                findAndHookMethod(mLauncher, "openFolder", mFolderInfo, View.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        mDockBlur.setVisibility(View.GONE);
                    }
                });

                findAndHookMethod(mLauncher, "closeFolder", boolean.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        if (!isShowEditPanel) mDockBlur.setVisibility(View.VISIBLE);
                    }
                });

                findAndHookMethod(mLauncher, "onStateSetStart", mLauncherState, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        Boolean mLauncherState = param.args[0].getClass().getSimpleName().equals("LauncherState");
                        Boolean mNormalState = param.args[0].getClass().getSimpleName().equals("NormalState");

                        if (mLauncherState || mNormalState) {
                            mDockBlur.setVisibility(View.VISIBLE);
                        } else {
                            mDockBlur.setVisibility(View.GONE);
                        }
                    }
                });
            }
        };

        findAndHookMethod(mFolderIcon2x1, "onFinishInflate", mBigFolderIconBlur);
    }
}
