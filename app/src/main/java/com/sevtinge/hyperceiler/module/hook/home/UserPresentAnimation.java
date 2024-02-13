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

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;

import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class UserPresentAnimation extends BaseHook {

    Class<?> mUserPresentAnimationCompatV12Phone;

    @Override
    public void init() {
        mUserPresentAnimationCompatV12Phone = !isPad() ?
            findClassIfExists("com.miui.home.launcher.compat.UserPresentAnimationCompatV12Phone") :
        findClassIfExists("com.miui.home.launcher.compat.UserPresentAnimationCompatV12Spring");
        findAndHookMethod(mUserPresentAnimationCompatV12Phone, "getSpringAnimator", View.class, int.class, float.class, float.class, float.class, float.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.args[4] = 0.5f;
                param.args[5] = 0.5f;
            }
        });
    }
}
