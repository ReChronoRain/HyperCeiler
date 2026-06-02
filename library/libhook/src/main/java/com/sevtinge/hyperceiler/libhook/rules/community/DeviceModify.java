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

package com.sevtinge.hyperceiler.libhook.rules.community;

import android.os.Build;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;


public class DeviceModify extends BaseHook {
    private Method mSystemPropertiesGetStringWithNullMethod;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    String mDevice;
    String mModel;
    String mManufacturer;

    @Override
    protected boolean initDexKit() {
        // com.xiaomi.vipbase.utils.SystemProperties.b(String, String)
        mSystemPropertiesGetStringWithNullMethod = requiredMember("SystemPropertiesGetStringWithNull", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                            .declaredClass("com.xiaomi.vipbase.utils.SystemProperties")
                            .paramCount(2)
                            .returnType(String.class)
                        )).singleOrNull();
                return methodData;
            }
        });
        return true;
    }

    @Override
    public void init() {

        mDevice = PrefsBridge.getString("community_device_modify_device", "");
        mModel = PrefsBridge.getString("community_device_modify_model", "");
        mManufacturer = PrefsBridge.getString("community_device_modify_manufacturer", "");

        // 覆盖直接读取 android.os.Build 静态字段的 SDK (账号、推送、market sdk 等)
        setStaticObjectField(Build.class, "DEVICE", mDevice);
        setStaticObjectField(Build.class, "MODEL", mModel);
        setStaticObjectField(Build.class, "MANUFACTURER", mManufacturer);

        // 拦截 App 自身的 SystemProperties 包装方法
        hookMethod(mSystemPropertiesGetStringWithNullMethod, new IMethodHook() {
            @Override
            public void before(@NonNull HookParam param) {
                Object arg0 = param.getArgs()[0];
                if (!(arg0 instanceof String key)) return;
                switch (key) {
                    case "ro.product.model":
                    case "ro.product.marketname":
                        param.setResult(mModel);
                        break;
                    case "ro.product.mod_device":
                    case "ro.product.device":
                        param.setResult(mDevice);
                        break;
                    case "ro.product.manufacturer":
                        param.setResult(mManufacturer);
                        break;
                }
            }
        });

    }
}
