package com.voyager.star.utils

import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.Log.logexIfThrow
import com.voyager.star.utils.AppRegister
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam
import de.robv.android.xposed.callbacks.XC_LoadPackage

abstract class EasyXposedInit : IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {

    private lateinit var packageParam: XC_LoadPackage.LoadPackageParam
    private lateinit var packageResourcesParam: InitPackageResourcesParam
    abstract val registeredApp: List<AppRegister>
    private val TAG = "Voyager"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {

        packageParam = lpparam!!
        registeredApp.forEach { app ->
            if (app.packageName == lpparam.packageName) {
                EzXHelperInit.apply {
                    setLogXp(true)
                    setLogTag(TAG)
                    setToastTag(TAG)
                    initHandleLoadPackage(lpparam)
                }
                runCatching { app.handleLoadPackage(lpparam) }.logexIfThrow("Failed call handleLoadPackage, package: ${app.packageName}")
            }
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        EzXHelperInit.initZygote(startupParam!!)
    }

    override fun handleInitPackageResources(resparam: InitPackageResourcesParam?) {
        packageResourcesParam = resparam!!
        registeredApp.forEach { app ->
            if(app.packageName == resparam.packageName) {
                runCatching { app.handleInitPackageResources(resparam) }.logexIfThrow("Failed call handleInitPackageResources, package: ${app.packageName}")
            }
        }
    }
}