package io.github.lingqiqi5211.ezhooktool.xposed.java

import io.github.libxposed.api.XposedInterface
import io.github.lingqiqi5211.ezhooktool.core.findConstructorBestMatch
import io.github.lingqiqi5211.ezhooktool.core.findMethodBestMatch
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.common.AfterChainStage
import io.github.lingqiqi5211.ezhooktool.xposed.common.BeforeChainStage
import io.github.lingqiqi5211.ezhooktool.xposed.common.ReplaceChainStage
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.buildHooker
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createInterceptHook
import io.github.lingqiqi5211.ezhooktool.xposed.internal.HookClassLoader
import java.lang.reflect.Constructor
import java.lang.reflect.Method

/**
 * 供 Java 调用的 hook 入口。
 *
 * 已经拿到 `Method` / `Constructor` 时使用 `createHook`。
 * 想按名称和参数类型查找并立即 hook 时使用 `findAndHookMethod` 或 `findAndHookConstructor`。
 *
 * `findAndHookMethod` 和 `findAndHookConstructor` 的最后一个可变参数必须是
 * `IMethodHook`、`IReplaceHook` 或 libxposed 的 `Hooker`。
 */
object Hooks {
    /**
     * 为方法创建普通 hook。
     *
     * @param method 目标方法
     * @param callback before / after 回调
     */
    @JvmStatic
    fun createHook(method: Method, callback: IMethodHook): XposedInterface.HookHandle =
        method.createHook {
            before { callback.before(it) }
            after { callback.after(it) }
        }

    /**
     * 为构造器创建普通 hook。
     *
     * @param constructor 目标构造器
     * @param callback before / after 回调
     */
    @JvmStatic
    fun createHook(constructor: Constructor<*>, callback: IMethodHook): XposedInterface.HookHandle =
        constructor.createHook {
            before { callback.before(it) }
            after { callback.after(it) }
        }

    /**
     * 为方法创建替换 hook。
     *
     * @param method 目标方法
     * @param callback replace 回调，返回值会作为原方法结果
     */
    @JvmStatic
    fun createHook(method: Method, callback: IReplaceHook): XposedInterface.HookHandle =
        method.createHook {
            replace { callback.replace(it) }
        }

    /**
     * 为构造器创建替换 hook。
     *
     * @param constructor 目标构造器
     * @param callback replace 回调，返回值会作为原构造器结果
     */
    @JvmStatic
    fun createHook(constructor: Constructor<*>, callback: IReplaceHook): XposedInterface.HookHandle =
        constructor.createHook {
            replace { callback.replace(it) }
        }

    /**
     * 为方法列表批量创建普通 hook。
     *
     * @param methods 目标方法列表
     * @param callback before / after 回调
     */
    @JvmStatic
    fun createHooks(methods: Iterable<Method>, callback: IMethodHook): List<XposedInterface.HookHandle> =
        methods.map { createHook(it, callback) }

    /**
     * 为方法列表批量创建替换 hook。
     *
     * @param methods 目标方法列表
     * @param callback replace 回调，返回值会作为原方法结果
     */
    @JvmStatic
    fun createHooks(methods: Iterable<Method>, callback: IReplaceHook): List<XposedInterface.HookHandle> =
        methods.map { createHook(it, callback) }

    /**
     * 为构造器列表批量创建普通 hook。
     *
     * @param constructors 目标构造器列表
     * @param callback before / after 回调
     */
    @JvmStatic
    fun createConstructorHooks(
        constructors: Iterable<Constructor<*>>,
        callback: IMethodHook,
    ): List<XposedInterface.HookHandle> = constructors.map { createHook(it, callback) }

    /**
     * 为构造器列表批量创建替换 hook。
     *
     * @param constructors 目标构造器列表
     * @param callback replace 回调，返回值会作为原构造器结果
     */
    @JvmStatic
    fun createConstructorHooks(
        constructors: Iterable<Constructor<*>>,
        callback: IReplaceHook,
    ): List<XposedInterface.HookHandle> = constructors.map { createHook(it, callback) }

    /**
     * 按方法名和参数类型查找方法并立即创建 hook。
     *
     * @param clazz 目标类
     * @param methodName 方法名
     * @param parameterTypesAndCallback 参数类型列表，最后一项必须是 `IMethodHook`、`IReplaceHook` 或 `Hooker`
     */
    @JvmStatic
    fun findAndHookMethod(
        clazz: Class<*>,
        methodName: String,
        vararg parameterTypesAndCallback: Any,
    ): XposedInterface.HookHandle {
        val callback = requireCallback(parameterTypesAndCallback)
        val method = resolveMethod(clazz, methodName, parameterTypesAndCallback)
        return installCallback(method, callback)
    }

    /**
     * 按类名、方法名和参数类型查找方法并立即创建 hook。
     *
     * 默认使用当前 hook 运行时的 `ClassLoader`。
     *
     * @param className 目标类名
     * @param methodName 方法名
     * @param parameterTypesAndCallback 参数类型列表，最后一项必须是 `IMethodHook`、`IReplaceHook` 或 `Hooker`
     */
    @JvmStatic
    fun findAndHookMethod(
        className: String,
        methodName: String,
        vararg parameterTypesAndCallback: Any,
    ): XposedInterface.HookHandle =
        findAndHookMethod(loadClass(className, HookClassLoader.currentOrDefault()), methodName, *parameterTypesAndCallback)

