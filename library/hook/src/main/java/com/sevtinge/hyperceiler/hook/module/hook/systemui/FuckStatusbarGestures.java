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
package com.sevtinge.hyperceiler.hook.module.hook.systemui;

import android.annotation.SuppressLint;
import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import java.lang.reflect.Field;

public class FuckStatusbarGestures extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        hookAllConstructors("com.android.systemui.statusbar.phone.CentralSurfacesImpl", new MethodHook() {
            @SuppressLint({"PrivateApi", "SdCardPath"})
            @Override
            public void afterHookedMethod(MethodHookParam param) {
                try {
                    Object mGestureRec = param.thisObject
                            .getClass()
                            .getDeclaredField("mGestureRec")
                            .get(param.thisObject);
                    if (mGestureRec == null) return;
                    Field mFieldLogfile = mGestureRec
                            .getClass()
                            .getDeclaredField("mLogfile");
                    String mLogfile = (String) mFieldLogfile.get(mGestureRec);
                    if (mLogfile == null) return;
                    String mLogfileDir = mLogfile.substring(0, mLogfile.lastIndexOf('/'));
                    if ("/sdcard".equals(mLogfileDir)) {
                        mFieldLogfile.set(mGestureRec, "/sdcard/MIUI/" + mLogfile.substring(mLogfile.lastIndexOf('/') + 1));
                    }
                } catch (Throwable e) {
                    logE(TAG, lpparam.packageName, e);
                }
            }
        });
    }
}
