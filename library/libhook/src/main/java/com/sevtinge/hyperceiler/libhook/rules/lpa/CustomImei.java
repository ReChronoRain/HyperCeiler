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

package com.sevtinge.hyperceiler.libhook.rules.lpa;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;



public class CustomImei extends BaseHook {
    @Override
    public void init() {
        Method method = DexKit.findMember("GetImei", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("phone", "F")
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                // 傻逼小米你写了个什么
                param.setResult(processIMEI(mPrefsMap.getString("lpa_custom_imei", "")));
            }
            @Override
            public void after(AfterHookParam param) {
                XposedLog.e(TAG, getPackageName(), "pr = " + param.getResult());
            }
        });
    }

    private static String processIMEI(String imei) {
        imei = imei.replaceAll("[^0123456789F]", "");
        // if (imei.length() % 2 != 0) return imei + "F"; else return imei;
        // 小米的逻辑，我不李姐但大受震撼
        StringBuilder sb = new StringBuilder(16);
        int i = 0;
        if (imei.length() % 2 == 0) {
            while (i < imei.length()) {
                sb.append(imei.charAt(i + 1));
                sb.append(imei.charAt(i));
                i += 2;
            }
            return sb.toString();
        }
        String str = imei + "F";
        while (i < str.length() - 2) {
            sb.append(str.charAt(i + 1));
            sb.append(str.charAt(i));
            i += 2;
        }
        sb.append(str.substring(str.length() - 2));
        return sb.toString();
    }
}
