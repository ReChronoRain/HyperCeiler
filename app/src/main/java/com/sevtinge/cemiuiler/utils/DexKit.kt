package com.sevtinge.cemiuiler.utils

import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.query.matchers.MethodMatcher

/**
 * DexKit 工具
 */
object DexKit {
    private lateinit var hostDir: String
    private var isInitialized = false
    val dexKitBridge: DexKitBridge by lazy {
        System.loadLibrary("dexkit")
        DexKitBridge.create(hostDir)!!.also {
            isInitialized = true
        }
    }

    /**
     * 初始化 DexKit 的 apk 完整路径
     */
    fun initDexKit(loadPackageParam: LoadPackageParam) {
        hostDir = loadPackageParam.appInfo.sourceDir
    }

    /**
     * 关闭 DexKit bridge
     */
    fun closeDexKit() {
        if (isInitialized) dexKitBridge.close()
    }

    /**
     * DexKit 封装查找方式
     */
    fun MethodMatcher.addUsingStringsEquals(vararg strings: String) {
        for (string in strings) {
            addUsingString(string, StringMatchType.Equals)
        }
    }

    fun ClassMatcher.addUsingStringsEquals(vararg strings: String) {
        for (string in strings) {
            addUsingString(string, StringMatchType.Equals)
        }
    }
}
