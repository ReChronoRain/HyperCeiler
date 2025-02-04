package com.sevtinge.hyperceiler.module.hook.systemui.base.api

abstract class BaseReflectObject(open val instance: Any) {
    override fun toString(): String = instance.toString()
}
