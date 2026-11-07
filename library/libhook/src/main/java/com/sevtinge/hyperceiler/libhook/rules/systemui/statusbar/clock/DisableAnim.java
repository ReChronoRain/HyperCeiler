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
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.clock;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class DisableAnim extends BaseHook {
    @Override
    public void init() {
        // 三版固件里都没有 FakeStatusBarClockController（ROM DEX 核验），因此不能再用硬 findClass：
        // 它会在下面的 OS4 挂点注册之前抛断。类存在才挂，不存在则静默跳过。
        Class<?> legacyClockController = findClassIfExists("com.android.systemui.statusbar.policy.FakeStatusBarClockController");
        if (legacyClockController != null) {
            findAndHookMethod(legacyClockController, "onPanelStretchChanged", float.class, boolean.class, new IMethodHook() {
                @Override
                public void before(final HookParam param) {
                    param.setResult(null);
                }
            });
        }

        // OS4: 下拉拉伸回调迁到 ControlCenterExpandControllerDelegate（ROM DEX 核验：onStretchHeightChanged(float)）
        Class<?> os4ExpandDelegate = findClassIfExists("com.miui.systemui.controlcenter.container.ControlCenterExpandControllerDelegate");
        if (os4ExpandDelegate != null) {
            findAndHookMethod(os4ExpandDelegate, "onStretchHeightChanged", float.class, new IMethodHook() {
                @Override
                public void before(final HookParam param) {
                    param.setResult(null);
                }
            });
        }
    }
}
