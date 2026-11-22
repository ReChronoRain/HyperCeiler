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
        // 改动日志 (2026.11.06)：
        // Method1 的锚点串在 OS3.0.319 / OS4.0.0.44 / OS4.0.5.0 三版里都只命中 1 个方法（B1/a#b 与 x3/a#b），
        // 保持不动；但 requiredMember 在查不到时会抛异常，把同一条规则里与 DexKit 完全无关的反射 Hook 一并带坑，
        // 所以降为 optionalMember，并在 init() 里逐个判空。
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
                // 原查询（usingStrings("fillYellowPageContact: ", "ContactFetcher") + void + 2 参）只在 OS3 命中；
                // OS4.0.0.44 与 OS4.0.5.0 里“ContactFetcher”这个日志标签已被移除，两串 AND 后 0 命中，
                // 整条规则被跳过。改用“类名 + 方法名 + 精确入参类型 + void”这个结构不变量：
                // YellowpageContactFetcher#fillYellowPageContact(SmartContact, YellowPagePhone)V 在三版里名字与签名均未漂。
                // 已在三版反汇编上离线复算：三版均收敛为 1 个命中。
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .declaredClass("com.miui.smsextra.internal.sdk.xiaomi.YellowpageContactFetcher")
                        .name("fillYellowPageContact")
                        .paramTypes("com.miui.smsextra.sdk.SmartContact",
                                "com.miui.smsextra.internal.sdk.xiaomi.YellowPagePhone")
                        .returnType(void.class)
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
                if (getObjectField(param.getArgs()[0], "mRiskType") == "11" && PrefsBridge.getBoolean("mms_disable_overseas_risk_tip")) setObjectField(param.getArgs()[0], "mRiskType", ""); setObjectField(param.getArgs()[0], "mRiskType", "");
                if (getObjectField(param.getArgs()[0], "mRiskType") == "12" && PrefsBridge.getBoolean("mms_disable_fraud_risk_tip")) setObjectField(param.getArgs()[0], "mRiskType", ""); setObjectField(param.getArgs()[0], "mRiskType", "");
                // XposedLog.d("smsrisk 2 g3.a "+getObjectField(param.getArgs()[0], "mRiskType"));
            }
        });
        if (mMethod2 != null) hookMethod(mMethod2, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                // XposedLog.d("smsrisk n6.p "+getObjectField(param.getArgs()[0], "mRiskType"));
                // 不知道为什么set两遍才能跑，先留在这里吧
                if (getObjectField(param.getArgs()[0], "mRiskType") == "11" && PrefsBridge.getBoolean("mms_disable_overseas_risk_tip")) setObjectField(param.getArgs()[0], "mRiskType", ""); setObjectField(param.getArgs()[0], "mRiskType", "");
                if (getObjectField(param.getArgs()[0], "mRiskType") == "12" && PrefsBridge.getBoolean("mms_disable_fraud_risk_tip")) setObjectField(param.getArgs()[0], "mRiskType", ""); setObjectField(param.getArgs()[0], "mRiskType", "");
                // XposedLog.d("smsrisk 2 n6.p "+getObjectField(param.getArgs()[0], "mRiskType"));
            }
        });
    }
}
