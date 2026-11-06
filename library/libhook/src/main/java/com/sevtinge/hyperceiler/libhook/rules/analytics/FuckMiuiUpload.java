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
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
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

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

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

        // 为什么改锚点（在 OS3.0.319 / OS4.0.0.44 / OS4.0.5.0 三版 AnalyticsCore 上离线复算过，
        // 脚本 netutil_verify.py，结果 netutil_verify_3ver_20261106.txt）：
        // DexKit 2.2.0 的 usingStrings(String...) 默认是 StringMatchType.Contains（不是等值），
        // 所以 "NetworkUtil" 会同时命中日志标签为 "NetworkUtils" 的那个类：
        //   OS3          -> 1 个类（com.miui.analytics.onetrack.q.d，只有 4 个方法，一切正常）
        //   OS4.0.0.44   -> 2 个类（a.b.a.a.a.c.a 是被 R8 合并出来的 95 方法大杂烩工具类，
        //                          a.b.a.c.v.k 的标签是 "NetworkUtils"）
        //   OS4.0.5      -> 2 个类（同上，合并类 96 方法）
        // 两个候选让 singleOrNull() 返回 null，再进 DexKitCacheManager 就变成一条与规则无关的
        // NullPointerException("Object.getClass() on null")，整条规则被跳过，连本来有效的
        // usage json 拦截也一起失效。
        // "isNetworkConnected exception" 这条日志串在三版里都只出现在真正的 NetworkUtil 里
        // （OS3: onetrack.q.d#c；OS4.0.0.44: a.b.a.a.a.c.a#k0；OS4.0.5: 同类#l0），
        // 用它做类级锚点即可唯一定位；注意 44 与 405 的混淆名整体右移一位（k0/l0、U/V），
        // 所以不能按方法名定位，只能用日志串这类语义锚点。
        // 这里不用 "NetworkUtil" + 结构约束叠加，是因为多条 usingStrings 的与/或语义不该依赖。
        // 同时全部降级为 optional：即便将来再漂移，也只丢网络相关的附加 hook，
        // 不连带本规则的核心（UsageJsonProcesser 拦截）。
        mNetworkUtil = optionalMember("NetworkUtil", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData clazzData = bridge.findClass(FindClass.create()
                    .matcher(ClassMatcher.create()
                        .usingStrings("isNetworkConnected exception")
                    )).singleOrNull();
                return clazzData;
            }
        });

        // 只按 (Context)int 约束，在 OS4.0.0.44 / OS4.0.5 上会唯一命中 getCoreVersionCode()
        // （同一个合并类里的 #K，日志串是 getCoreVersionCode exception），把它的返回值改写成 0 是错的。
        // 加 "2G" 锚点后：
        //   OS3          -> onetrack.q.d#a(Context)I（网络类型，正确）
        //   OS4.0.0.44/05 -> 0 命中（合并类里已没有带网络类型字符串的 (Context)int 方法）
        //              -> 跳过这一条，不误伤 getCoreVersionCode。
        mNetworkA = mNetworkUtil == null ? null : optionalMember("NetworkA", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .declaredClass(mNetworkUtil)
                        .paramTypes(Context.class)
                        .returnType(int.class)
                        .usingStrings("2G")
                    )).singleOrNull();
                return methodData;
            }
        });

        mNetworkB = mNetworkUtil == null ? null : optionalMember("NetworkB", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    // OS4.0.0.44 / OS4.0.5 的合并类里各有 7 个 (Context)String 方法（版本名、fid、路径…），
                    // 光靠签名会歧义 -> singleOrNull() 返回 null。"2G" 在三版里都只在
                    // 网络类型描述方法里出现（OS3: #b；44: #U；405: #V），可唯一定位。
                    .matcher(MethodMatcher.create()
                        .declaredClass(mNetworkUtil)
                        .paramTypes(Context.class)
                        .returnType(String.class)
                        .usingStrings("2G")
                    )).singleOrNull();
                return methodData;
            }
        });

        mNetworkC = mNetworkUtil == null ? null : optionalMember("NetworkC", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    // OS4.0.0.44 / OS4.0.5 的合并类里各有 2 个 ()Z 方法（一个是 Looper.myLooper()==mainLooper，
                    // 语义完全不同），光靠签名会歧义。用 isNetworkConnected 的日志串定位。
                    .matcher(MethodMatcher.create()
                        .declaredClass(mNetworkUtil)
                        .paramCount(0)
                        .returnType(boolean.class)
                        .usingStrings("isNetworkConnected exception")
                    )).singleOrNull();
                return methodData;
            }
        });

        return true;
    }

    /**
     * 可选成员（optionalMember 可能返回 null）的统一挂载入口：
     * 查不到就跳过这一条 hook，而不是把整条规则带崩。
     */
    private static void hookIfPresent(Method method, IMethodHook callback) {
        if (method != null) {
            hookMethod(method, callback);
        }
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

        hookIfPresent(mNetworkA, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(0);
            }
        });

        hookIfPresent(mNetworkB, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult("NONE");
            }
        });

        hookIfPresent(mNetworkC, new IMethodHook() {
            @Override
            public void before(HookParam param) {
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

