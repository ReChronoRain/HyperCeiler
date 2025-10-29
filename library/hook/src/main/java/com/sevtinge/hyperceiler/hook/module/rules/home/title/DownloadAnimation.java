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
package com.sevtinge.hyperceiler.hook.module.rules.home.title;

import com.sevtinge.hyperceiler.hook.module.base.pack.home.HomeBaseHookNew;

public class DownloadAnimation extends HomeBaseHookNew {

    @Version(isPad = false, min = 600000000)
    private void initOS3Hook() {
        hookAllMethods("com.miui.home.common.utils.DeviceLevelUtils", "needMamlProgressIcon", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        hookAllMethods("com.miui.home.common.utils.DeviceLevelUtils", "needRemoveDownloadAnimationDevice", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }

    @Override
    public void initBase() {
        try {
            hookAllMethods("com.miui.home.launcher.common.DeviceLevelUtils", "needMamlProgressIcon", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
            hookAllMethods("com.miui.home.launcher.common.DeviceLevelUtils", "needRemoveDownloadAnimationDevice", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });
        } catch (Exception e) {
            hookAllMethods("com.miui.home.launcher.common.CpuLevelUtils", "needMamlDownload", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        }
    }
}
