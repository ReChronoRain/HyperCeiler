package com.sevtinge.cemiuiler.utils.woobox

import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam

abstract class ResourcesHookRegister {
    private lateinit var resparam: XC_InitPackageResources.InitPackageResourcesParam
    var isInit: Boolean = false
    abstract fun init()

    fun setInitPackageResourcesParam(initPackageResourcesParam: InitPackageResourcesParam) {
        resparam = initPackageResourcesParam
    }

    protected fun getInitPackageResourcesParam(): InitPackageResourcesParam {
        if (!this::resparam.isInitialized) {
            throw RuntimeException("resparam should be initialized")
        }
        return resparam
    }

}