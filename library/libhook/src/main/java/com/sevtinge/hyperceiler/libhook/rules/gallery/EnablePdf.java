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
package com.sevtinge.hyperceiler.libhook.rules.gallery;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;
import io.github.libxposed.api.XposedInterface;

;

public class EnablePdf extends BaseHook {
    XposedInterface.MethodUnhooker isGlobal;

    @Override
    public void init() {
        try {
            findAndHookMethod("com.miui.gallery.request.PicToPdfHelper", "isPicToPdfSupport", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(true);
                }
            });
        } catch (Throwable e) {
            hookAllConstructors("com.miui.gallery.ui.ProduceCreationDialogWithMediaEditorConfig",  new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    isGlobal = findAndHookMethod("com.miui.gallery.util.BuildUtil", "isGlobal", new IMethodHook() {
                        @Override
                        public void before(BeforeHookParam param) {
                            param.setResult(false);
                        }
                    });
                }

                @Override
                public void after(AfterHookParam param) {
                    if (isGlobal != null) {
                        isGlobal.unhook();
                    }
                    isGlobal = null;
                }
            });
        }
    }
}
