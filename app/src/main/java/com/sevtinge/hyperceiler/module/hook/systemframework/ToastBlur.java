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
package com.sevtinge.hyperceiler.module.hook.systemframework;

import static com.sevtinge.hyperceiler.module.hook.systemframework.corepatch.XposedHelper.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;

import android.content.Context;
import android.view.View;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ToastBlur implements IXposedHookZygoteInit {
    private static final String TAG = "ToastBlur";

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        ClassLoader classLoader = startupParam.getClass().getClassLoader();
        findAndHookMethod("android.widget.ToastStubImpl", classLoader, "addBlur",
                View.class, Context.class, boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        // logE(TAG, "view: " + param.args[0] + " con: " + param.args[1] + " boo: " + param.args[2]);
                    }
                }
        );

        findAndHookMethod("android.widget.ToastPresenter", classLoader, "trySendAccessibilityEvent",
                View.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        boolean mCustomizeView = false;
                        View mView = (View) XposedHelpers.getObjectField(param.thisObject, "mView");
                        if (mView == null) return;
                        if (mView != null) {
                            mCustomizeView = true;
                        }
                        Object toastStub = null;
                        Class<?> ToastStub = findClassIfExists("android.widget.ToastStub", classLoader);
                        Context mContext = mView.getContext();
                        if (ToastStub != null) {
                            toastStub = XposedHelpers.callStaticMethod(ToastStub, "get");
                            XposedHelpers.callMethod(toastStub, "addBlur", mView, mContext, false);
                        }
                    }
                }
        );

        findAndHookMethod("android.widget.ToastStubImpl$1", classLoader,
                "onLayoutChange",
                View.class, int.class, int.class, int.class,
                int.class, int.class, int.class, int.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        // logE(TAG, "change");
                    }
                }
        );
    }

    private void logE(String tag, String mag) {
        XposedBridge.log(tag + " " + mag);
    }
}
