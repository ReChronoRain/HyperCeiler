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
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.BlurUtils;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class DockCustom extends BaseHook {

    FrameLayout mDockView;

    boolean isFolderShowing;
    boolean isShowEditPanel;
    boolean isRecentShowing;

    Class<?> mLauncherCls;
    Class<?> mLauncherStateCls;
    Class<?> mDeviceConfigCls;
    Class<?> mFolderInfo;
    Class<?> mBlurUtils;

    @Override
    public void init() {
        mLauncherCls = findClassIfExists("com.miui.home.launcher.Launcher");
        mLauncherStateCls = findClassIfExists("com.miui.home.launcher.LauncherState");
        mDeviceConfigCls = findClassIfExists("com.miui.home.launcher.DeviceConfig");
        mFolderInfo = findClassIfExists("com.miui.home.launcher.FolderInfo");
        mBlurUtils = findClassIfExists("com.miui.home.launcher.common.BlurUtils");


        findAndHookMethod(mLauncherCls, "onCreate", Bundle.class, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                Activity mActivity = (Activity) param.getThisObject();

                FrameLayout mSearchBarContainer = (FrameLayout) callMethod(param.getThisObject(), "getSearchBarContainer");
                FrameLayout mSearchEdgeLayout = (FrameLayout) mSearchBarContainer.getParent();

                int mDockHeight = DisplayUtils.dp2px(PrefsBridge.getInt("home_dock_bg_height", 80));
                int mDockMargin = DisplayUtils.dp2px(PrefsBridge.getInt("home_dock_bg_margin_horizontal", 30));
                int mDockBottomMargin = DisplayUtils.dp2px(PrefsBridge.getInt("home_dock_bg_margin_bottom", 30));

                mDockView = new FrameLayout(mSearchBarContainer.getContext());
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mDockHeight);
                layoutParams.gravity = Gravity.BOTTOM;
                layoutParams.setMargins(mDockMargin, 0, mDockMargin, mDockBottomMargin);
                mDockView.setLayoutParams(layoutParams);
                mSearchEdgeLayout.addView(mDockView, 0);

                new BlurUtils(mDockView, "home_dock_bg_custom");


                findAndHookMethod(mLauncherCls, "isFolderShowing", new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        isFolderShowing = (boolean) param.getResult();
                    }
                });

                findAndHookMethod(mLauncherCls, "showEditPanel", boolean.class, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        isShowEditPanel = (boolean) param.getArgs()[0];
                        mDockView.setVisibility(isShowEditPanel ? View.GONE : View.VISIBLE);
                    }
                });

                findAndHookMethod(mLauncherCls, "openFolder", mFolderInfo, View.class, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        mDockView.setVisibility(View.GONE);
                    }
                });

                findAndHookMethod(mLauncherCls, "closeFolder", boolean.class, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        if (!isShowEditPanel) mDockView.setVisibility(View.VISIBLE);
                    }
                });

                findAndHookMethod(mBlurUtils, "fastBlurWhenEnterRecents", mLauncherCls, mLauncherStateCls, boolean.class, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        mDockView.setVisibility(View.GONE);
                    }
                });
            }
        });

        findAndHookMethod(mLauncherCls, "onStateSetStart", mLauncherStateCls, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                Boolean mLauncherState = param.getArgs()[0].getClass().getSimpleName().equals("LauncherState");
                Boolean mNormalState = param.getArgs()[0].getClass().getSimpleName().equals("NormalState");

                if ((mLauncherState || mNormalState) && !isFolderShowing && !isShowEditPanel) {
                    mDockView.setVisibility(View.VISIBLE);
                } else {
                    mDockView.setVisibility(View.GONE);
                }
            }
        });


        /*findAndHookMethod(mDeviceConfigCls,"calcHotSeatsMarginTop", Context.class, boolean.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                Context context = (Context) param.getArgs()[0];
                param.setResult(DisplayUtils.dip2px(context, PrefsBridge.getInt("home_dock_margin_top",25)));
            }
        });

        findAndHookMethod(mDeviceConfigCls,"calcHotSeatsMarginBottom", Context.class, boolean.class, boolean.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                Context context = (Context) param.getArgs()[0];
                param.setResult(DisplayUtils.dip2px(context, PrefsBridge.getInt("home_dock_icon_margin_bottom",90)));
            }
        });*/
    }


    public GradientDrawable getDockBackground(Context context) {
        GradientDrawable mDockBackground = new GradientDrawable();
        mDockBackground.setShape(GradientDrawable.RECTANGLE);
        mDockBackground.setColor(Color.argb(60, 255, 255, 255));
        mDockBackground.setCornerRadius(DisplayUtils.dp2px(22));
        return mDockBackground;
    }

}