    /**
     * 按类名、指定 `ClassLoader`、方法名和参数类型查找方法并立即创建 hook。
     *
     * @param className 目标类名
     * @param classLoader 用于加载目标类的 `ClassLoader`
     * @param methodName 方法名
     * @param parameterTypesAndCallback 参数类型列表，最后一项必须是 `IMethodHook`、`IReplaceHook` 或 `Hooker`
     */
    @JvmStatic
    fun findAndHookMethod(
        className: String,
        classLoader: ClassLoader,
        methodName: String,
        vararg parameterTypesAndCallback: Any,
    ): XposedInterface.HookHandle =
        findAndHookMethod(loadClass(className, classLoader), methodName, *parameterTypesAndCallback)

    /**
     * 按参数类型查找构造器并立即创建 hook。
     *
     * @param clazz 目标类
     * @param parameterTypesAndCallback 参数类型列表，最后一项必须是 `IMethodHook`、`IReplaceHook` 或 `Hooker`
     */
    @JvmStatic
    fun findAndHookConstructor(
        clazz: Class<*>,
        vararg parameterTypesAndCallback: Any,
    ): XposedInterface.HookHandle {
        val callback = requireCallback(parameterTypesAndCallback)
        val constructor = resolveConstructor(clazz, parameterTypesAndCallback)
        return installCallback(constructor, callback)
    }

    /**
     * 按类名和参数类型查找构造器并立即创建 hook。
     *
     * 默认使用当前 hook 运行时的 `ClassLoader`。
     *
     * @param className 目标类名
     * @param parameterTypesAndCallback 参数类型列表，最后一项必须是 `IMethodHook`、`IReplaceHook` 或 `Hooker`
     */
    @JvmStatic
    fun findAndHookConstructor(
        className: String,
        vararg parameterTypesAndCallback: Any,
    ): XposedInterface.HookHandle =
        findAndHookConstructor(loadClass(className, HookClassLoader.currentOrDefault()), *parameterTypesAndCallback)

    /**
     * 按类名、指定 `ClassLoader` 和参数类型查找构造器并立即创建 hook。
     *
     * @param className 目标类名
     * @param classLoader 用于加载目标类的 `ClassLoader`
     * @param parameterTypesAndCallback 参数类型列表，最后一项必须是 `IMethodHook`、`IReplaceHook` 或 `Hooker`
     */
    @JvmStatic
    fun findAndHookConstructor(
        className: String,
        classLoader: ClassLoader,
        vararg parameterTypesAndCallback: Any,
    ): XposedInterface.HookHandle =
        findAndHookConstructor(loadClass(className, classLoader), *parameterTypesAndCallback)

    /**
     * 为方法创建 intercept hook。
     *
     * @param method 目标方法
     * @param callback libxposed `Hooker` 回调
     */
    @JvmStatic
    fun intercept(
        method: Method,
        callback: XposedInterface.Hooker,
    ): XposedInterface.HookHandle = method.createInterceptHook { callback.intercept(it) }

    /**
     * 为构造器创建 intercept hook。
     *
     * @param constructor 目标构造器
     * @param callback libxposed `Hooker` 回调
     */
    @JvmStatic
    fun intercept(
        constructor: Constructor<*>,
        callback: XposedInterface.Hooker,
    ): XposedInterface.HookHandle = constructor.createInterceptHook { callback.intercept(it) }

    private fun resolveMethod(clazz: Class<*>, methodName: String, args: Array<out Any>): Method {
        val types = args.dropLast(1).map { resolveType(clazz, it) }.toTypedArray()
        return runCatching { clazz.getDeclaredMethod(methodName, *types).also { it.isAccessible = true } }.getOrNull()
            ?: findMethodBestMatch(clazz, methodName, *types)
    }

    private fun resolveConstructor(clazz: Class<*>, args: Array<out Any>): Constructor<*> {
        val types = args.dropLast(1).map { resolveType(clazz, it) }.toTypedArray()
        return runCatching { clazz.getDeclaredConstructor(*types).also { it.isAccessible = true } }.getOrNull()
            ?: findConstructorBestMatch(clazz, *types)
    }

    private fun resolveType(owner: Class<*>, value: Any): Class<*> = when (value) {
        is Class<*> -> value
        is String -> loadClass(value, owner.classLoader ?: HookClassLoader.currentOrDefault())
        else -> throw IllegalArgumentException("Parameter type must be Class or class name String, got ${value.javaClass.name}")
    }

    private fun requireCallback(args: Array<out Any>): Any {
        require(args.isNotEmpty()) { "findAndHook requires parameter types followed by a callback" }
        return args.last()
    }

    private fun installCallback(member: Any, callback: Any): XposedInterface.HookHandle = when (callback) {
        is IMethodHook -> when (member) {
            is Method -> createHook(member, callback)
            is Constructor<*> -> createHook(member, callback)
            else -> error("Unsupported member: $member")
        }
        is IReplaceHook -> when (member) {
            is Method -> createHook(member, callback)
            is Constructor<*> -> createHook(member, callback)
            else -> error("Unsupported member: $member")
        }
        is XposedInterface.Hooker -> when (member) {
            is Method -> intercept(member, callback)
            is Constructor<*> -> intercept(member, callback)
            else -> error("Unsupported member: $member")
        }
        else -> throw IllegalArgumentException("Unsupported callback type: ${callback.javaClass.name}")
    }
}
