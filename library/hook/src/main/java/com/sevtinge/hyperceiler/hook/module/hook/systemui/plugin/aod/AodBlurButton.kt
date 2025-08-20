package com.sevtinge.hyperceiler.hook.module.hook.systemui.plugin.aod

import android.widget.ImageView
import com.sevtinge.hyperceiler.hook.module.base.tool.HookTool
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api.ShortcutEntity
import com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen.BlurButton
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.setBooleanField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

object AodBlurButton {
    private val hyperBlur by lazy {
        HookTool.mPrefsMap.getBoolean("system_ui_lock_screen_hyper_blur_button")
    }
    private val blurBotton by lazy {
        BlurButton.isTransparencyLow(
            HookTool.mPrefsMap.getInt(
                "system_ui_lock_screen_blur_button_bg_color",
                0
            )
        )
    }

    fun initLoader(classLoader: ClassLoader) {
        val aodPlugin = ClassUtil.loadClass(
            "com.miui.keyguard.shortcuts.controller.ShortcutViewLayoutController",
            classLoader
        )

        aodPlugin.methodFinder().filterByName("updateShortcutView").first()
            .createAfterHook {
                val controller = it.thisObject
                val getShortcutEntity = ShortcutEntity(it.args[0])
                val shortcutViewLeft = controller.getObjectFieldAs<ImageView>("shortcutViewLeft")
                val shortcutViewRight = controller.getObjectFieldAs<ImageView>("shortcutViewRight")

                if (blurBotton) controller.setBooleanField(
                    "isBottomIconRectIsDeep",
                    BlurButton.isColorDark(
                        HookTool.mPrefsMap.getInt(
                            "system_ui_lock_screen_blur_button_bg_color",
                            0
                        )
                    )
                )

                if (hyperBlur) {
                    BlurButton.addHyBlur(shortcutViewLeft)
                    BlurButton.addHyBlur(shortcutViewRight)
                } else {
                    if (getShortcutEntity.drawable != null) {
                        shortcutViewLeft.background =
                            BlurButton.setNewBackgroundBlur(shortcutViewLeft)
                        shortcutViewRight.background =
                            BlurButton.setNewBackgroundBlur(shortcutViewRight)
                    } else {
                        shortcutViewLeft.background = null
                        shortcutViewRight.background = null
                    }
                }
            }
    }
}
