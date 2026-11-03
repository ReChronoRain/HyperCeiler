/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.navigation;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.view.Display;
import android.view.Surface;
import android.view.View;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.libxposed.api.XposedInterface;

public class RotationButton extends BaseHook {
    private static final ThreadLocal<Boolean> sForceAttachedToWindow = new ThreadLocal<>();
    private static final String STATE_NAVIGATION_BAR = "RotationButton.navigationBar";
    private static final String STATE_CONTEXT = "RotationButton.context";

    /**
     * NavigationBar / NavigationBarView 在较新的系统里被挪到了 views 子包下。
     * 这里不按版本判断，而是把两个候选路径都试一遍，谁真的存在就用谁，
     * 因此同一份产物在挪包前后的系统上都能拿到同一个类。
     */
    private static final String[] NAV_BAR_CANDIDATES = {
        "com.android.systemui.navigationbar.views.NavigationBar",
        "com.android.systemui.navigationbar.NavigationBar",
    };
    private static final String[] NAV_BAR_VIEW_CANDIDATES = {
        "com.android.systemui.navigationbar.views.NavigationBarView",
        "com.android.systemui.navigationbar.NavigationBarView",
    };
    private static final String ROTATION_CONTROLLER =
        "com.android.systemui.shared.rotation.RotationButtonController";
    /** 脱糖（desugar）类的编号上界：只用于限制穷举范围，不作为任何版本判据 */
    private static final int MAX_LAMBDA_INDEX = 64;
    /** 编译期脱糖出来的 lambda 类统一带这个前缀，编号本身会随版本漂移 */
    private static final String DESUGARED_PREFIX = "$$ExternalSyntheticLambda";
    boolean isListen = false;
    boolean enable = PrefsBridge.getStringAsInt("system_framework_other_rotation_button_int", 0) != 1;

