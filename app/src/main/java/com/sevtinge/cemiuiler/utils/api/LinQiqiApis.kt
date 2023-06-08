package com.sevtinge.cemiuiler.utils.api

/**
 * 源自 EzXHelper 1.x 版本所附赠的扩展函数，2.0 丢失，暂时先复用
 *
 * 判断类是否相同(用于判断参数)
 * eg: fun foo(a: Boolean, b: Int) { }
 * foo.parameterTypes.sameAs(*array)
 * foo.parameterTypes.sameAs(Boolean::class.java, Int::class.java)
 * foo.parameterTypes.sameAs("boolean", "int")
 * foo.parameterTypes.sameAs(Boolean::class.java, "int")
 *
 * @param other 其他类(支持String或者Class<*>)
 * @return 是否相等
 */
fun Array<Class<*>>.sameAs(vararg other: Any): Boolean {
    if (this.size != other.size) return false
    for (i in this.indices) {
        when (val otherClazz = other[i]) {
            is Class<*> -> {
                if (this[i] != otherClazz) return false
            }
            is String -> {
                if (this[i].name != otherClazz) return false
            }
            else -> {
                throw IllegalArgumentException("Only support Class<*> or String")
            }
        }
    }
    return true
}
