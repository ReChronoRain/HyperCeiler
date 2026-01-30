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

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.getAdditionalInstanceField;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.setAdditionalInstanceField;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.BlurUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

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

        hookAllConstructors(mFolderIcon, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                Context mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                try {
                    mIconImageView = (ImageView) getObjectField(param.getThisObject(), "mImageView");
                } catch (Exception e) {
                    mIconImageView = (ImageView) getObjectField(param.getThisObject(), "mIconImageView");
                }

                FrameLayout mDockBlur = (FrameLayout) getAdditionalInstanceField(param.getThisObject(), "mDockBlur");
                if (mDockBlur != null) return;
                mDockBlur = new FrameLayout(mContext);
                setAdditionalInstanceField(param.getThisObject(), "mDockBlur", mDockBlur);
            }
        });

        IMethodHook mDockBlur = new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                try {
                    mIconImageView = (ImageView) getObjectField(param.getThisObject(), "mImageView");
                } catch (Exception e) {
                    mIconImageView = (ImageView) getObjectField(param.getThisObject(), "mIconImageView");
                }

                Context mContext = mIconImageView.getContext();
                mFolderIconSize = DisplayUtils.dp2px(mPrefsMap.getInt("home_small_folder_icon_bg_size", 56));

                FrameLayout mIconContainer = (FrameLayout) mIconImageView.getParent();
                FrameLayout mDockBlur = (FrameLayout) getAdditionalInstanceField(param.getThisObject(), "mDockBlur");
                FrameLayout view = new FrameLayout(mContext);

                mIconImageView.setVisibility(View.GONE);
                mDockBlur.addView(view);
                new BlurUtils(mDockBlur, "home_small_folder_icon_bg_custom");
                mIconContainer.addView(mDockBlur, 0);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mDockBlur.getLayoutParams();
                lp.gravity = Gravity.CENTER;
                lp.height = mFolderIconSize;
                lp.width = mFolderIconSize;

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


        try {
            findAndHookMethod(mFolderIcon1x1, "onFinishInflate", mDockBlur);
        } catch (Exception e) {
            findAndHookMethod(mFolderIcon, "onFinishInflate", mDockBlur);
        }

        /*hookAllMethods(mDragView, "showWithAnim", new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                View dragView = (View) param.getThisObject();
                dragView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                Object mDragInfo = getObjectField(param.getThisObject(), "mDragInfo");
            }
        });*/
    }
}
