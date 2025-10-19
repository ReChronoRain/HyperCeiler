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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.hook.module.hook.contacts;

import static de.robv.android.xposed.XposedHelpers.setBooleanField;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

public class DisableNetworkAssistantOfflineInfoManager extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        hookAllConstructors("com.mobile.businesshall.bean.OffLineData$BaseData", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                setBooleanField(param.thisObject, "isOffline", false);
            }
        });
        findAndHookMethod("com.mobile.businesshall.bean.OffLineData$BaseData", "isOffline", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }
}
