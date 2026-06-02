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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.battery;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import java.lang.reflect.Method;
import java.util.List;

public class PowerConsumptionRanking extends BaseHook {
    private List<Method> mMiuiVersionCodeMethods;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        // 仅查找 ro.miui.ui.version.code 相关的版本判断方法即可，
        // 历史实现还查找了一个 usingStrings("%d %s %d %s") 的容器类并 hookAllConstructors，
        // 然后在 constructor 的 before 里嵌套 hook 每个 version-code 方法，
        // 这种嵌套会让同一方法被反复 hook（每次构造一次目标类都新增一个 hook 实例）。
        // 改为 init() 中一次性 hook，全局生效。
        mMiuiVersionCodeMethods = requiredMemberList("MiuiVersionCode", bridge ->
            bridge.findMethod(FindMethod.create()
                .matcher(MethodMatcher.create()
                    .declaredClass(ClassMatcher.create()
                        .usingStrings("ro.miui.ui.version.code"))
                    .usingNumbers(9)
                    .returnType(boolean.class)
                )
            )
        );
        return true;
    }

    @Override
    public void init() {
        for (Method method : mMiuiVersionCodeMethods) {
            try {
                hookMethod(method, returnConstant(false));
            } catch (Throwable t) {
                XposedLog.w(TAG, getPackageName(), "Failed to hook " + method, t);
            }
        }
    }
}
