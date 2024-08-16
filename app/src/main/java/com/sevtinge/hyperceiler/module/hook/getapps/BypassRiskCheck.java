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
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.getapps;

import static de.robv.android.xposed.XposedHelpers.getStaticObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.ArrayList;

public class BypassRiskCheck extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.xiaomi.market.common.component.componentbeans.UnknownAppResult", "isExcellent", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        findAndHookMethod("com.xiaomi.market.common.component.componentbeans.RiskAppResult", "isExcellent", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        /*findAndHookMethod("com.xiaomi.market.common.component.componentbeans.GuardResult", "isExcellent", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });*/
        findAndHookMethod("com.xiaomi.market.business_ui.main.mine.app_security.check_page.risk_app.AppSecurityRiskAppView", "setListOpenImageStatus", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                setObjectField(param.thisObject, "riskModelList", new ArrayList<>());
            }
        });
        findAndHookMethod("com.xiaomi.market.business_ui.main.mine.app_security.check_page.unknown_app.AppSecurityUnknownAppView", "setListOpenImageStatus", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                setObjectField(param.thisObject, "unknownModelList", new ArrayList<>());
            }
        });
        findAndHookMethod("com.xiaomi.market.common.component.componentbeans.AppSecurityCheckRules$Companion", "getTotalResult", "com.xiaomi.market.common.component.componentbeans.AppSecurityCheckRules", "com.xiaomi.market.common.component.componentbeans.GuardResult", "com.xiaomi.market.common.component.componentbeans.RiskAppResult", "com.xiaomi.market.common.component.componentbeans.UnknownAppResult", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(getStaticObjectField(findClassIfExists("com.xiaomi.market.common.component.componentbeans.TotalResult"), "TOTAL_EXCELLENT"));
            }
        });
        findAndHookMethod("com.xiaomi.market.business_ui.main.mine.app_security.AppSecurityCheckManager", "getSecurityResultTxtByPage", "com.xiaomi.market.common.component.componentbeans.TotalResult", int.class, int.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.args[0] = getStaticObjectField(findClassIfExists("com.xiaomi.market.common.component.componentbeans.TotalResult"), "TOTAL_EXCELLENT");
                param.args[2] = 0;
            }
        });
    }
}
