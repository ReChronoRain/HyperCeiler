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
package com.sevtinge.hyperceiler.module.hook.systemsettings.aiimage;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class UnlockSuperResolution extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.display.ScreenEnhanceEngineStatusCheck", "getSrForVideoStatus", Context.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.android.settings.display.ScreenEnhanceEngineStatusCheck", "getSrForImageStatus", Context.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.android.settings.display.ScreenEnhanceEngineStatusCheck", "getS2hStatus", Context.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.android.settings.display.ScreenEnhanceEngineStatusCheck", "isSrForVideoSupport", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.android.settings.display.ScreenEnhanceEngineStatusCheck", "isSrForImageSupport", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.android.settings.display.ScreenEnhanceEngineStatusCheck", "isS2hSupport", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
    }
}
