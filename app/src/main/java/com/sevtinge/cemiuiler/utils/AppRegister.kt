package com.sevtinge.cemiuiler.utils

import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.LogExtensions.logexIfThrow
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage

abstract class AppRegister : IXposedHookLoadPackage, IXposedHookInitPackageResources {

    abstract val packageName: String

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {}

    protected fun autoInitHooks(lpparam: XC_LoadPackage.LoadPackageParam, vararg hook: HookRegister) {
        hook.also {
        }.forEach {
            runCatching {
                if (it.isInit) return@forEach
                it.setLoadPackageParam(lpparam)
                it.init()
                it.isInit = true
                Log.ix("Inited hook: ${it.javaClass.simpleName}")
            }.logexIfThrow("Failed to Hook [$packageName]")
        }
    }

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {}

    protected fun autoInitResourcesHooks(
        resparam: XC_InitPackageResources.InitPackageResourcesParam,
        vararg hook: ResourcesHookRegister
    ) {
        hook.also {
        }.forEach {
            runCatching {
                if (it.isInit) return@forEach
                it.setInitPackageResourcesParam(resparam)
                it.init()
                it.isInit = true
                Log.ix("Inited hook: ${it.javaClass.simpleName}")
            }.logexIfThrow("Failed to Hook [$packageName]")
        }
    }
}
