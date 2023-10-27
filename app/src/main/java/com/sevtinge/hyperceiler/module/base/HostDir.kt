package com.sevtinge.hyperceiler.module.base

import com.sevtinge.hyperceiler.utils.DexKit.closeDexKit
import com.sevtinge.hyperceiler.utils.DexKit.initDexKit

object LoadHostDir : BaseHook() {
    override fun init() {
        if (lpparam != null) {
            initDexKit(lpparam)
        }
    }
}


object CloseHostDir : BaseHook() {
    override fun init() {
        if (lpparam != null) {
            closeDexKit()
        }
    }
}
