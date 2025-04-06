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
package com.sevtinge.hyperceiler.hook.module.hook.gallery;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;

public class EnablePdf extends BaseHook {
    XC_MethodHook.Unhook isGlobal;

    @Override
    public void init() {
        try {
            findAndHookMethod("com.miui.gallery.request.PicToPdfHelper", "isPicToPdfSupport", new MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } catch (Throwable e) {
            hookAllConstructors("com.miui.gallery.ui.ProduceCreationDialogWithMediaEditorConfig",  new MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    isGlobal = findAndHookMethodUseUnhook("com.miui.gallery.util.BuildUtil", lpparam.classLoader, "isGlobal", new MethodHook() {
                        @Override
                        protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                            param.setResult(false);
                        }
                    });
                }

                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    if (isGlobal != null) {
                        isGlobal.unhook();
                    }
                    isGlobal = null;
                }
            });
        }
    }
}
