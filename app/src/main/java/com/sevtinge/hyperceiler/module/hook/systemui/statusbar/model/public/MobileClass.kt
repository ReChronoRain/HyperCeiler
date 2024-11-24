package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model.public

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass

object MobileClass {
    val statusBarMobileClass by lazy {
        loadClass("com.android.systemui.statusbar.StatusBarMobileView")
    }
    val miuiMobileIconBinder by lazy {
        loadClass("com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder")
    }
    val mOperatorConfig by lazy {
        loadClass("com.miui.interfaces.IOperatorCustomizedPolicy\$OperatorConfig")
    }
    val miuiCellularIconVM by lazy {
        loadClass("com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiCellularIconVM")
    }
    val miuiCarrier by lazy {
        loadClass("com.android.systemui.statusbar.policy.MiuiCarrierTextControllerImpl")
    }
    val hdController by lazy {
        loadClass("com.android.systemui.statusbar.policy.HDController")
    }
}