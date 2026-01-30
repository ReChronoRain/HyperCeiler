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

package com.sevtinge.hyperceiler.libhook.rules.securitycenter;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.newInstance;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class BypassAdbInstallVerify extends BaseHook {
    @Override
    public void init() {
        Method method1 = DexKit.findMember("AdbInstallNetworkVerify", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create()
                                        .usingStrings("https://srv.sec.miui.com/data/adb"))
                                .usingStrings("connectivity")
                        )).singleOrNull();
                return methodData;
            }
        });
        Method method2 = DexKit.findMember("AdbInstallCaller", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("AdbInstallActivity", "start request for adb install!")
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method1, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                Object instance = param.getThisObject();
                Object adbInstallVerifyActivityInstance = newInstance(findClass("com.miui.permcenter.install.AdbInstallVerifyActivity$b", getClassLoader()), instance);
                setObjectField(instance, "d", adbInstallVerifyActivityInstance);
                Object threadPoolExecutor = getStaticObjectField(findClass("android.os.AsyncTask", getClassLoader()), "THREAD_POOL_EXECUTOR");
                callMethod(adbInstallVerifyActivityInstance, "executeOnExecutor", threadPoolExecutor, new Void[]{});
                param.setResult(null);

            }
        });
        hookMethod(method2, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(null);
            }
        });
    }
}
