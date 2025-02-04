package com.sevtinge.hyperceiler.module.hook.systemui.base.api

import android.content.Intent
import com.sevtinge.hyperceiler.utils.callMethod

@Suppress("unused")
class ActivityStarter(instance: Any) : BaseReflectObject(instance) {
    @JvmOverloads
    fun startActivity(intent: Intent, isCreateStatusBarTransitionAnimator: Boolean = true) {
        instance.callMethod("startActivity", intent, isCreateStatusBarTransitionAnimator)
    }
}
