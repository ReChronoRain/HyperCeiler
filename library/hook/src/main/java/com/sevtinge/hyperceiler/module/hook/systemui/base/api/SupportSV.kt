package com.sevtinge.hyperceiler.module.hook.systemui.base.api

import com.sevtinge.hyperceiler.utils.PropUtils.getProp

val mSupportSV: Boolean by lazy {
    getProp("ro.vendor.audio.volume_super_index_add", 0) != 0
}
