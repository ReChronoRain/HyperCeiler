/*
 * This file is part of HyperCeiler.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.sevtinge.hyperceiler.common.log

import androidx.annotation.Keep

/**
 * Stable JNI entry used by the native logging facade.
 *
 * The class name and method signature are part of the native ABI. Logging
 * remains owned by [XposedLog], so the existing user level switch applies.
 */
@Keep
object NativeLogBridge {
    @JvmStatic
    fun log(priority: Int, tag: String, message: String) {
        when (priority) {
            3 -> XposedLog.d(tag, message)
            4 -> XposedLog.i(tag, message)
            5 -> XposedLog.w(tag, message)
            6 -> XposedLog.e(tag, message)
            else -> XposedLog.i(tag, message)
        }
    }
}
