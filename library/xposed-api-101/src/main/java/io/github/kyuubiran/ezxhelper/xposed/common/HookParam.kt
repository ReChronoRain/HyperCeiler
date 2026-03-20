@file:Suppress("unused", "UNCHECKED_CAST", "NOTHING_TO_INLINE")
package io.github.kyuubiran.ezxhelper.xposed.common

/**
 * Wraps a hook invocation context based on chain semantics.
 */
open class HookParam internal constructor(
    private val state: InvocationContext,
) {

    /**
     * Gets the method / constructor to be hooked.
     */
    val executable
        get() = state.executable

    /**
     * Gets the {@code this} pointer for the call, or {@code null} for static methods.
     */
    val thisObject: Any
        get() = state.thisObject
            ?: throw NullPointerException("static method should not have thisObject")

    /**
     * Gets the nullable receiver object for the current call.
     */
    val thisObjectOrNull: Any?
        get() = state.thisObject

    /**
     * Convenience cast of [thisObject] to a specific type T.
     *
     * @throws NullPointerException if the hooked method is static.
     * @throws ClassCastException if the object is not of type T.
     */
    inline fun <T> thisObjectAs(): T = thisObject as T

    /**
     * Arguments passed to the hooked method or constructor.
     * Modifications to this array will change the arguments passed
     * to the original member.
     */
    val args: Array<Any?>
        get() = state.args

    /**
     * Gets the argument at the given index.
     */
    fun arg(index: Int): Any? = args[index]

    /**
     * Indicates whether the original executable has been skipped.
     */
    val isSkipped: Boolean
        get() = state.skipped

    /**
     * Assign a return value and skip the original method or constructor.
     * For constructors the `result` is ignored.
     */
    var result: Any?
        get() = state.result
        set(value) {
            if (!state.isAfterStage) {
                state.skipped = true
            }
            state.result = value
            state.throwable = null
        }

    /**
     * Throws the given exception and skips the original method or constructor.
     */
    var throwable: Throwable?
        get() = state.throwable
        set(value) {
            if (!state.isAfterStage) {
                state.skipped = true
            }
            state.result = null
            state.throwable = value
        }
    internal fun context(): InvocationContext = state
}
