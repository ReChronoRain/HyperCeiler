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
package com.sevtinge.hyperceiler.module.hook.home;

import android.app.Activity;

import com.sevtinge.hyperceiler.module.base.BaseHook;

// what is it?
public class AllAppsBlur extends BaseHook {

    Class<?> mLauncher;
    Class<?> mBlurUtils;
    Class<?> mAllAppsTransitionController;
    Class<?> mActivityCls;
    Activity mActivity;

    @Override
    public void init() {
        mActivityCls = Activity.class;
        mLauncher = findClassIfExists("com.miui.home.launcher.Launcher");
        mBlurUtils = findClassIfExists("com.miui.home.launcher.common.BlurUtils");
        mAllAppsTransitionController = findClassIfExists("com.miui.home.launcher.allapps.BaseAllAppsContainerView");

    }
}
