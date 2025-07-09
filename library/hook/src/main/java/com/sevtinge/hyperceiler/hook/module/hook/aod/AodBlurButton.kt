/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.hook.aod

import android.widget.ImageView
import com.sevtinge.hyperceiler.hook.module.base.tool.HookTool.mPrefsMap
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api.ShortcutEntity
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.BlurButton.addHyBlur
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.BlurButton.isColorDark
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.BlurButton.isTransparencyLow
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.BlurButton.setNewBackgroundBlur
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.setBooleanField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

object AodBlurButton {
    private val hyperBlur by lazy {
        mPrefsMap.getBoolean("system_ui_lock_screen_hyper_blur_button")
    }
    private val blurBotton by lazy {
        isTransparencyLow(mPrefsMap.getInt("system_ui_lock_screen_blur_button_bg_color", 0))
    }

    fun initLoader(classLoader: ClassLoader) {
        val aodPlugin = loadClass("com.miui.keyguard.shortcuts.controller.ShortcutViewLayoutController", classLoader)

        aodPlugin.methodFinder().filterByName("updateShortcutView").first()
            .createAfterHook {
                val controller = it.thisObject
                val getShortcutEntity = ShortcutEntity(it.args[0])
                val shortcutViewLeft = controller.getObjectFieldAs<ImageView>("shortcutViewLeft")
                val shortcutViewRight = controller.getObjectFieldAs<ImageView>("shortcutViewRight")

                if (blurBotton) controller.setBooleanField(
                    "isBottomIconRectIsDeep",
                    isColorDark(mPrefsMap.getInt("system_ui_lock_screen_blur_button_bg_color", 0))
                )

                if (hyperBlur) {
                    addHyBlur(shortcutViewLeft)
                    addHyBlur(shortcutViewRight)
                } else {
                    if (getShortcutEntity.drawable != null) {
                        shortcutViewLeft.background = setNewBackgroundBlur(shortcutViewLeft)
                        shortcutViewRight.background = setNewBackgroundBlur(shortcutViewRight)
                    } else {
                        shortcutViewLeft.background = null
                        shortcutViewRight.background = null
                    }
                }
            }
    }
}
