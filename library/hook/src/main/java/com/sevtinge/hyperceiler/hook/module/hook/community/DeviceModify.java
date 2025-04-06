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

package com.sevtinge.hyperceiler.hook.module.hook.community;

import android.os.Build;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

public class DeviceModify extends BaseHook {

    String mDevice;
    String mModel;
    String mManufacturer;

    @Override
    public void init() throws NoSuchMethodException {

        mDevice = mPrefsMap.getString("community_device_modify_device", "");
        mModel = mPrefsMap.getString("community_device_modify_model", "");
        mManufacturer = mPrefsMap.getString("community_device_modify_manufacturer", "");

        XposedHelpers.setStaticObjectField(Build.class, "DEVICE", mDevice);
        XposedHelpers.setStaticObjectField(Build.class, "MODEL", mModel);
        XposedHelpers.setStaticObjectField(Build.class, "MANUFACTURER", mManufacturer);

        Method method1 = DexKit.findMember("SystemPropertiesGetStringWithNull", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass("com.xiaomi.vipbase.utils.SystemProperties")
                                .paramCount(1)
                                .returnType(String.class)
                        )).singleOrNull();
                return methodData;
            }
        });
        Method method2 = DexKit.findMember("GetDevice", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("ro.product.device")
                        )).singleOrNull();
                return methodData;
            }
        });
        Method method3 = DexKit.findMember("GetModel", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("ro.product.model")
                        )).singleOrNull();
                return methodData;
            }
        });
        Method method4 = DexKit.findMember("GetManufacturer", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("ro.product.manufacturer")
                        )).singleOrNull();
                return methodData;
            }
        });

        hookMethod(method1, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if (param.args[0] == "ro.product.model") {
                    param.setResult(mModel);
                } else if (param.args[0] == "ro.product.device") {
                    param.setResult(mDevice);
                } else if (param.args[0] == "ro.product.manufacturer") {
                    param.setResult(mManufacturer);
                }
            }
        });
        hookMethod(method2, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(mDevice);
            }
        });
        hookMethod(method3, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(mModel);
            }
        });
        hookMethod(method4, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(mManufacturer);
            }
        });

    }
}
