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
package com.sevtinge.hyperceiler.libhook.utils.api;

import java.lang.reflect.Method;

import io.github.lingqiqi5211.ezhooktool.core.java.Classes;
import io.github.lingqiqi5211.ezhooktool.core.java.Methods;

public class TelephonyManager {
    Object telephonyManager;
    String name = "miui.telephony.TelephonyManager";

    public TelephonyManager() {
        telephonyManager = getDefaultTelephonyManager();
    }

    public static TelephonyManager getDefault() {
        return new TelephonyManager();
    }

    public void setUserFiveGEnabled(boolean enabled) {
        callTelephonyManagerMethod("setUserFiveGEnabled", new Class[]{boolean.class}, enabled);
    }

    public boolean isUserFiveGEnabled() {
        Boolean isEnabled = callTelephonyManagerMethod("isUserFiveGEnabled", new Class[0]);
        return isEnabled != null && isEnabled;
    }

    public boolean isFiveGCapable() {
        Boolean isCapable = callTelephonyManagerMethod("isFiveGCapable", new Class[0]);
        return isCapable != null && isCapable;
    }

    private Object getDefaultTelephonyManager() {
        try {
            return findTelephonyManagerMethod("getDefault", new Class[0]).invoke(null);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T callTelephonyManagerMethod(String methodName, Class<?>[] parameterTypes, Object... args) {
        if (telephonyManager == null) {
            return null;
        }
        try {
            return (T) findTelephonyManagerMethod(methodName, parameterTypes).invoke(telephonyManager, args);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    private Method findTelephonyManagerMethod(String methodName, Class<?>[] parameterTypes) {
        var search = Methods.find(Classes.loadClass(name, ClassLoader.getSystemClassLoader()))
            .filterByName(methodName);
        Method method = parameterTypes.length == 0
            ? search.filterEmptyParam().first()
            : search.filterByParamTypes(parameterTypes).first();
        method.setAccessible(true);
        return method;
    }
}
