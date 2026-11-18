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
package com.sevtinge.hyperceiler.libhook.rules.systemui.other;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

/**
 * 强制所有应用走无极窗口 2.0（CVW 2.0）的比例策略，让任意小窗都能自由调整宽高比。
 * <p>
 * 系统侧的判断全部在 WMShell（{@code /system_ext/framework/Miui-Window-Manager-Shell.jar}，
 * 运行在 SystemUI 进程）的 {@code com.android.wm.shell.multitasking.miuiinfinitymode} 里：
 * 只有声明了 {@code miui.window.CVW_ENABLED} 的应用、且处于工作台模式时，才会用
 * {@code MiuiInfinityModeRatioPolicy} + {@code MiuiInfinityModeRatioPositioner}（自由缩放），
 * 其余一律落到 {@code MiuiInfinityModeGirdPolicy}（无极 1.0，只有固定几档等比尺寸）。
 * 这里把入口判断点全部改成"支持"，从而不改任何应用的 manifest。
 * <p>
 * 宽高比仍被限制在系统尺寸池两端之间（0.5625 ~ 1.3333，即 9:16 ~ 4:3），
 * 尺寸上下限来自池内 level 的 min/max。
 */
public class ForceFreeformResize extends BaseHook {

    private static final String CONFIG_CLASS =
        "com.android.wm.shell.multitasking.miuiinfinitymode.MiuiInfinityModeSizeLevelConfig";
    private static final String LEVEL_POLICY_CLASS =
        "com.android.wm.shell.multitasking.miuiinfinitymode.MiuiInfinityModeLevelPolicyCompat";
    private static final String TASK_WRAPPER_CLASS =
        "com.android.wm.shell.multitasking.miuiinfinitymode.MiuiInfinityModeTaskWrapper";

    @Override
    public void init() {
        // 读不到说明当前版本没有无极窗口 2.0，直接跳过
        Class<?> configClass = findClassIfExists(CONFIG_CLASS);
        if (configClass == null) {
            return;
        }

        // 1. 所有应用都视为声明了 miui.window.CVW_ENABLED
        // 后续 getActivityCVWLevel 找不到 CVW_MODE 属性时会缺省返回 LEVEL_FULL(1)
        hookAllMethods(configClass, "isApplicationSupportCVW", returnConstant(true));

        // 2. CVW_MODE 缺省值补成 LEVEL_FULL，避免个别路径直接读 int 属性拿到 LEVEL_NULL(0)
        hookAllMethods(configClass, "getApplicationCVWLevel", returnConstant(1));

        // 3. 是否走 2.0：原逻辑还要求 MiuiDesktopModeStatus.isActive()，这里一并绕过，
        // 这样普通模式（非工作台）下的悬浮小窗也能自由缩放
        Class<?> levelPolicyClass = findClassIfExists(LEVEL_POLICY_CLASS);
        if (levelPolicyClass != null) {
            hookAllMethods(levelPolicyClass, "useNewLevelPolicy", returnConstant(true));
        }

        // 4. 拖角手势按 mStrategicPolicy==1 判断用自由缩放还是等比缩放，统一改成 2.0
        Class<?> taskWrapperClass = findClassIfExists(TASK_WRAPPER_CLASS);
        if (taskWrapperClass != null) {
            hookAllMethods(taskWrapperClass, "useNewVersionStrategy", returnConstant(true));
        }
    }
}
