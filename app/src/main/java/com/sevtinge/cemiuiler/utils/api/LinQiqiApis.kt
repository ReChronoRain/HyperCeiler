package com.sevtinge.cemiuiler.utils.api

import android.app.admin.DevicePolicyManager
import android.content.Context

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

/**
检测设备是否加密，来源米客
 */
fun isDeviceEncrypted(context: Context): Boolean {
    val policyMgr = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val encryption = policyMgr.storageEncryptionStatus
    return encryption == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE ||
        encryption == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER
    // API 34 已弃用下面属性值，官方说法
    // This result code has never actually been used, so there is no reason for apps to check for it.
    // encryption == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING
}
