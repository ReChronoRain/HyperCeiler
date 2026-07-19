package io.github.lingqiqi5211.ezhooktool.xposed.java

import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.Arrays

/**
 * 供 Java 调用的去优化入口。
 */
object Deoptimizers {
    /**
     * 对指定方法做去优化。
     *
     * @param method 目标方法
     */
    @JvmStatic
    fun deoptimize(method: Method): Boolean = EzXposed.deoptimize(method)

    /**
     * 对指定方法组做去优化。
     *
     * @param clazz 目标类名
     * @param names 目标方法名列表
     */
    @JvmStatic
    fun deoptimizeMethods(clazz: Class<*>, vararg names: String) {
        val list = listOf(*names)
        Arrays.stream(clazz.declaredMethods)
            .filter { method: Method? ->
                list.contains(method!!.name)
            }.forEach { method: Method? ->
                deoptimize(method!!)
            }
    }

    /**
     * 对指定构造器做去优化。
     *
     * @param constructor 目标构造器
     */
    @JvmStatic
    fun deoptimize(constructor: Constructor<*>): Boolean = EzXposed.deoptimize(constructor)
}
