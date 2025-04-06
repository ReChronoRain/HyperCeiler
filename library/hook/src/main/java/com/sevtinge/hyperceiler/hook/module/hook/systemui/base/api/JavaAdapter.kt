package com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api

import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.callMethodAs
import java.util.concurrent.CancellationException
import java.util.function.Consumer

class JavaAdapter(instance: Any) : BaseReflectObject(instance) {
    fun <T> alwaysCollectFlow(flow: Any, consumer: Consumer<T>): KotlinJob {
        return KotlinJob(instance.callMethodAs("alwaysCollectFlow", flow, consumer))
    }
}

class KotlinJob(instance: Any) : BaseReflectObject(instance) {
    fun cancel(e: CancellationException? = null) {
        instance.callMethod("cancel", e)
    }
}
