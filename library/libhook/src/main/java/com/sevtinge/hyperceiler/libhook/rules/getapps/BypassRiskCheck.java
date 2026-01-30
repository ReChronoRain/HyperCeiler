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

package com.sevtinge.hyperceiler.libhook.rules.getapps;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getPackageVersionCode;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.util.ArrayList;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

;

public class BypassRiskCheck extends BaseHook {
    @Override
    public void init() {
        boolean isNew = getPackageVersionCode(getLpparam()) >= 40005740;
        if (isNew) {
            hookAllMethods("com.xiaomi.market.business_ui.main.mine.app_security.check_page.risk_app.AppSecurityRiskAppView", "updateResultStatus", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.getArgs()[0] = 0;
                }
            });
            findAndHookMethod("com.xiaomi.market.common.component.componentbeans.AppSecurityCheckRules$Companion", "getTotalResult", "com.xiaomi.market.common.component.componentbeans.AppSecurityCheckRules", int.class, int.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(getStaticObjectField(findClassIfExists("com.xiaomi.market.common.component.componentbeans.TotalResult"), "TOTAL_EXCELLENT"));
                }
            });
            findAndHookMethod("com.xiaomi.market.business_ui.main.mine.app_security.AppSecurityCheckManager", "getRiskResultTxtByMine", int.class, int.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.getArgs()[1] = 0;
                }
            });
        } else {
            findAndHookMethod("com.xiaomi.market.common.component.componentbeans.UnknownAppResult", "isExcellent", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(true);
                }
            });
            findAndHookMethod("com.xiaomi.market.common.component.componentbeans.RiskAppResult", "isExcellent", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(true);
                }
            });
            findAndHookMethod("com.xiaomi.market.business_ui.main.mine.app_security.check_page.unknown_app.AppSecurityUnknownAppView", "setListOpenImageStatus", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    setObjectField(param.getThisObject(), "unknownModelList", new ArrayList<>());
                }
            });
            findAndHookMethod("com.xiaomi.market.common.component.componentbeans.AppSecurityCheckRules$Companion", "getTotalResult", "com.xiaomi.market.common.component.componentbeans.AppSecurityCheckRules", "com.xiaomi.market.common.component.componentbeans.GuardResult", "com.xiaomi.market.common.component.componentbeans.RiskAppResult", "com.xiaomi.market.common.component.componentbeans.UnknownAppResult", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(getStaticObjectField(findClassIfExists("com.xiaomi.market.common.component.componentbeans.TotalResult"), "TOTAL_EXCELLENT"));
                }
            });
        }
        /*findAndHookMethod("com.xiaomi.market.common.component.componentbeans.GuardResult", "isExcellent", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(true);
            }
        });*/
        findAndHookMethod("com.xiaomi.market.business_ui.main.mine.app_security.check_page.risk_app.AppSecurityRiskAppView", "setListOpenImageStatus", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                setObjectField(param.getThisObject(), "riskModelList", new ArrayList<>());
            }
        });
        findAndHookMethod("com.xiaomi.market.business_ui.main.mine.app_security.AppSecurityCheckManager", "getSecurityResultTxtByPage", "com.xiaomi.market.common.component.componentbeans.TotalResult", int.class, int.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.getArgs()[0] = getStaticObjectField(findClassIfExists("com.xiaomi.market.common.component.componentbeans.TotalResult"), "TOTAL_EXCELLENT");
                param.getArgs()[2] = 0;
            }
        });
    }
}
