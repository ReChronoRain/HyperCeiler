package com.sevtinge.cemiuiler.utils

import de.robv.android.xposed.callbacks.XC_LoadPackage
import io.luckypray.dexkit.DexKitBridge

class DexKitHelper {
    companion object {

        lateinit var hostDir: String
        lateinit var dexKitBridge: DexKitBridge

        @JvmStatic
        fun initDexKit(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
            hostDir = loadPackageParam.appInfo.sourceDir
        }

        @JvmStatic
        fun loadDexKit() {
            if (this::dexKitBridge.isInitialized) return
            System.loadLibrary("dexkit")
            DexKitBridge.create(hostDir)?.let {
                dexKitBridge = it
            }
        }

        @JvmStatic
        fun closeDexKit() {
            dexKitBridge.close()
        }
    }
}