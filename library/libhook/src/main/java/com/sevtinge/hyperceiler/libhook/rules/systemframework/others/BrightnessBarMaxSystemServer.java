 /*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

/**
 * 解锁系统亮度上限（system_server 进程）。
 *
 * 反编译 services.jar 确认：BrightnessInfo.brightnessMax 来自
 * BrightnessRangeController.getCurrentBrightnessMax()，其正常亮度上限由
 * NormalBrightnessModeController.recalculateMaxBrightness 按环境光查
 * DisplayDeviceConfig 的亮度限制映射得到（本机当前环境光下 = 0.31）。
 * 手动模式也应用该限制，导致 UI 拖到顶也只有 0.31 亮度（背光 5185）。
 *
 * hook getCurrentBrightnessMax() 返回 1.0，解锁全链路亮度上限。
 * 返回 1.0 是安全值（仅影响亮度上限计算，不影响系统稳定性）。
 *
 * 需配合 SystemUI 进程的 BrightnessBarMaxActual（UI 层 max + 换算）。
 */
public class BrightnessBarMaxSystemServer extends BaseHook {
    @Override
    public void init() {
        XposedLog.w(TAG, "init() called");
        try {
            findAndHookMethod(
                "com.android.server.display.BrightnessRangeController",
                "getCurrentBrightnessMax",
                new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        param.setResult(1.0f);
                    }
                }
            );
            XposedLog.w(TAG, "hooked BrightnessRangeController#getCurrentBrightnessMax -> 1.0");
        } catch (Throwable t) {
            XposedLog.e(TAG, "hook BrightnessRangeController failed", t);
        }
    }
}
