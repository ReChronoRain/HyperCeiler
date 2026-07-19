@file:Suppress("unused", "UNCHECKED_CAST", "NOTHING_TO_INLINE")

package io.github.lingqiqi5211.ezhooktool.xposed.common

import java.lang.reflect.Executable

/** libxposed 101 hook 回调参数包装。 */
open class HookParam internal constructor(
    private val state: InvocationContext,
) {
    /** 当前正在被 hook 的方法或构造器。 */
    val executable: Executable
        get() = state.executable

    /** 兼容经典 Xposed 命名的成员别名。 */
    val member: Executable
        get() = executable

    /** 当前实例方法的 `this` 对象；静态方法时会抛异常。 */
    val thisObject: Any
        get() = state.thisObject
            ?: throw NullPointerException("static method should not have thisObject")

    /** 与 [thisObject] 相同，便于和上游命名保持一致。 */
    val thisObjectOrNull: Any?
        get() = state.thisObject

    /** 把 [thisObject] 直接转换成目标类型。 */
    fun <T> thisObjectAs(): T = thisObject as T

    /** 当前调用的参数数组。 */
    val args: Array<Any?>
        get() = state.args

    /** 按下标读取参数。 */
    fun arg(index: Int): Any? = args[index]

    /** 按下标读取参数并转换成目标类型。 */
    fun <T> argAs(index: Int): T = args[index] as T

    /** 当前调用是否已跳过原始实现。 */
    val isSkipped: Boolean
        get() = state.skipped

    /** 当前回调设置或读取的返回值。 */
    var result: Any?
        get() = state.result
        set(value) {
            if (!state.isAfterStage) {
                state.skipped = true
            }
            state.throwable = null
            state.result = value
        }

    /** 把 [result] 直接转换成目标类型。 */
    fun <T> resultAs(): T = result as T

    /** 当前回调设置或读取的异常。 */
    var throwable: Throwable?
        get() = state.throwable
        set(value) {
            if (!state.isAfterStage) {
                state.skipped = true
            }
            state.result = null
            state.throwable = value
        }

    /** 当前调用是否带有异常结果。 */
    val hasThrowable: Boolean
        get() = state.throwable != null

    internal fun context(): InvocationContext = state
}
