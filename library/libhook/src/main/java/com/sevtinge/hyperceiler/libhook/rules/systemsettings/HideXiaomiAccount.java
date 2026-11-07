/*
  * This file is part of HyperCeiler.
  *
  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
  *
  * Copyright (C) 2023-2026 HyperCeiler Contributions
  */
package com.sevtinge.hyperceiler.libhook.rules.systemsettings;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.util.Iterator;
import java.util.List;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class HideXiaomiAccount extends BaseHook {
    private static final String XIAOMI_ACCOUNT_PKG = "com.xiaomi.account";
    private static final String XIAOMI_ACCOUNT_ACTION = "android.settings.XIAOMI_ACCOUNT_SYNC_SETTINGS";

    @Override
    public void init() {
        Class<?> mMiuiSettings = findClassIfExists("com.android.settings.MiuiSettings");
        if (mMiuiSettings == null) return;

        findAndHookMethod(mMiuiSettings, "updateHeaderList", List.class, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                if (param.getArgs()[0] == null) return;
                List<?> headers = (List<?>) param.getArgs()[0];
                removeXiaomiAccountHeaders(headers);
            }
        });
    }

    private void removeXiaomiAccountHeaders(List<?> headers) {
        Iterator<?> iterator = headers.iterator();
        while (iterator.hasNext()) {
            Object header = iterator.next();
            Object intent = getObjectField(header, "intent");
            if (intent == null) continue;

            String pkg = (String) callMethod(intent, "getPackage");
            String action = (String) callMethod(intent, "getAction");

            if (XIAOMI_ACCOUNT_PKG.equals(pkg) || XIAOMI_ACCOUNT_ACTION.equals(action)) {
                iterator.remove();
            }
        }
    }
}
