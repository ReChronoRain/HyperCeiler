/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.rules.home.other.NativeHomeHooksOS4

/** Rust dispatch + DE bootstrap + authenticated WMS; no Java Activity or pre-fork callback thread. */
object HomeGestureOS4 : BaseHook() {
    override fun init() {
        NativeHomeHooksOS4.init()
    }
}
