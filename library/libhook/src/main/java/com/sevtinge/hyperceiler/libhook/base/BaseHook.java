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
package com.sevtinge.hyperceiler.libhook.base;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.ResourcesTool;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsMap;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import java.lang.reflect.Method;
import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.EzXposed;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.MethodUnhooker;
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam;
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam;

/**
 * Hook 基类
 * <p>
 * 提供 Java 常用的 Hook 工具方法
 * Kotlin 建议直接使用 ezxhelper API
 *
 * @author HyperCeiler
 */
public abstract class BaseHook {
    public static String ACTION_PREFIX = "com.sevtinge.hyperceiler.module.action.";
    public final String TAG = getClass().getSimpleName();
    public static final PrefsMap<String, Object> mPrefsMap = PrefsUtils.mPrefsMap;

    /**
     * 初始化 Hook，子类实现此方法编写具体 Hook 逻辑
     */
    public abstract void init();

    /**
     * 获取类加载器
     */
    public ClassLoader getClassLoader() {
        if (BaseLoad.getClassLoader() != null) {
            return BaseLoad.getClassLoader();
        }
        return EzXposed.getAppContext().getClassLoader();
    }

    /**
     * 获取包名
     */
    public String getPackageName() {
        return BaseLoad.getPackageName();
    }

    /**
     * 获取包加载参数 (应用)
     */
    public PackageLoadedParam getLpparam() {
        return BaseLoad.getLpparam();
    }

    /**
     * 获取包加载参数 (系统框架)
     */
    public SystemServerLoadedParam getSystemParam() {
        return BaseLoad.getSystemServerParam();
    }

    /**
     * 获取 Xposed 接口
     */
    public XposedInterface xposed() {
        return BaseLoad.getXposed();
    }

    // ==================== 类查找 ====================

    public Class<?> findClass(String className) {
        return EzxHelpUtils.findClass(className, getClassLoader());
    }

    public Class<?> findClass(String className, ClassLoader classLoader) {
        return EzxHelpUtils.findClass(className, classLoader);
    }

    public Class<?> findClassIfExists(String className) {
        return EzxHelpUtils.findClassIfExists(className, getClassLoader());
    }

