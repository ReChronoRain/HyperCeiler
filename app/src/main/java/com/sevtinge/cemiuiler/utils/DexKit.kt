package com.sevtinge.cemiuiler.utils

import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.luckypray.dexkit.DexKitBridge

object DexKit {
    lateinit var hostDir: String
    lateinit var dexKitBridge: DexKitBridge

    fun initDexKit(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        hostDir = loadPackageParam.appInfo.sourceDir
    }

    fun loadDexKit() {
        if (this::dexKitBridge.isInitialized) return
        System.loadLibrary("dexkit")
        DexKitBridge.create(hostDir)?.let {
            dexKitBridge = it
        }
    }

    fun closeDexKit() {
        if (this::dexKitBridge.isInitialized) dexKitBridge.close()
    }
}
