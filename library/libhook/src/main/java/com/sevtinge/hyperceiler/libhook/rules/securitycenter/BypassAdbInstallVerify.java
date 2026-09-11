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

import android.os.AsyncTask;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class BypassAdbInstallVerify extends BaseHook {
    private Method mAdbInstallNetworkVerifyMethod;
    private Method mAdbInstallCallerMethod;
    private Class<?> mAdbInstallTaskClass;
    private String mAdbInstallTaskField;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mAdbInstallNetworkVerifyMethod = requiredMember("AdbInstallNetworkVerify", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                // CN and Global use different server URLs. The host
                                // activity and connectivity method signature are shared.
                                .declaredClass("com.miui.permcenter.install.AdbInstallVerifyActivity")
                                .usingStrings("connectivity")
                                .paramCount(0)
                                .returnType(void.class)
                        )).singleOrNull();
                return methodData;
            }
        });
        mAdbInstallCallerMethod = requiredMember("AdbInstallCaller", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("AdbInstallActivity", "start request for adb install!")
                        )).singleOrNull();
                return methodData;
            }
        });
        mAdbInstallTaskClass = mAdbInstallCallerMethod.getDeclaringClass();
        Class<?> activityClass = mAdbInstallNetworkVerifyMethod.getDeclaringClass();
        if (!AsyncTask.class.isAssignableFrom(mAdbInstallTaskClass)) {
            throw new IllegalStateException("ADB verification caller is not an AsyncTask");
        }
        try {
            mAdbInstallTaskClass.getDeclaredConstructor(activityClass);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("ADB verification task constructor not found", e);
        }
        // R8 renamed d (CN) to e (Global); d is a URL String on Global.
        // Resolve by the verified task type, rejecting an ambiguous host layout.
        String taskFieldName = null;
        for (Field field : activityClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && field.getType() == mAdbInstallTaskClass) {
                if (taskFieldName != null) {
                    throw new IllegalStateException("Multiple ADB verification task fields");
                }
                taskFieldName = field.getName();
            }
        }
        if (taskFieldName == null) {
            throw new IllegalStateException("ADB verification task field not found");
        }
        mAdbInstallTaskField = taskFieldName;
        return true;
    }

    @Override
    public void init() {
        hookMethod(mAdbInstallNetworkVerifyMethod, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                Object instance = param.getThisObject();
                Object adbInstallVerifyActivityInstance = newInstance(mAdbInstallTaskClass, instance);
                setObjectField(instance, mAdbInstallTaskField, adbInstallVerifyActivityInstance);
                Object threadPoolExecutor = getStaticObjectField(findClass("android.os.AsyncTask", getClassLoader()), "THREAD_POOL_EXECUTOR");
                callMethod(adbInstallVerifyActivityInstance, "executeOnExecutor", threadPoolExecutor, new Void[]{});
                param.setResult(null);
                XposedLog.d(TAG, getPackageName(), "Started ADB verification task using field " + mAdbInstallTaskField);
            }
        });
        hookMethod(mAdbInstallCallerMethod, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(null);
                XposedLog.d(TAG, getPackageName(), "Skipped ADB network verification request");
            }
        });
    }
}
