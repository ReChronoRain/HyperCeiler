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

import android.content.Context;

import io.github.lingqiqi5211.ezhooktool.core.java.Classes;
import io.github.lingqiqi5211.ezhooktool.core.java.Methods;

import java.lang.reflect.Method;

public class SubscriptionManagerProvider {
    private static final String CLASS = "android.telephony.SubscriptionManager";

    private final Object subscriptionManager;

    public SubscriptionManagerProvider(Context context) {
        subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
    }

    public int[] getActiveSubscriptionIdList(boolean visibleOnly) {
        try {
            return (int[]) findSubscriptionManagerMethod().invoke(subscriptionManager, visibleOnly);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return null;
        }
    }

    private Method findSubscriptionManagerMethod() {
        Method method = Methods.find(Classes.loadClass(CLASS, ClassLoader.getSystemClassLoader()))
            .filterByName("getActiveSubscriptionIdList")
            .filterByParamTypes(boolean.class)
            .first();
        method.setAccessible(true);
        return method;
    }
}
