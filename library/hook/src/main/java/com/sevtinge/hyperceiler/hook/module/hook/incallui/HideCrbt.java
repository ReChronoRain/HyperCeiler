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
package com.sevtinge.hyperceiler.hook.module.hook.incallui;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;


public class HideCrbt extends BaseHook {
    Class<?> loadClass;

    public void init() {
        loadClass = findClassIfExists("com.android.incallui.Call");
        try {
            hookAllMethods(loadClass, "getVideoCall", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(null);
                }
            });
            findAndHookMethod(loadClass, "setPlayingVideoCrbt", int.class, boolean.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.args[0] = 0;
                    param.args[1] = Boolean.FALSE;
                }
            });
            /*hookAllMethods(loadClass, "setPlayingVideoCrbt", new MethodHook(){
                    Integer.TYPE, Boolean.TYPE, beforeHookedMethod()
            });*/
        } catch (Exception e) {
            logE(TAG, this.lpparam.packageName, "method hooked failed! " + e);
        }
    }
    /*public final void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
        HideCrbt.super.before(methodHookParam);
        methodHookParam.args[0]=0;
        methodHookParam.args[1]=Boolean.FALSE;
    }*/
}
