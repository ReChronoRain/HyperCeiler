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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.display;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class BackgroundBlur extends BaseHook {
    private final static String TAG = "MYBackgroundBlur";
    private final static String mode = "persist.sys.background_blur_mode";
    private final static String supported = "persist.sys.background_blur_supported";
    private final static String status = "persist.sys.background_blur_status_default";
    private final static String version = "persist.sys.background_blur_version";
    private Class<?> SystemProperties = null;
    private boolean done = false;

    @Override
    public void init() {
        SystemProperties = findClassIfExists("android.os.SystemProperties");
        if (SystemProperties == null) return;
        try {
            setProp();
            XposedLog.d(TAG, "Enabled Advanced Materials Successfully!");
        } catch (Throwable e) {
            XposedLog.w(TAG, "Failed to turn on advanced materials!Try the second method...");
            Method[] methods = SystemProperties.getDeclaredMethods();
            for (Method method : methods) {
                switch (method.getName()) {
                    case "get", "getInt", "getBoolean" -> hookProp(method);
                }
            }
        }
    }

    IMethodHook xcMethodHook = new IMethodHook() {
        @Override
        public void after(AfterHookParam param) {
            if (done) return;
            Object def = null;
            Object result = param.getResult();
            String key = (String) param.getArgs()[0];
            if (param.getArgs().length > 1) {
                def = param.getArgs()[1];
            }
            Object get = isBlurProp(key);
            // logE(TAG, "key: " + key + " def: " + def + " result: " + result + " get: " + get);
            if (get != null) {
                if ("get".equals(param.getMember().getName())) {
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
        hookMethod(method, xcMethodHook);
    }

    private void setProp() {
        callStaticMethod(SystemProperties, "set", supported, String.valueOf(true));
        callStaticMethod(SystemProperties, "set", status, String.valueOf(true));
        callStaticMethod(SystemProperties, "set", mode, String.valueOf(0));
        callStaticMethod(SystemProperties, "set", version, String.valueOf(2));
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
