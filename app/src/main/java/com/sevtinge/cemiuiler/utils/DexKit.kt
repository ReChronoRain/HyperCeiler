package com.sevtinge.cemiuiler.utils

import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.luckypray.dexkit.DexKitBridge

/**
* DexKit 工具
*/
object DexKit {
    private lateinit var hostDir: String
    private lateinit var dexKitBridge: DexKitBridge
    val safeDexKitBridge: DexKitBridge
        get() {
            loadDexKit()
            return dexKitBridge
        }

    /**
     * 初始化 DexKit 的 apk 完整路径
     */
    fun initDexKit(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        hostDir = loadPackageParam.appInfo.sourceDir
    }

    /**
     * 初始化 DexKit bridge
     */
    private fun loadDexKit() {
        if (this::dexKitBridge.isInitialized) return
        System.loadLibrary("dexkit")
        DexKitBridge.create(hostDir)?.let {
            dexKitBridge = it
        }
    }

    /**
     * 关闭 DexKit bridge
     */
    fun closeDexKit() {
        if (this::dexKitBridge.isInitialized) dexKitBridge.close()
    }
}
