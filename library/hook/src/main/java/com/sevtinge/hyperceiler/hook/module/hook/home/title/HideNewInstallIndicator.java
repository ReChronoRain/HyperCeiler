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
package com.sevtinge.hyperceiler.hook.module.hook.home.title;

import com.sevtinge.hyperceiler.hook.module.base.pack.home.HomeBaseHookNew;

public class HideNewInstallIndicator extends HomeBaseHookNew {

    @Version(isPad = false, min = 600000000)
    private void initOS3Hook() {
        findAndHookMethod("com.miui.home.icon.TitleTextView",
            "updateNewInstallIndicator",
            boolean.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(null);
                }
            }
        );

        findAndHookMethod("com.miui.home.folder.FolderIcon",
            "updateNewInstallIndicator",
            boolean.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(null);
                }
            }
        );
    }

    @Override
    public void initBase() {
        findAndHookMethod("com.miui.home.launcher.TitleTextView",
            "updateNewInstallIndicator",
            boolean.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(null);
                }
            }
        );
    }
}
