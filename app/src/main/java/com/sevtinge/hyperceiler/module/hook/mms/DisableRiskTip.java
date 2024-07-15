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

package com.sevtinge.hyperceiler.module.hook.mms;

import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DisableRiskTip extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.smsextra.sdk.SmartContact", "isRiskyNumber", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
        findAndHookMethod("g3.a", "c", "com.miui.smsextra.sdk.SmartContact", "com.miui.smsextra.sdk.SmartContact", new MethodHook(){
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                // logD("smsrisk g3.a "+getObjectField(param.args[0], "mRiskType"));
                // 不知道为什么set两遍才能跑，先留在这里吧
                if (getObjectField(param.args[0], "mRiskType") == "11" && mPrefsMap.getBoolean("mms_disable_overseas_risk_tip")) setObjectField(param.args[0], "mRiskType", ""); setObjectField(param.args[0], "mRiskType", "");
                if (getObjectField(param.args[0], "mRiskType") == "12" && mPrefsMap.getBoolean("mms_disable_fraud_risk_tip")) setObjectField(param.args[0], "mRiskType", ""); setObjectField(param.args[0], "mRiskType", "");
                // logD("smsrisk 2 g3.a "+getObjectField(param.args[0], "mRiskType"));
            }
        });
        findAndHookMethod("n6.p", "a", "com.miui.smsextra.sdk.SmartContact", "n6.o", new MethodHook(){
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
