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
package com.sevtinge.hyperceiler.libhook.rules.systemui.other;

import android.content.Context;
import android.view.View;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.lang.ref.WeakReference;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class ToastBlur extends BaseHook {
    @Override
    public void init() {
        /*findAndHookMethod("android.widget.ToastStubImpl", lpparam.classLoader, "addBlur",
            View.class, Context.class, boolean.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    // logE(TAG, "view: " + param.args[0] + " con: " + param.args[1] + " boo: " + param.args[2]);
                }
            }
        );*/

        findAndHookMethod("android.widget.ToastPresenter", "trySendAccessibilityEvent", View.class, String.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                View mView = (View) getObjectField(param.getThisObject(), "mView");
                if (mView == null) return;
                Class<?> ToastStub = findClassIfExists("android.widget.ToastStub");
                Context mContext = mView.getContext();
                if (ToastStub != null) {
                    Object toastStub = callStaticMethod(ToastStub, "get");
                    try {
                        callMethod(toastStub, "addBlur", new Class[]{View.class, Context.class, boolean.class}, mView, mContext, false);
                    } catch (NoSuchMethodError e) {
                        callMethod(toastStub, "addBlur", new Class[]{View.class, WeakReference.class, boolean.class}, mView, new WeakReference<>(mContext), false);
                    }
                }
            }
        });

        /*findAndHookMethod("android.widget.ToastStubImpl$1", lpparam.classLoader,
            "onLayoutChange",
            View.class, int.class, int.class, int.class,
            int.class, int.class, int.class, int.class, int.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    // logE(TAG, "change");
                }
            }
        );*/
    }
}
