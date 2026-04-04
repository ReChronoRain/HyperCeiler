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
package com.sevtinge.hyperceiler.libhook.rules.home;

import android.view.MotionEvent;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.libxposed.api.XposedInterface;

public class ToastSlideAgain extends BaseHook {
    private static final ThreadLocal<Boolean> sSuppressToast = new ThreadLocal<>();

    @Override
    public void init() {
        findAndChainMethod(findClassIfExists("android.widget.Toast"), "show",
            chain -> {
                if (Boolean.TRUE.equals(sSuppressToast.get())) {
                    return null;
                }
                return chain.proceed();
            }
        );

        findAndChainMethod("com.miui.home.recents.NavStubView",
            "onPointerEvent", MotionEvent.class,
            (XposedInterface.Hooker) chain -> {
                sSuppressToast.set(true);
                try {
                    return chain.proceed();
                } finally {
                    sSuppressToast.remove();
                }
            }
        );

        findAndChainMethod("com.miui.home.recents.GestureModeApp",
            "onStartGesture",
            chain -> {
                sSuppressToast.set(true);
                try {
                    return chain.proceed();
                } finally {
                    sSuppressToast.remove();
                }
            }
        );
    }
}
