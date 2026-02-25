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

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;


public class DisableRiskTip extends BaseHook {
    @Override
    public void init() {
        Method method1 = DexKit.findMember("Method1", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings("fillSmartContactByB2c: old smart contact is not null")
                    )).singleOrNull();
                return methodData;
            }
        });
        Method method2 = DexKit.findMember("Method2", new IDexKit() {
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
        findAndHookMethod("com.miui.smsextra.sdk.SmartContact", "isRiskyNumber", new IMethodHook()  {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(false);
            }
        });
        if (PrefsBridge.getBoolean("mms_disable_fraud_risk_tip")) findAndHookMethod("com.miui.smsextra.sdk.SmartContact", "isDefraudNumber", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(false);
            }
        });
        if (getPackageVersionCode(getLpparam()) >= 170000000) {
            findAndHookMethod("com.miui.smsextra.internal.sdk.xiaomi.YellowPagePhone", "isRiskyNumber", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(false);
                }
            });
            if (PrefsBridge.getBoolean("mms_disable_fraud_risk_tip"))
                findAndHookMethod("com.miui.smsextra.internal.sdk.xiaomi.YellowPagePhone", "isDefraudNumber", new IMethodHook()  {
                    @Override
                    public void before(BeforeHookParam param) {
                        param.setResult(false);
                    }
                });
        }
        hookMethod(method1, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                // XposedLog.d("smsrisk g3.a "+getObjectField(param.getArgs()[0], "mRiskType"));
                // 不知道为什么set两遍才能跑，先留在这里吧
                if (getObjectField(param.getArgs()[0], "mRiskType") == "11" && PrefsBridge.getBoolean("mms_disable_overseas_risk_tip")) setObjectField(param.getArgs()[0], "mRiskType", ""); setObjectField(param.getArgs()[0], "mRiskType", "");
                if (getObjectField(param.getArgs()[0], "mRiskType") == "12" && PrefsBridge.getBoolean("mms_disable_fraud_risk_tip")) setObjectField(param.getArgs()[0], "mRiskType", ""); setObjectField(param.getArgs()[0], "mRiskType", "");
                // XposedLog.d("smsrisk 2 g3.a "+getObjectField(param.getArgs()[0], "mRiskType"));
            }
        });
        hookMethod(method2, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                // XposedLog.d("smsrisk n6.p "+getObjectField(param.getArgs()[0], "mRiskType"));
                // 不知道为什么set两遍才能跑，先留在这里吧
                if (getObjectField(param.getArgs()[0], "mRiskType") == "11" && PrefsBridge.getBoolean("mms_disable_overseas_risk_tip")) setObjectField(param.getArgs()[0], "mRiskType", ""); setObjectField(param.getArgs()[0], "mRiskType", "");
                if (getObjectField(param.getArgs()[0], "mRiskType") == "12" && PrefsBridge.getBoolean("mms_disable_fraud_risk_tip")) setObjectField(param.getArgs()[0], "mRiskType", ""); setObjectField(param.getArgs()[0], "mRiskType", "");
                // XposedLog.d("smsrisk 2 n6.p "+getObjectField(param.getArgs()[0], "mRiskType"));
            }
        });
    }
}
