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
package com.sevtinge.hyperceiler.hook.module.hook.home.drawer;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils.dp2px;

import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.ViewSwitcher;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool;
import com.sevtinge.hyperceiler.hook.utils.blur.MiBlurUtils;
import com.sevtinge.hyperceiler.hook.utils.blur.MiBlurUtilsKt;

public class AllAppsContainerViewSuperBlur extends BaseHook {
    private boolean isBlur;
    private View view;

    @Override
    public void init() {
        findAndHookMethod("com.miui.home.launcher.allapps.BaseAllAppsContainerView", "onFinishInflate",
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        ViewSwitcher mCategoryContainer = (ViewSwitcher) getObjectField(param.thisObject, "mCategoryContainer");
                        RelativeLayout appsView = (RelativeLayout) mCategoryContainer.getParent();
                        FrameLayout frameLayout = new FrameLayout(mCategoryContainer.getContext());
                        view = new View(mCategoryContainer.getContext());
                        frameLayout.addView(view);
                        view.getLayoutParams().height = FrameLayout.LayoutParams.MATCH_PARENT;
                        view.getLayoutParams().width = FrameLayout.LayoutParams.MATCH_PARENT;
                        if (!isBlur) {
                            MiBlurUtils.setPassWindowBlurEnabled(view, true);
                            MiBlurUtils.setMiBackgroundBlurMode(view, 1);
                            MiBlurUtils.setMiBackgroundBlurRadius(view, mPrefsMap.getInt("drawer_background_blur_degree",
                                    200));
                            MiBlurUtils.clearMiBackgroundBlendColor(view);
                            int a;
                            if (AppsTool.isDarkMode(view.getContext())) a = 100;
                            else a = 140;
                            MiBlurUtils.addMiBackgroundBlendColor(view, Color.argb(a, 0, 0, 0), 103);
                            MiBlurUtils.setMiViewBlurMode(view, 1);
                            MiBlurUtilsKt.INSTANCE.setBlurRoundRect(view, dp2px(
                                    mPrefsMap.getInt("home_drawer_blur_super_radius", 30)));
                        }
                        view.setBackgroundColor(mPrefsMap.getInt("home_drawer_blur_super_bg_color", 0));
                        isBlur = true;
                        appsView.addView(frameLayout, 0);
                    }
                }
        );
    }
}
