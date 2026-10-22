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

package com.sevtinge.hyperceiler.libhook.rules.mms;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getPackageVersionCode;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;


public class DisableRiskTip extends BaseHook {
    private Method mMethod1;
    private Method mMethod2;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mMethod1 = optionalMember("Method1", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings("fillSmartContactByB2c: old smart contact is not null")
                    )).singleOrNull();
                return methodData;
            }
        });
        mMethod2 = optionalMember("Method2", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings("fillYellowPageContact: ", "ContactFetcher")
                        .returnType(void.class)
                        .paramCount(2)
                    )).singleOrNull();
                return methodData;
            }
        });
        return true;
    }

    @Override
    public void init() {
        findAndHookMethod("com.miui.smsextra.sdk.SmartContact", "isRiskyNumber", new IMethodHook()  {
            @Override
            public void before(HookParam param) {
                param.setResult(false);
            }
        });
        if (PrefsBridge.getBoolean("mms_disable_fraud_risk_tip")) findAndHookMethod("com.miui.smsextra.sdk.SmartContact", "isDefraudNumber", new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(false);
            }
        });
        if (getPackageVersionCode(getLpparam()) >= 170000000) {
            findAndHookMethod("com.miui.smsextra.internal.sdk.xiaomi.YellowPagePhone", "isRiskyNumber", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    param.setResult(false);
                }
            });
            if (PrefsBridge.getBoolean("mms_disable_fraud_risk_tip"))
                findAndHookMethod("com.miui.smsextra.internal.sdk.xiaomi.YellowPagePhone", "isDefraudNumber", new IMethodHook()  {
                    @Override
                    public void before(HookParam param) {
                        param.setResult(false);
                    }
                });
        }
        if (mMethod1 != null) hookMethod(mMethod1, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                // XposedLog.d("smsrisk g3.a "+getObjectField(param.getArgs()[0], "mRiskType"));
                // 不知道为什么set两遍才能跑，先留在这里吧
                clearEnabledRiskType(param.getArgs()[0]);
                // XposedLog.d("smsrisk 2 g3.a "+getObjectField(param.getArgs()[0], "mRiskType"));
            }
        });
        if (mMethod2 != null) hookMethod(mMethod2, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                // XposedLog.d("smsrisk n6.p "+getObjectField(param.getArgs()[0], "mRiskType"));
                // 不知道为什么set两遍才能跑，先留在这里吧
                clearEnabledRiskType(param.getArgs()[0]);
                // XposedLog.d("smsrisk 2 n6.p "+getObjectField(param.getArgs()[0], "mRiskType"));
            }
        });
    }

    private void clearEnabledRiskType(Object contact) {
        Object type = getObjectField(contact, "mRiskType");
        if (("11".equals(type) && PrefsBridge.getBoolean("mms_disable_overseas_risk_tip"))
            || ("12".equals(type) && PrefsBridge.getBoolean("mms_disable_fraud_risk_tip"))) {
            setObjectField(contact, "mRiskType", "");
        }
    }
}
