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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.setStaticBooleanField;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class DisableMiuiLite extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("miui.os.Build", "isMiuiLiteVersion", new IMethodHook() {
            @Override
            public void before(final BeforeHookParam param) {
                param.setResult(false);
            }
        });
        setStaticBooleanField(findClassIfExists("miui.util.DeviceLevel"), "IS_MIUI_GO_VERSION", false);
        setStaticBooleanField(findClassIfExists("miui.util.DeviceLevel"), "IS_MIUI_LITE_VERSION", false);
        setStaticBooleanField(findClassIfExists("miui.util.DeviceLevel"), "IS_MIUI_MIDDLE_VERSION", false);
    }
}
