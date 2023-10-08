package com.sevtinge.cemiuiler.module.base

import com.sevtinge.cemiuiler.utils.DexKit.closeDexKit
import com.sevtinge.cemiuiler.utils.DexKit.initDexKit

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
