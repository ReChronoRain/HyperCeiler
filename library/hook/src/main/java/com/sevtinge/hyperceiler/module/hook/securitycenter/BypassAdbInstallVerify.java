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

package com.sevtinge.hyperceiler.module.hook.securitycenter;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

public class BypassAdbInstallVerify extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
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
        hookMethod(method1, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object instance = param.thisObject;
                Object adbInstallVerifyActivityInstance = XposedHelpers.newInstance(XposedHelpers.findClass("com.miui.permcenter.install.AdbInstallVerifyActivity$b", lpparam.classLoader), instance);
                XposedHelpers.setObjectField(instance, "d", adbInstallVerifyActivityInstance);
                Object threadPoolExecutor = XposedHelpers.getStaticObjectField(XposedHelpers.findClass("android.os.AsyncTask", lpparam.classLoader), "THREAD_POOL_EXECUTOR");
                XposedHelpers.callMethod(adbInstallVerifyActivityInstance, "executeOnExecutor", threadPoolExecutor, new Void[]{});
                param.setResult(null);

            }
        });
        hookMethod(method2, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
    }
}
