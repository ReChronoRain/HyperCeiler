package io.github.lingqiqi5211.ezhooktool.xposed.java

import io.github.lingqiqi5211.ezhooktool.xposed.internal.AdditionalFields

/**
 * 供 Java 调用的额外字段入口。
 */
object ExtraFields {
    /**
     * 为实例写入附加字段。
     *
     * @param obj 目标实例
     * @param key 附加字段名
     * @param value 新值
     */
    @JvmStatic
    fun setInstanceField(obj: Any, key: String, value: Any?): Any? = AdditionalFields.setInstance(obj, key, value)

    /**
     * 读取实例上的附加字段。
     *
     * @param obj 目标实例
     * @param key 附加字段名
     */
    @JvmStatic
    fun getInstanceField(obj: Any, key: String): Any? = AdditionalFields.getInstance(obj, key)

    /**
     * 移除实例上的附加字段。
     *
     * @param obj 目标实例
     * @param key 附加字段名
     */
    @JvmStatic
    fun removeInstanceField(obj: Any, key: String): Any? = AdditionalFields.removeInstance(obj, key)

    /**
     * 为类写入附加静态字段。
     *
     * @param clazz 目标类
     * @param key 附加字段名
     * @param value 新值
     */
    @JvmStatic
    fun setStaticField(clazz: Class<*>, key: String, value: Any?): Any? = AdditionalFields.setStatic(clazz, key, value)

    /**
     * 读取类上的附加静态字段。
     *
     * @param clazz 目标类
     * @param key 附加字段名
     */
    @JvmStatic
    fun getStaticField(clazz: Class<*>, key: String): Any? = AdditionalFields.getStatic(clazz, key)

    /**
     * 移除类上的附加静态字段。
     *
     * @param clazz 目标类
     * @param key 附加字段名
     */
    @JvmStatic
    fun removeStaticField(clazz: Class<*>, key: String): Any? = AdditionalFields.removeStatic(clazz, key)
}
