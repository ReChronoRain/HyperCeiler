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
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;


import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.newInstance;

import android.content.Context;
import android.os.Bundle;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.moduleload.GlobalActions;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;
import io.github.libxposed.api.XposedInterface;

public class SwipeAndStop extends BaseHook {
    @Override
    public void init() {
        Class<?> VibratorCls = findClassIfExists("android.os.Vibrator");
        hookAllMethods("com.miui.home.recents.GestureBackArrowView", "setReadyFinish", new IMethodHook() {
            private XposedInterface.MethodUnhooker<?> vibratorHook = null;

            @Override
            public void before(BeforeHookParam param) {
                vibratorHook = findAndHookMethod(VibratorCls, "vibrate", long.class, doNothing());
            }

            @Override
            public void after(AfterHookParam param) {
                if (vibratorHook != null) {
                    vibratorHook.unhook();
                }
            }
        });

        findAndHookMethod("com.miui.home.recents.GestureStubView", "disableQuickSwitch", boolean.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                param.getArgs()[0] = false;
            }
        });
        findAndHookMethod("com.miui.home.recents.GestureStubView", "isDisableQuickSwitch", returnConstant(false));
        findAndHookMethod("com.miui.home.recents.GestureStubView", "getNextTask", Context.class, boolean.class, int.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                boolean switchApp = (boolean) param.getArgs()[1];
                if (switchApp) {
                    Context mContext = (Context) param.getArgs()[0];
                    Bundle bundle = new Bundle();
                    bundle.putInt("inDirection", (int) param.getArgs()[2]);
                    if (GlobalActions.handleAction(mContext, "pref_key_controls_fsg_swipeandstop")) {
                        Class<?> Task = findClassIfExists("com.android.systemui.shared.recents.model.Task");
                        param.setResult(newInstance(Task));
                        return;
                    }
                }
                param.setResult(null);
            }
        });
    }
}
