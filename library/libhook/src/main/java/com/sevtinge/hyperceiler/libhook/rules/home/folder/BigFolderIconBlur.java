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

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.BlurUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class BigFolderIconBlur extends BaseHook {

    boolean isShowEditPanel;
    Class<?> mLauncher;
    Class<?> mFolderInfo;
    Class<?> mFolderIcon;
    Class<?> mLauncherState;
    Class<?> mDragView;

    Class<?> mFolderIcon2x2;
    Class<?> mFolderIcon2x2_4;
    Class<?> mFolderIcon2x2_9;

    @Override
    public void init() {
        mLauncher = findClassIfExists("com.miui.home.launcher.Launcher");
        mFolderInfo = findClassIfExists("com.miui.home.launcher.FolderInfo");
        mFolderIcon = findClassIfExists("com.miui.home.launcher.FolderIcon");
        mLauncherState = findClassIfExists("com.miui.home.launcher.LauncherState");
        mDragView = findClassIfExists("com.miui.home.launcher.DragView");
        mFolderIcon2x2 = findClassIfExists("com.miui.home.launcher.folder.FolderIcon2x2");

        if (isPad()) {
            mFolderIcon2x2_4 = findClassIfExists("com.miui.home.launcher.folder.FolderIcon4x4_16");
            mFolderIcon2x2_9 = findClassIfExists("com.miui.home.launcher.folder.FolderIcon3x3_9");
        } else {
            mFolderIcon2x2_4 = findClassIfExists("com.miui.home.launcher.folder.FolderIcon2x2_4");
            mFolderIcon2x2_9 = findClassIfExists("com.miui.home.launcher.folder.FolderIcon2x2_9");
        }

        hookAllConstructors(mFolderIcon, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                Context mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                Object mDockBlur = EzxHelpUtils.getAdditionalInstanceField(param.getThisObject(), "mDockBlur");
                if (mDockBlur != null) return;
                mDockBlur = new FrameLayout(mContext);
                EzxHelpUtils.setAdditionalInstanceField(param.getThisObject(), "mDockBlur", mDockBlur);
            }
        });

        IMethodHook mBigFolderIconBlur = new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                int mFolderWidth = DisplayUtils.dp2px(PrefsBridge.getInt("home_big_folder_icon_bg_width", 145));
                int mFolderHeight = DisplayUtils.dp2px(PrefsBridge.getInt("home_big_folder_icon_bg_height", 145));
                ImageView mIconImageView = (ImageView) EzxHelpUtils.getObjectField(param.getThisObject(), "mIconImageView");
                FrameLayout mIconContainer = (FrameLayout) mIconImageView.getParent();
                FrameLayout mDockBlur = (FrameLayout) EzxHelpUtils.getAdditionalInstanceField(param.getThisObject(), "mDockBlur");
                FrameLayout view = new FrameLayout(mIconImageView.getContext());

                mIconImageView.setVisibility(View.GONE);
                mDockBlur.addView(view);

                new BlurUtils(mDockBlur, "home_big_folder_icon_bg_custom");

                mIconContainer.addView(mDockBlur, 0);
                FrameLayout.LayoutParams lp1 = (FrameLayout.LayoutParams) mDockBlur.getLayoutParams();
                lp1.gravity = Gravity.CENTER;

                if(isPad()){
                    lp1.width = mFolderWidth * 2;
                    lp1.height = mFolderHeight *2;
                }else {
                    lp1.width = mFolderWidth;
                    lp1.height = mFolderHeight;
                }

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


        Method FolderIcon2x2_4_OnFinishInflate = EzxHelpUtils.findMethodExactIfExists(mFolderIcon2x2_4, "onFinishInflate", Void.TYPE);
        Method FolderIcon2x2_9_OnFinishInflate = EzxHelpUtils.findMethodExactIfExists(mFolderIcon2x2_9, "onFinishInflate", Void.TYPE);

        if (FolderIcon2x2_4_OnFinishInflate != null && FolderIcon2x2_9_OnFinishInflate != null) {
            findAndHookMethod(mFolderIcon2x2_4, "onFinishInflate", mBigFolderIconBlur);
            findAndHookMethod(mFolderIcon2x2_9, "onFinishInflate", mBigFolderIconBlur);
        } else if (mFolderIcon2x2 != null) {
            findAndHookMethod(mFolderIcon2x2, "onFinishInflate", mBigFolderIconBlur);
        }

        hookAllConstructors(mDragView, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                View mDragView = (View) param.getThisObject();
                mDragView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

                Object mDragInfo = EzxHelpUtils.getObjectField(param.getThisObject(), "mDragInfo");
                int itemType = (int) EzxHelpUtils.getObjectField(mDragInfo, "itemType");
                Object mLauncher = EzxHelpUtils.getObjectField(param.getThisObject(), "mLauncher");
                boolean isFolderShowing = (boolean) EzxHelpUtils.callMethod(mLauncher, "isFolderShowing");

                if (!isFolderShowing && itemType == 21) {
                    new BlurUtils(mDragView, "home_big_folder_icon_bg_custom");
                }
            }
        });
    }
}
