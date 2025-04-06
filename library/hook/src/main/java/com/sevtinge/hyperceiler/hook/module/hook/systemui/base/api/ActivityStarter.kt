package com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api

import android.content.Intent
import com.sevtinge.hyperceiler.hook.utils.callMethod

@Suppress("unused")
class ActivityStarter(instance: Any) : BaseReflectObject(instance) {
    @JvmOverloads
    fun startActivity(intent: Intent, isCreateStatusBarTransitionAnimator: Boolean = true) {
        instance.callMethod("startActivity", intent, isCreateStatusBarTransitionAnimator)
    }
}
