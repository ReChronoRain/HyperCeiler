package com.sevtinge.cemiuiler.module.hook.systemui.statusbar

import android.widget.LinearLayout
import com.sevtinge.cemiuiler.R
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.ResourcesHook
import de.robv.android.xposed.XposedHelpers

object StatusResFindHook : BaseHook() {
    val textIconTagId: Int = ResourcesHook.getFakeResId("text_icon_tag")
    val viewInitedTag: Int = ResourcesHook.getFakeResId("view_inited_tag")
    private var statusbarTextIconLayoutResId :Int = 0
    var newStyle = false

    override fun init() {
        // 查找是否为新版状态栏
        val mNetworkSpeedViewCls = XposedHelpers.findClassIfExists(
            "com.android.systemui.statusbar.views.NetworkSpeedView", lpparam.classLoader
        )
        if (mNetworkSpeedViewCls != null) {
            newStyle =
                LinearLayout::class.java.isAssignableFrom(mNetworkSpeedViewCls)
        }

        statusbarTextIconLayoutResId = if (newStyle) {
            mResHook.addResource("statusbar_text_icon", R.layout.statusbar_text_icon_new)
        } else {
            mResHook.addResource("statusbar_text_icon", R.layout.statusbar_text_icon)
        }

    }
}
