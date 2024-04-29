package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter

import android.util.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.*
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*

class MediaControlPanelTimeViewTextSize : BaseHook() {

    private val textSize by lazy {
        mPrefsMap.getInt("system_ui_control_center_media_control_time_view_text_size", 13).toFloat()
    }

    //from https://github.com/YuKongA/MediaControl-BlurBg
    override fun init() {
        EzXHelper.initHandleLoadPackage(lpparam)
        EzXHelper.setLogTag(TAG)
        EzXHelper.setToastTag(TAG)

        val miuiMediaControlPanel =
            ClassUtils.loadClassOrNull("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaControlPanel")

        miuiMediaControlPanel?.methodFinder()?.filterByName("bindPlayer")?.first()
            ?.createAfterHook {
                val mMediaViewHolder =
                    it.thisObject.objectHelper().getObjectOrNullUntilSuperclass("mMediaViewHolder")
                        ?: return@createAfterHook
                val elapsedTimeView =
                    mMediaViewHolder.objectHelper().getObjectOrNullAs<TextView>("elapsedTimeView")
                val totalTimeView =
                    mMediaViewHolder.objectHelper().getObjectOrNullAs<TextView>("totalTimeView")

                elapsedTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
                totalTimeView?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
            }
    }
}