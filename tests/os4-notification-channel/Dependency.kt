package com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui
object Dependency {
    fun getDependencyInner(clazz: Class<*>): Any? = when(clazz) {
        fixture.CommandQueue::class.java -> fixture.Scenario.queue
        fixture.ModalController::class.java -> fixture.Scenario.modal
        else -> null
    }
}
