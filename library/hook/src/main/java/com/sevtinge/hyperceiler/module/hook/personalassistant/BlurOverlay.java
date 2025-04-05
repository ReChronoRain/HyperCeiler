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
package com.sevtinge.hyperceiler.module.hook.personalassistant;

import android.content.res.Configuration;
import android.os.Build;
import android.view.Window;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class BlurOverlay extends BaseHook {

    Class<?> mDeviceAdapter;
    Class<?> mPhoneDeviceAdapter;
    Class<?> mFoldableDeviceAdapter;

    Class<?> v;

    Window window;

    @Override
    public void init() {

        v = findClassIfExists("c.h.e.p.v");

        Class<?> mF = findClassIfExists("c.h.c.a.a.f");

        mDeviceAdapter = findClassIfExists("com.miui.personalassistant.device.DeviceAdapter");
        mPhoneDeviceAdapter = findClassIfExists("com.miui.personalassistant.device.PhoneDeviceAdapter");
        mFoldableDeviceAdapter = findClassIfExists("com.miui.personalassistant.device.FoldableDeviceAdapter");

        hookAllMethods(mDeviceAdapter, "create", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (v != null) {
                    findAndHookMethod(v, "j", XC_MethodReplacement.returnConstant(true));
                }
            }
        });

        findAndHookMethod(mFoldableDeviceAdapter, "blurOverlayWindow", float.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                float f = (float) param.args[0];

                if (f >= 0f) {
                    hookAllConstructors(mF, new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            window = (Window) XposedHelpers.getObjectField(param.thisObject, "b");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                window.setBackgroundBlurRadius((int) f);
                            }
                        }
                    });
                }
            }
        });

        findAndHookMethod(mFoldableDeviceAdapter, "onConfigurationChanged", Configuration.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                /*Configuration configuration = (Configuration) param.args[0];
                Configuration mConfiguration = (Configuration) XposedHelpers.getObjectField(param.thisObject, "mConfiguration");
                Context mThemedContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mThemedContext");

                if (mThemedContext instanceof ComponentCallbacks) {
                    ((ComponentCallbacks) mThemedContext).onConfigurationChanged(configuration);
                }

                int updateFrom = mConfiguration.updateFrom(configuration);

                boolean z = (updateFrom & 128) != 0;
                boolean z2 = (updateFrom & 1024) != 0;

                if (z || z2) {
                    Class<?> mDensityScaleUtil = findClass("com.miui.personalassistant.device.DensityScaleUtil");
                    XposedHelpers.callStaticMethod(mDensityScaleUtil, "scaleHostDensity", mThemedContext, 0);

                }


*/
                findAndHookMethod("com.miui.launcher.overlay.server.pane.SlidingPaneStateManager", "a", boolean.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.args[0] = false;
                    }
                });
                int mCurrentBlurRadius = XposedHelpers.getIntField(param.thisObject, "mCurrentBlurRadius");
                XposedHelpers.setIntField(param.thisObject, "mScreenSize", 5);
                XposedHelpers.setIntField(param.thisObject, "mCurrentBlurRadius", 100);
                XposedHelpers.callMethod(param.thisObject, "blurOverlayWindow", mCurrentBlurRadius);
            }
        });


        findAndHookMethod(mFoldableDeviceAdapter, "onOpened", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                int mCurrentBlurRadius = XposedHelpers.getIntField(param.thisObject, "mCurrentBlurRadius");
                if (Float.compare(mCurrentBlurRadius, 100f) != 0) {
                    XposedHelpers.setIntField(param.thisObject, "mCurrentBlurRadius", 100);
                    XposedHelpers.callMethod(param.thisObject, "blurOverlayWindow", mCurrentBlurRadius);
                }
            }
        });

        findAndHookMethod(mFoldableDeviceAdapter, "onScroll", float.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                float f = (float) param.args[0];
                int i = (int) ((f - 0.49f) * 204.08163f);
                int mCurrentBlurRadius = XposedHelpers.getIntField(param.thisObject, "mCurrentBlurRadius");

                int i2 = mCurrentBlurRadius;

                if (i2 != i) {
                    if (i2 <= 0 || i >= 0) {
                        mCurrentBlurRadius = i;
                    } else {
                        mCurrentBlurRadius = 0;
                    }
                    XposedHelpers.callMethod(param.thisObject, "blurOverlayWindow", mCurrentBlurRadius);
                }
            }
        });
    }
}
