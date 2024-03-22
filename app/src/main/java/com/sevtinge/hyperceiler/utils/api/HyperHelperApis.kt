package com.sevtinge.hyperceiler.utils.api

object HyperHelperApis {
    fun linearInterpolate(start: Float, stop: Float, amount: Float): Float {
        return start + (stop - start) * amount
    }

    fun linearInterpolate(start: Int, stop: Int, amount: Float): Int {
        return start + ((stop - start) * amount).toInt()
    }
}