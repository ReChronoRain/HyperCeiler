package com.sevtinge.hyperceiler.module.hook.systemui.api

abstract class BaseReflectObject(open val instance: Any) {
    override fun toString(): String = instance.toString()
}
