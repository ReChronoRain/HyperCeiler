package com.voyager.star.hooks.apps

import android.os.Build
import com.voyager.star.hooks.rules.miuihome.MonetColor
import com.voyager.star.utils.AppRegister
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LoadPackage

object MiuiHome : AppRegister() {
    override val packageName: String = "com.miui.home"
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.TIRAMISU -> {
                autoInitHooks(
                    lpparam,
                )
            }

            Build.VERSION_CODES.S -> {
                autoInitHooks(
                    lpparam,

                )
            }
        }
    }

@Throws(Throwable::class)
override fun handleInitPackageResources(resparam: InitPackageResourcesParam) {
    when (Build.VERSION.SDK_INT) {
        Build.VERSION_CODES.TIRAMISU -> {
            autoInitResourcesHooks(
                resparam,
                MonetColor
            )
        }
    }
}
}