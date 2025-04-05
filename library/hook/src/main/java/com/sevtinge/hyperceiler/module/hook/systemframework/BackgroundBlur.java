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
package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class BackgroundBlur extends BaseHook {
    private final static String TAG = "MYBackgroundBlur";
    private final static String mode = "persist.sys.background_blur_mode";
    private final static String supported = "persist.sys.background_blur_supported";
    private final static String status = "persist.sys.background_blur_status_default";
    private final static String version = "persist.sys.background_blur_version";
    private Class<?> SystemProperties = null;
    private boolean done = false;

    @Override
    public void init() throws NoSuchMethodException {
        SystemProperties = findClassIfExists("android.os.SystemProperties");
        if (SystemProperties == null) return;
        try {
            setProp();
            logI(TAG, "Enabled Advanced Materials Successfully!");
        } catch (Throwable e) {
            logE(TAG, "Failed to turn on advanced materials!Try the second method...");
            Method[] methods = SystemProperties.getDeclaredMethods();
            for (Method method : methods) {
                switch (method.getName()) {
                    case "get", "getInt", "getBoolean" -> hookProp(method);
                }
            }
        }
    }

    XC_MethodHook xcMethodHook = new XC_MethodHook() {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            if (done) return;
            Object def = null;
            Object result = param.getResult();
            String key = (String) param.args[0];
            if (param.args.length > 1) {
                def = param.args[1];
            }
            Object get = isBlurProp(key);
            // logE(TAG, "key: " + key + " def: " + def + " result: " + result + " get: " + get);
            if (get != null) {
                if ("get".equals(param.method.getName())) {
                    param.setResult(String.valueOf(get));
                } else param.setResult(get);
            }
            if (supported.equals(key)) {
                try {
                    setProp();
                    done = true;
                    // logE(TAG, "key: " + key + " set: " + String.valueOf(get));
                } catch (Throwable e) {
                    done = false;
                    // logE(TAG, "key: " + key + " e: " + e);
                }
            }
        }
    };

    private void hookProp(Method method) {
        XposedBridge.hookMethod(method, xcMethodHook);
    }

    private void setProp() throws Throwable {
        XposedHelpers.callStaticMethod(SystemProperties, "set", supported, String.valueOf(true));
        XposedHelpers.callStaticMethod(SystemProperties, "set", status, String.valueOf(true));
        XposedHelpers.callStaticMethod(SystemProperties, "set", mode, String.valueOf(0));
        XposedHelpers.callStaticMethod(SystemProperties, "set", version, String.valueOf(2));
    }

    private Object isBlurProp(String key) {
        switch (key) {
            case supported, status -> {
                return true;
            }
            case mode -> {
                return 0;
            }
            case version -> {
                return 2;
            }
            default -> {
                return null;
            }
        }
    }
}
