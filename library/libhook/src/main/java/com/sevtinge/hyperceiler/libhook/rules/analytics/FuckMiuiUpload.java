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

package com.sevtinge.hyperceiler.libhook.rules.analytics;

import android.content.Context;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKitList;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.BaseDataList;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;
import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

// from https://github.com/gykkuo/MIUI_NoGuard/blob/master/app/src/main/java/cn/fyyr/noguardpls/MainHook.java
public class FuckMiuiUpload extends BaseHook {

    private List<Method> mUsageJsonProcessers;

    private Class<?> mNetworkUtil;

    private Method mNetworkA;
    private Method mNetworkB;
    private Method mNetworkC;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mUsageJsonProcessers = requiredMemberList("UsageJsonProcesser", new IDexKitList() {
            @Override
            public BaseDataList<MethodData> dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodDataList methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .usingStrings("Don't collect usage below Android KK 4.4")
                    )
                );
                return methodData;
            }
        });

        mNetworkUtil = requiredMember("NetworkUtil", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData clazzData = bridge.findClass(FindClass.create()
                    .matcher(ClassMatcher.create()
                        .usingStrings("NetworkUtil")
                    )).singleOrNull();
                return clazzData;
            }
        });

        mNetworkA = requiredMember("NetworkA", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .declaredClass(mNetworkUtil)
                        .paramTypes(Context.class)
                        .returnType(int.class)
                    )).singleOrNull();
                return methodData;
            }
        });

        mNetworkB = requiredMember("NetworkB", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .declaredClass(mNetworkUtil)
                        .paramTypes(Context.class)
                        .returnType(String.class)
                    )).singleOrNull();
                return methodData;
            }
        });

        mNetworkC = requiredMember("NetworkC", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .declaredClass(mNetworkUtil)
                        .paramCount(0)
                        .returnType(boolean.class)
                    )).singleOrNull();
                return methodData;
            }
        });

        return true;
    }

    @Override
    public void init() {

        for (Method method : mUsageJsonProcessers) {
            try {
                hookMethod(method, new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        param.setResult(null);
                    }
                });
            } catch (Exception e) {
                XposedLog.e(TAG, getPackageName(), e);
            }
        }

        hookMethod(mNetworkA, new IMethodHook() {
            @Override
            public void before(HookParam param) throws Throwable {
                param.setResult(0);
            }
        });

        hookMethod(mNetworkB, new IMethodHook() {
            @Override
            public void before(HookParam param) throws Throwable {
                param.setResult("NONE");
            }
        });

        hookMethod(mNetworkC, new IMethodHook() {
            @Override
            public void before(HookParam param) throws Throwable {
                param.setResult(false);
            }
        });

        /*int needmod = 0;
        Class<?> classs = null;
        // com.miui.analytics.onetrack.p.x.w
        // com.miui.analytics.onetrack.p.f0.x(JJZ)
        try {
            classs = findClass("com.miui.analytics.onetrack.p.u");
            findAndHookMethod(classs, "w", long.class, long.class, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    param.setResult(null);
                }
            });

            needmod++;
        } catch (NoSuchMethodError err) {
            XposedLog.e(TAG, getPackageName(), err.toString());
        }
        // com.miui.analytics.c.f.l.y
        // com.miui.analytics.d.f.o.y
        try {
            classs = findClass("com.miui.analytics.c.f.k");
            findAndHookMethod(classs, "y", long.class, long.class, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    // XposedBridge.log("com.miui.analytics:执行拦截！ --- 2");
                    param.setResult(null);
                }
            });
            needmod++;
        } catch (NoSuchMethodError err) {
            XposedLog.e(TAG, getPackageName(), err.toString());
        }
        try {
            classs = findClass("com.miui.analytics.onetrack.q.c");
            // com.miui.analytics.onetrack.q.c.a
            try {
                findAndHookMethod(classs, "a", Context.class, new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        // XposedBridge.log("com.miui.analytics:执行拦截！ --- 3");
                        param.setResult(0);
                    }
                });
                needmod++;
            } catch (NoSuchMethodError err) {
                XposedLog.e(TAG, getPackageName(), err.toString());
            }
            // com.miui.analytics.onetrack.q.c.b
            try {
                findAndHookMethod(classs, "b", Context.class, new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        // XposedBridge.log("com.miui.analytics:执行拦截！ --- 4");
                        param.setResult("NONE");
                    }
                });
                needmod++;
            } catch (NoSuchMethodError err) {
                XposedLog.e(TAG, getPackageName(), err.toString());
            }
            // com.miui.analytics.onetrack.q.c.c
            try {
                findAndHookMethod(classs, "c", new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        // XposedBridge.log("com.miui.analytics:执行拦截！ --- 5");
                        param.setResult(false);
                    }
                });
                needmod++;
            } catch (NoSuchMethodError err) {
                XposedLog.e(TAG, getPackageName(), err.toString());
            }
        } catch (NoSuchMethodError err) {
            XposedLog.e(TAG, getPackageName(), err.toString());
        }*/
    }
}

