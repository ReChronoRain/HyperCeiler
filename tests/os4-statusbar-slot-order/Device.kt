package com.sevtinge.hyperceiler.libhook.utils.api
object DeviceHelper {
    object System {
        var version = 4f
        fun isHyperOSVersion(value: Float) = version == value
        fun isMoreHyperOSVersion(value: Float) = version >= value
    }
}
