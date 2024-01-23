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

public class SmallFolderIconBlur extends BaseHook {

    boolean isShowEditPanel;
    Class<?> mLauncher;
    Class<?> mFolderInfo;
    Class<?> mFolderIcon;
    Class<?> mFolderIcon1x1;
    Class<?> mLauncherState;
    Class<?> mDragView;

    int mFolderIconSize;
    ImageView mIconImageView;

    @Override
    public void init() {

        mLauncher = findClassIfExists("com.miui.home.launcher.Launcher");
        mFolderInfo = findClassIfExists("com.miui.home.launcher.FolderInfo");
        mFolderIcon = findClassIfExists("com.miui.home.launcher.FolderIcon");
        mFolderIcon1x1 = findClassIfExists("com.miui.home.launcher.folder.FolderIcon1x1");
        mLauncherState = findClassIfExists("com.miui.home.launcher.LauncherState");
        mDragView = findClassIfExists("com.miui.home.launcher.DragView");

        hookAllConstructors(mFolderIcon, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                try {
                    mIconImageView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mImageView");
                } catch (Exception e) {
                    mIconImageView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mIconImageView");
                }

                FrameLayout mDockBlur = (FrameLayout) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mDockBlur");
                if (mDockBlur != null) return;
                mDockBlur = new FrameLayout(mContext);
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "mDockBlur", mDockBlur);
            }
        });

        MethodHook mDockBlur = new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                try {
                    mIconImageView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mImageView");
                } catch (Exception e) {
                    mIconImageView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mIconImageView");
                }

                Context mContext = mIconImageView.getContext();
                mFolderIconSize = DisplayUtils.dip2px(mContext, mPrefsMap.getInt("home_small_folder_icon_bg_size", 56));

                FrameLayout mIconContainer = (FrameLayout) mIconImageView.getParent();
                FrameLayout mDockBlur = (FrameLayout) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mDockBlur");
                FrameLayout view = new FrameLayout(mContext);

                mIconImageView.setVisibility(View.GONE);
                mDockBlur.addView(view);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    new BlurUtils(mDockBlur, "home_small_folder_icon_bg_custom");
                }
                mIconContainer.addView(mDockBlur, 0);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mDockBlur.getLayoutParams();
                lp.gravity = Gravity.CENTER;
                lp.height = mFolderIconSize;
                lp.width = mFolderIconSize;

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


        try {
            findAndHookMethod(mFolderIcon1x1, "onFinishInflate", mDockBlur);
        } catch (Exception e) {
            findAndHookMethod(mFolderIcon, "onFinishInflate", mDockBlur);
        }

        /*hookAllMethods(mDragView, "showWithAnim", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                View dragView = (View) param.thisObject;
                dragView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                Object mDragInfo = XposedHelpers.getObjectField(param.thisObject, "mDragInfo");
            }
        });*/
    }
}
