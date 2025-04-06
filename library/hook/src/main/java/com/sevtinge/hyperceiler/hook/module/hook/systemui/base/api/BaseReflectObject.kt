package com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api

abstract class BaseReflectObject(open val instance: Any) {
    override fun toString(): String = instance.toString()
}
