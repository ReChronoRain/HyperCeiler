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
package com.sevtinge.hyperceiler.module.hook.home.other;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class AllowShareApk extends BaseHook{
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.home.launcher.common.Utilities", "isSecurityCenterSupportShareAPK", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(false);
                }
            }
        );
        findAndHookMethod("com.miui.home.launcher.shortcuts.SystemShortcutMenuItem$ShareAppShortcutMenuItem", "isValid", "com.miui.home.launcher.ItemInfo", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    findAndHookMethod("com.miui.home.launcher.common.Utilities", "isSystemPackage", Context.class, String.class, new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) {
                                param.setResult(false);
                            }
                        }
                    );
                }
            }
        );
        //mResHook.setResReplacement("com.miui.home", "XML", "file_paths", R.xml.hook_home_file_paths);

    }
}