    @Override
    public void init() {
        if (enable) {
            Object restoredNavigationBar = getHotReloadRuntimeState(
                STATE_NAVIGATION_BAR, Object.class
            );
            Context restoredContext = getHotReloadRuntimeState(STATE_CONTEXT, Context.class);
            if (restoredNavigationBar != null && restoredContext != null) {
                ensureRotationObserver(restoredNavigationBar, restoredContext);
            }
        }

        Class<?> navigationBarClass = findFirstClass(NAV_BAR_CANDIDATES);
        if (navigationBarClass != null) {
            hookAllConstructors(navigationBarClass,
                new IMethodHook() {
                    @Override
                    public void after(HookParam param) {
                        if (!enable) return;
                        ensureRotationObserver(param.getThisObject(), resolveContext(param.getThisObject()));
                    }
                }
            );
        }

        hookOrientationSupplier();

        // 下面每个挂钩点各自独立注册、各自兜底：某一点在这个版本上不存在，只跳过它自己，
        // 绝不连带后面的注册一起失败（框架层在类缺失时返回 null、方法缺失时抛异常，
        // 两种情况都不能拿来当流程控制）。
        tryHook("CommandQueue#onProposedRotationChanged", () ->
            findAndHookMethod("com.android.systemui.statusbar.CommandQueue",
                "onProposedRotationChanged", int.class, boolean.class,
                new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        if (!enable) {
                            return;
                        }
                        Context context = resolveContext(param.getThisObject());
                        if (context == null) {
                            // 宿主自身没有 Context 字段时才退回那个 handler 对象，仍按类型解析
                            try {
                                context = resolveContext(getObjectField(param.getThisObject(), "mDumpHandler"));
                            } catch (Throwable ignored) {
                            }
                        }
                        if (context != null) {
                            setData(context, param.getArgs()[0] + "," + param.getArgs()[1]);
                        }
                        param.setResult(null);
                    }
                }));

        // 首选链式（chain）：让 NavigationBar 自己的 onRotationProposal 照常执行，
        // 只在它里面临时把"是否已附加到窗口"判成真，这样未附加时方向建议也能出来。
        boolean attachedChained = tryHook("View#isAttachedToWindow", () ->
            findAndChainMethod(View.class, "isAttachedToWindow",
                chain -> {
                    if (Boolean.TRUE.equals(sForceAttachedToWindow.get())) {
                        return true;
                    }
                    return chain.proceed();
                }));

        boolean proposalChained = navigationBarClass != null && tryHook("NavigationBar#onRotationProposal", () ->
            findAndChainMethod(navigationBarClass,
                "onRotationProposal", int.class, boolean.class,
                (XposedInterface.Hooker) chain -> {
                    if (!enable) {
                        return null;
                    }
                    sForceAttachedToWindow.set(true);
                    try {
                        return chain.proceed();
                    } finally {
                        sForceAttachedToWindow.remove();
                    }
                }));

        if (!(attachedChained && proposalChained)) {
            // 链式那一条路没走通，才退回最省事的办法：直接把方向建议吞掉
            IMethodHook swallowProposal = new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    if (enable) {
                        param.setResult(null);
                    }
                }
            };
            tryHook("RotationButtonController#onRotationProposal", () ->
                findAndHookMethod(ROTATION_CONTROLLER, "onRotationProposal",
                    int.class, boolean.class, swallowProposal));
            if (navigationBarClass != null) {
                tryHook("NavigationBar#onRotationProposal(swallow)", () ->
                    findAndHookMethod(navigationBarClass, "onRotationProposal",
                        int.class, boolean.class, swallowProposal));
            }
        }

        hookRotateSuggestionClick();
    }

    /** 把方向建议改写成当前屏幕方向对应的值；拿不到上下文或方向未知就不动，交给原方法 */
    private void applyForcedOrientation(HookParam param, @Nullable Object owner) {
        if (owner == null) {
            return;
        }
        Context lightContext = resolveContext(owner, "mLightContext");
        if (lightContext == null) {
            return;
        }
        int intValue = switch (getScreenOrientation(lightContext)) {
            case 0 -> 1;
            case 1 -> 0;
            default -> -1;
        };
        if (intValue == -1) {
            XposedLog.e(TAG, "Unknown parameters, unable to continue execution, execute the original method!");
            return;
        }
        param.setResult(intValue);
    }

    /**
     * 方向建议按钮的取值来自一个 lambda。编译器可能把它内联回宿主类（形如 lambda$new$N），
     * 也可能脱糖成独立的 $$ExternalSyntheticLambdaN 类，两种形态的编号都会随版本漂移，
     * 所以两种形态都按结构特征去找，而不是写死某一个名字或编号。
     */
    private void hookOrientationSupplier() {
        IMethodHook forceOrientation = new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (enable) {
                    applyForcedOrientation(param, param.getThisObject());
                }
            }
        };
        for (String candidate : NAV_BAR_VIEW_CANDIDATES) {
            if (hookOrientationFrom(candidate, forceOrientation)) {
                break;
            }
        }
    }

    /** 在单个候选类上依次试内联与脱糖两种形态，挂上了返回 true */
    private boolean hookOrientationFrom(String candidate, IMethodHook forceOrientation) {
        Class<?> viewClass = findClassIfExists(candidate);
        if (viewClass == null) {
            return false;
        }
        if (hookInlineOrientation(viewClass, forceOrientation)) {
            return true;
        }
        // 形态二：脱糖成独立类。三条结构特征同时成立才算命中：
        //   ① 实现 java.util.function.Supplier（接口是编译器写死的，最稳）
        //   ② 有且只有一个无参 get()
        //   ③ 捕获字段里有一个的类型正是宿主类本身
        // 在 44 与 405 的 dexdump 上，NavigationBarView 的 4 个脱糖类里
        // 只有 1 个满足（其余是 Consumer / Runnable），所以不存在挑错对象的可能。
        DesugaredLambda supplier = findDesugaredLambda(viewClass,
            java.util.function.Supplier.class, "get", new Class<?>[0]);
        if (supplier == null) {
            return false;
        }
        hookMethod(supplier.method, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (!enable) {
                    return;
                }
                applyForcedOrientation(param, getObjectField(param.getThisObject(), supplier.capturedField));
            }
        });
        return true;
    }

    /**
     * 形态一：lambda 仍留在宿主类里。特征是合成方法名带 lambda$ 前缀（不猜编号，避免误挂普通 getter），
     * 无参数，返回 int。返回类型既可能是原始 int，也可能是装箱后的 Integer / Object，只认 int 会漏。
     */
    private boolean hookInlineOrientation(Class<?> viewClass, IMethodHook forceOrientation) {
        boolean hooked = false;
        for (Method method : viewClass.getDeclaredMethods()) {
            if (isInlineOrientationSupplier(method)) {
                hookMethod(method, forceOrientation);
                hooked = true;
            }
        }
        return hooked;
    }

    private static boolean isInlineOrientationSupplier(Method method) {
        Class<?> returnType = method.getReturnType();
        return method.getParameterTypes().length == 0
                && (returnType == int.class || returnType == Integer.class || returnType == Object.class)
                && method.getName().startsWith("lambda$");
    }

    /**
     * 旋转建议按钮的点击回调：先按方法名前缀找宿主类里的真实方法，
     * 找不到再退回脱糖出来的 onClick。两边都不写死参数表与编号。
     */
    private void hookRotateSuggestionClick() {
        Class<?> controllerClass = findClassIfExists(ROTATION_CONTROLLER);
        if (controllerClass == null) {
            return;
        }
        IMethodHook afterClick = new IMethodHook() {
            @Override
            public void after(HookParam param) {
                if (enable) {
                    hideRotateSuggestion(param.getThisObject());
                }
            }
        };
        if (!hookInlineClickCallbacks(controllerClass, afterClick)) {
            hookDesugaredClickCallback(controllerClass);
        }
    }

    /** 真实方法形态：名字前缀加“首参是 View”两条就够，其余参数表各版本不一，不写死 */
    private boolean hookInlineClickCallbacks(Class<?> controllerClass, IMethodHook afterClick) {
        boolean hooked = false;
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (isRotateSuggestionClick(method)) {
                hookMethod(method, afterClick);
                hooked = true;
            }
        }
        return hooked;
    }

    private static boolean isRotateSuggestionClick(Method method) {
        return method.getName().startsWith("onRotateSuggestionClick")
                && method.getParameterTypes().length > 0
                && method.getParameterTypes()[0] == View.class;
    }

    /**
     * 点进去时 R8 已经把 onRotateSuggestionClick 整个内联掉了（44 与 405 的 dexdump
     * 里都没有这个方法名，只在脱糖类的日志字符串里留下 "RotationButtonController#onRotateSuggestionClick"），
     * 所以真正要挂的是那个实现 View.OnClickListener、且捕获了控制器实例的类。
     */
    private void hookDesugaredClickCallback(Class<?> controllerClass) {
        DesugaredLambda click = findDesugaredLambda(controllerClass,
            View.OnClickListener.class, "onClick", new Class<?>[]{View.class});
        if (click == null) {
            XposedLog.w(TAG, "未定位到旋转建议的点击回调（内联形态与脱糖形态都没匹配上）");
            return;
        }
        hookMethod(click.method, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (!enable) {
                    return;
                }
                Object controller = getObjectField(param.getThisObject(), click.capturedField);
                if (controller != null) {
                    hideRotateSuggestion(controller);
                }
            }
        });
    }

    /** 一次结构匹配的产物：脱糖类本身、要挂的方法、以及持有宿主实例的捕获字段名 */
    private static final class DesugaredLambda {
        final Class<?> clazz;
        final Method method;
        final String capturedField;

        DesugaredLambda(Class<?> clazz, Method method, String capturedField) {
            this.clazz = clazz;
            this.method = method;
            this.capturedField = capturedField;
        }
    }

    /**
     * 穷举宿主类的脱糖类，按“函数式接口 + 方法名与参数表 + 捕获字段类型 = 宿主类”
     * 三条结构特征找目标。编号只是遍历边界，任何版本都不会因为编号变化而失效。
     * 命中多于一个时只取编号最小的那个，并把数量记进日志，便于换版本后复核。
     */
    private DesugaredLambda findDesugaredLambda(Class<?> ownerClass, Class<?> functionalInterface,
                                                String methodName, Class<?>[] paramTypes) {
        ClassLoader classLoader = ownerClass.getClassLoader();
        if (classLoader == null) {
            return null;
        }
        String ownerName = ownerClass.getName();
        DesugaredLambda matched = null;
        int hits = 0;
        for (int index = 0; index < MAX_LAMBDA_INDEX; index++) {
            DesugaredLambda candidate = matchDesugaredLambda(
                ownerName + DESUGARED_PREFIX + index, ownerClass, classLoader,
                functionalInterface, methodName, paramTypes);
            if (candidate != null) {
                hits++;
                if (matched == null) {
                    matched = candidate;
                }
            }
        }
        if (matched != null && hits > 1) {
            XposedLog.w(TAG, "结构匹配到 " + hits + " 个脱糖类，取用 " + matched.clazz.getName()
                + "（请复核这个版本上是否真有多个同形 lambda）");
        }
        return matched;
    }

    /** 单个编号的判定：四条结构特征全对上才算命中，否则按“这个编号不是目标”返回 null */
    @Nullable
    private DesugaredLambda matchDesugaredLambda(String lambdaName, Class<?> ownerClass, ClassLoader classLoader,
                                                 @Nullable Class<?> functionalInterface,
                                                 String methodName, Class<?>[] paramTypes) {
        Class<?> lambdaClass = loadClassQuietly(lambdaName, classLoader);
        if (lambdaClass == null) {
            return null;
        }
        if (functionalInterface != null && !functionalInterface.isAssignableFrom(lambdaClass)) {
            return null;
        }
        Method target = findMethodBySignature(lambdaClass, methodName, paramTypes);
        if (target == null) {
            return null;
        }
        String capturedFieldName = findCapturedField(lambdaClass, ownerClass);
        if (capturedFieldName == null) {
            return null;
        }
        return new DesugaredLambda(lambdaClass, target, capturedFieldName);
    }

    /** 绝大多数编号本来就不存在，取不到就当没有，不算错误 */
    @Nullable
    private static Class<?> loadClassQuietly(String className, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(className);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 按方法名加完整参数表比对，不认编号也不认参数个数的先后 */
    @Nullable
    private static Method findMethodBySignature(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        Method matched = null;
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName)
                    && Arrays.equals(method.getParameterTypes(), paramTypes)) {
                matched = method;
            }
        }
        return matched;
    }

    /**
     * 注册单个挂钩点：这个版本上没有目标（类缺失会返回 null、方法缺失会抛异常）时
     * 只记一条日志并跳过它，返回有没有挂上，不打断同一规则里的其它挂钩。
     */
    private boolean tryHook(String what, Runnable register) {
        try {
            register.run();
            return true;
        } catch (Throwable e) {
            XposedLog.w(TAG, "跳过挂钩 " + what + "：" + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * 按"类型"而不是"字段名"拿 Context：名字改成什么都不影响。
     * 先试 prefer 里的名字（拿到的是语义更准的那个，例如主题化的 mLightContext），
     * 再按声明顺序扫实例字段找第一个 Context。都拿不到就返回 null，由调用方跳过本次改写，
     * 绝不在 hook 里抛异常。
     */
    private Context resolveContext(@Nullable Object host, String... prefer) {
        if (host == null) {
            return null;
        }
        for (String name : prefer) {
            try {
                Object value = getObjectField(host, name);
                if (value instanceof Context context) {
                    return context;
                }
            } catch (Throwable ignored) {
                // 字段不存在时反射层会抛异常，这里按"没有这个字段"处理
            }
        }
        for (Class<?> clazz = host.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || !Context.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    if (field.get(host) instanceof Context context) {
                        return context;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    /** 收起旋转建议按钮：有的版本是 (boolean) 一个参数，有的是 (boolean, boolean) 两个，都试 */
    private void hideRotateSuggestion(Object controller) {
        try {
            callMethod(controller, "setRotateSuggestionButtonState", false, true);
        } catch (Throwable ignored) {
            try {
                callMethod(controller, "setRotateSuggestionButtonState", false);
            } catch (Throwable alsoIgnored) {
                XposedLog.e(TAG, "setRotateSuggestionButtonState 两种重载都没找到");
            }
        }
    }

    /** 找出脱糖（desugar）出来的 lambda 类里持有宿主实例的那个字段名（f$0、f$1 都可能是） */
    private String findCapturedField(Class<?> lambdaClass, Class<?> ownerClass) {
        for (Field field : lambdaClass.getDeclaredFields()) {
            if (field.getType().equals(ownerClass)) {
                return field.getName();
            }
        }
        return null;
    }

    private Class<?> findFirstClass(String... candidateNames) {
        for (String name : candidateNames) {
            Class<?> clazz = findClassIfExists(name);
            if (clazz != null) {
                return clazz;
            }
        }
        return null;
    }

    private void ensureRotationObserver(Object navigationBar, Context context) {
        if (isListen) return;
        if (context == null) {
            XposedLog.e(TAG, "context can't is null!");
            return;
        }
        ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                boolean isShow = getBoolean(context);
                int rotation = getInt(context);
                callMethod(navigationBar, "onRotationProposal", rotation, isShow);
            }
        };
        context.getContentResolver().registerContentObserver(
            Settings.System.getUriFor("rotation_button_data"), false, contentObserver
        );
        isListen = true;
        registerContentObserverHotReloadCleanup(context.getContentResolver(), contentObserver);
        putHotReloadRuntimeState(STATE_NAVIGATION_BAR, navigationBar);
        putHotReloadRuntimeState(STATE_CONTEXT, context);
    }

    public int getScreenOrientation(Context context) {
        Display display = context.getDisplay();
        int rotation = display.getRotation();
        // 获取屏幕方向
        return switch (rotation) {
            case Surface.ROTATION_0, Surface.ROTATION_180 ->
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; // 1 竖屏
            case Surface.ROTATION_90, Surface.ROTATION_270 ->
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; // 0 横屏
            default -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        };
    }

    private void setData(Context context, String value) {
        Settings.System.putString(context.getContentResolver(), "rotation_button_data", value);
    }

    private String getData(Context context) {
        return Settings.System.getString(context.getContentResolver(), "rotation_button_data");
    }

    private boolean getBoolean(Context context) {
        String data = getData(context);
        if (data == null) return false;
        String[] sp = data.split(",");
        String s = new ArrayList<>(Arrays.asList(sp)).get(1);
        return s.contains("true");
    }

    private int getInt(Context context) {
        String data = getData(context);
        if (data == null) return -1;
        String[] sp = data.split(",");
        return Integer.parseInt(new ArrayList<>(Arrays.asList(sp)).get(0));
    }
}
