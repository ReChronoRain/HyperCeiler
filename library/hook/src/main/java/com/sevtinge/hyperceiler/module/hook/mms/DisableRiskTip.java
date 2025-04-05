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

package com.sevtinge.hyperceiler.module.hook.mms;

import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

public class DisableRiskTip extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
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
                                .usingStrings("11", "460", "12", "")
                                .returnType(void.class)
                                .paramTypes("com.miui.smsextra.sdk.SmartContact", "")
                        )).singleOrNull();
                return methodData;
            }
        });
        findAndHookMethod("com.miui.smsextra.sdk.SmartContact", "isRiskyNumber", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
        hookMethod(method1, new MethodHook(){
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                // logD("smsrisk g3.a "+getObjectField(param.args[0], "mRiskType"));
                // 不知道为什么set两遍才能跑，先留在这里吧
                if (getObjectField(param.args[0], "mRiskType") == "11" && mPrefsMap.getBoolean("mms_disable_overseas_risk_tip")) setObjectField(param.args[0], "mRiskType", ""); setObjectField(param.args[0], "mRiskType", "");
                if (getObjectField(param.args[0], "mRiskType") == "12" && mPrefsMap.getBoolean("mms_disable_fraud_risk_tip")) setObjectField(param.args[0], "mRiskType", ""); setObjectField(param.args[0], "mRiskType", "");
                // logD("smsrisk 2 g3.a "+getObjectField(param.args[0], "mRiskType"));
            }
        });
        hookMethod(method2, new MethodHook(){
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                // logD("smsrisk n6.p "+getObjectField(param.args[0], "mRiskType"));
                // 不知道为什么set两遍才能跑，先留在这里吧
                if (getObjectField(param.args[0], "mRiskType") == "11" && mPrefsMap.getBoolean("mms_disable_overseas_risk_tip")) setObjectField(param.args[0], "mRiskType", ""); setObjectField(param.args[0], "mRiskType", "");
                if (getObjectField(param.args[0], "mRiskType") == "12" && mPrefsMap.getBoolean("mms_disable_fraud_risk_tip")) setObjectField(param.args[0], "mRiskType", ""); setObjectField(param.args[0], "mRiskType", "");
                // logD("smsrisk 2 n6.p "+getObjectField(param.args[0], "mRiskType"));
            }
        });
    }
}
