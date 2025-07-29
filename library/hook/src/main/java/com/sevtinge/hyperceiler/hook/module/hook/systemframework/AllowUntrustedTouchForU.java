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
package com.sevtinge.hyperceiler.hook.module.hook.systemframework;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

public class AllowUntrustedTouchForU  extends BaseHook {

    //Class<?> mInputManager;

    @Override
    public void init() {
        //mInputManager = findClassIfExists("android.hardware.input.InputManager");
        //XposedHelpers.setStaticLongField(mInputManager, "BLOCK_UNTRUSTED_TOUCHES", 0x96aec7eL);
        findAndHookMethod("com.android.server.wm.WindowState", "getTouchOcclusionMode", new MethodHook(){
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                param.setResult(2);
            }
        });
    }
}
