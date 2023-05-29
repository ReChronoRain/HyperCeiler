package com.sevtinge.cemiuiler.module.systemui.controlcenter

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.Helpers
import de.robv.android.xposed.XposedHelpers


object CompactNotificationsHook : BaseHook() {
    @SuppressLint("DiscouragedApi")
    override fun init() {
        val abHeight = 39

        if (mPrefsMap.getBoolean("system_ui_control_center_compact_notice")) {
            mResHook.setDensityReplacement("android", "dimen", "notification_action_height", abHeight.toFloat())
            mResHook.setDensityReplacement("android", "dimen", "android_notification_action_height", abHeight.toFloat())
            mResHook.setDensityReplacement("android", "dimen", "notification_action_list_height", abHeight.toFloat())
            mResHook.setDensityReplacement("com.android.systemui", "dimen", "notification_row_extra_padding", 0F)
        }

        Helpers.hookAllMethods(
            "com.android.systemui.statusbar.notification.row.wrapper.NotificationViewWrapper", lpparam.classLoader,
            "wrap",
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun after(param: MethodHookParam) {
                    if (param.args.size > 3) return
                    val res = param.result ?: return
                    val mView = XposedHelpers.getObjectField(res, "mView") as View
                    //if (mView.getId() != mView.getResources().getIdentifier("status_bar_latest_event_content", "id", "android")) return;
                    val container = mView.findViewById<FrameLayout>(
                        mView.resources.getIdentifier("actions_container", "id", "android")
                    ) ?: return
                    val density = mView.resources.displayMetrics.density
                    val height = Math.round(density * abHeight).toInt()
                    val actions = container.getChildAt(0) as ViewGroup
                    val lp1 = actions.layoutParams as FrameLayout.LayoutParams
                    lp1.height = height
                    actions.layoutParams = lp1
                    actions.setPadding(0, 0, 0, 0)
                    for (c in 0 until actions.childCount) {
                        val button = actions.getChildAt(c)
                        val lp2 = button.layoutParams as MarginLayoutParams
                        lp2.height = height
                        lp2.bottomMargin = 0
                        lp2.topMargin = 0
                    }
                }
            })
    }

}