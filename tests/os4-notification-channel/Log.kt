package com.sevtinge.hyperceiler.common.log
object XposedLog { fun w(tag: String, pkg: String, message: String, error: Throwable) { throw AssertionError(message, error) } }