    public Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        return EzxHelpUtils.findClassIfExists(className, classLoader);
    }

    // ==================== 字段操作 ====================

    /**
     * 获取对象字段值
     */
    public static Object getObjectField(Object obj, String fieldName) {
        return EzxHelpUtils.getObjectField(obj, fieldName);
    }

    /**
     * 设置对象字段值
     */
    public static void setObjectField(Object obj, String fieldName, Object value) {
        EzxHelpUtils.setObjectField(obj, fieldName, value);
    }

    /**
     * 获取静态字段值
     */
    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        return EzxHelpUtils.getStaticObjectField(clazz, fieldName);
    }

    /**
     * 设置静态字段值
     */
    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        EzxHelpUtils.setStaticObjectField(clazz, fieldName, value);
    }

    // ==================== 方法调用 ====================

    /**
     * 调用对象方法
     */
    public static Object callMethod(Object obj, String methodName, Object... args) {
        return EzxHelpUtils.callMethod(obj, methodName, args);
    }

    /**
     * 调用静态方法
     */
    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        return EzxHelpUtils.callStaticMethod(clazz, methodName, args);
    }

    // ==================== Application 生命周期 ====================

    /**
     * Application.attach(Context) 之前调用
     * 子类可重写此方法
     */
    protected void onApplicationAttachBefore(@NonNull Context context) {
    }

    /**
     * Application.attach(Context) 之后调用
     * 子类可重写此方法
     */
    protected void onApplicationAttachAfter(@NonNull Context context) {
    }

    /**
     * 注册当前 Hook 的 Application 生命周期回调
     */
    protected void registerApplicationHook() {
        EzxHelpUtils.registerApplicationHook(new EzxHelpUtils.IApplicationHook() {
            @Override
            public void onApplicationAttachBefore(@NonNull Context context) {
                try {
                    BaseHook.this.onApplicationAttachBefore(context);
                } catch (Throwable t) {
                    XposedLog.e(TAG, "onApplicationAttachBefore error", t);
                }
            }

            @Override
            public void onApplicationAttachAfter(@NonNull Context context) {
                try {
                    BaseHook.this.onApplicationAttachAfter(context);
                } catch (Throwable t) {
                    XposedLog.e(TAG, "onApplicationAttachAfter error", t);
                }
            }
        });
    }

    /**
     * 注册 Application attach 之后的回调
     */
    protected void runOnApplicationAttach(EzxHelpUtils.ContextConsumer callback) {
        EzxHelpUtils.runOnApplicationAttach(callback);
    }

    /**
     * 注册 Application 生命周期回调
     */
    protected void registerApplicationHook(
        @Nullable EzxHelpUtils.ContextConsumer before,
        @Nullable EzxHelpUtils.ContextConsumer after
    ) {
        EzxHelpUtils.registerApplicationHook(before, after);
    }

    // ==================== Hook 方法 ====================

    /**
     * Hook 方法
     *
     * @param method   要 Hook 的方法
     * @param callback Hook 回调
     * @return MethodUnhooker 对象
     */
    public MethodUnhooker<?> hookMethod(Method method, IMethodHook callback) {
        return EzxHelpUtils.hookMethod(method, callback);
    }

    /**
     * 查找并 Hook 方法
     * <p>
     * 最后一个参数必须是 IMethodHook，前面的参数是参数类型
     *
     * @param clazz      目标类
     * @param methodName 方法名
     * @param args       参数类型 + IMethodHook 回调
     * @return MethodUnhooker 对象
     */
    public MethodUnhooker<?> findAndHookMethod(Class<?> clazz, String methodName, Object... args) {
        return EzxHelpUtils.findAndHookMethod(clazz, methodName, args);
    }

    /**
     * 查找并 Hook 方法
     * <p>
     * 最后一个参数必须是 IMethodHook，前面的参数是参数类型
     *
     * @param className  类名
     * @param methodName 方法名
     * @param args       参数类型 + IMethodHook 回调
     * @return MethodUnhooker 对象
     */
    public MethodUnhooker<?> findAndHookMethod(String className, String methodName, Object... args) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w("IHook", "findAndHookMethod: class not found: " + className);
            return null;
        }
        return EzxHelpUtils.findAndHookMethod(clazz, methodName, args);
    }

    /**
     * 查找并 Hook 方法
     * <p>
     * 最后一个参数必须是 IMethodHook，前面的参数是参数类型
     *
     * @param clazz      目标类
     * @param methodName 方法名
     * @param args       参数类型 + IMethodHook 回调
     * @return MethodUnhooker 对象
     */
    public MethodUnhooker<?> findAndReplaceMethod(Class<?> clazz, String methodName, Object... args) {
        return EzxHelpUtils.findAndHookMethodReplace(clazz, methodName, args);
    }

    /**
     * 查找并 Hook 方法
     * <p>
     * 最后一个参数必须是 IMethodHook，前面的参数是参数类型
     *
     * @param className  类名
     * @param methodName 方法名
     * @param args       参数类型 + IMethodHook 回调
     * @return MethodUnhooker 对象
     */
    public MethodUnhooker<?> findAndReplaceMethod(String className, String methodName, Object... args) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w("BaseHook", "findAndReplaceMethod: class not found: " + className);
            return null;
        }
        return EzxHelpUtils.findAndHookMethodReplace(clazz, methodName, args);
    }

    /**
     * Hook 类中所有指定名称的方法
     *
     * @param clazz      目标类
     * @param methodName 方法名
     * @param callback   Hook 回调
     * @return MethodUnhooker 对象列表
     */
    public List<MethodUnhooker<?>> hookAllMethods(Class<?> clazz, String methodName, IMethodHook callback) {
        return EzxHelpUtils.hookAllMethods(clazz, methodName, callback);
    }

    /**
     * Hook 类中所有指定名称的方法（通过类名）
     *
     * @param className  类名
     * @param methodName 方法名
     * @param callback   Hook 回调
     * @return MethodUnhooker 对象列表
     */
    public List<MethodUnhooker<?>> hookAllMethods(String className, String methodName, IMethodHook callback) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w("BaseHook", "hookAllMethods: class not found: " + className);
            return java.util.Collections.emptyList();
        }
        return EzxHelpUtils.hookAllMethods(clazz, methodName, callback);
    }

    /**
     * Hook 类中所有构造器
     *
     * @param clazz    目标类
     * @param callback Hook 回调
     * @return MethodUnhooker 对象列表
     */
    public List<MethodUnhooker<?>> hookAllConstructors(Class<?> clazz, IMethodHook callback) {
        return EzxHelpUtils.hookAllConstructors(clazz, callback);
    }

    /**
     * Hook 类中所有构造器
     *
     * @param className    类名
     * @param callback Hook 回调
     * @return MethodUnhooker 对象列表
     */
    public List<MethodUnhooker<?>> hookAllConstructors(String className, IMethodHook callback) {
        Class<?> clazz = findClassIfExists(className);
        if (clazz == null) {
            XposedLog.w("BaseHook", "hookAllConstructors: class not found: " + className);
            return java.util.Collections.emptyList();
        }
        return EzxHelpUtils.hookAllConstructors(clazz, callback);
    }

    // ==================== 便捷 Hook 工具 ====================

    /**
     * 创建一个返回常量值的 Hook 回调
     */
    public IMethodHook returnConstant(Object result) {
        return EzxHelpUtils.returnConstant(result);
    }

    /**
     * 获取阻止原方法执行的 Hook（返回 null）
     */
    public IMethodHook doNothing() {
        return EzxHelpUtils.DO_NOTHING;
    }

    public Object proxySystemProperties(String method, String prop, int val, ClassLoader classLoader) {
        return callStaticMethod(findClassIfExists("android.os.SystemProperties", classLoader),
            method, prop, val);
    }

    // ==================== 资源 Hook ====================

    /**
     * 获取虚拟资源 ID
     */
    public static int getFakeResId(String resourceName) {
        return ResourcesTool.getFakeResId(resourceName);
    }

    /**
     * 设置资源替换
     */
    public static void setResReplacement(String pkg, String type, String name, int replacementResId) {
        ResourcesTool resTool = ResourcesTool.getInstance();
        if (resTool != null) {
            resTool.setResReplacement(pkg, type, name, replacementResId);
        }
    }

    /**
     * 设置密度资源替换
     */
    public static void setDensityReplacement(String pkg, String type, String name, float replacementResValue) {
        ResourcesTool resTool = ResourcesTool.getInstance();
        if (resTool != null) {
            resTool.setDensityReplacement(pkg, type, name, replacementResValue);
        }
    }

    /**
     * 设置对象资源替换
     */
    public static void setObjectReplacement(String pkg, String type, String name, Object replacementResValue) {
        ResourcesTool resTool = ResourcesTool.getInstance();
        if (resTool != null) {
            resTool.setObjectReplacement(pkg, type, name, replacementResValue);
        }
    }

}
