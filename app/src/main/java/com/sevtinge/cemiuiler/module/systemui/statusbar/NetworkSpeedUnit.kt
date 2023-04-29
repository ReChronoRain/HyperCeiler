package com.sevtinge.cemiuiler.module.systemui.statusbar

import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XposedBridge

object NetworkSpeedUnit : BaseHook() {
    override fun init() {
        hookAllConstructors(
            "com.android.systemui.statusbar.views.NetworkSpeedView",
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    //值和单位双排显示 + 上下行网速双排显示 + 字体大小调整
                    val doubleLine =
                        (mPrefsMap.getBoolean("system_ui_statusbar_network_speed_detailed") && mPrefsMap.getBoolean("system_ui_statusbar_network_speed_show_up_down"))
                    val dualRow =
                        mPrefsMap.getBoolean("system_ui_statusbar_network_speed_fakedualrow")
                    val meter = param.thisObject as TextView
                    if (meter.tag == null || "slot_text_icon" != meter.tag) {
                        val fontSize =
                            mPrefsMap.getInt("system_ui_statusbar_network_speed_font_size", 13)
                        /*if (dualRow) {
                            if (fontSize > 23 || fontSize == 13) fontSize = 16
                        } else {
                            if (fontSize < 20 && fontSize != 13) fontSize = 27
                        }*/
                        try {
                            if (doubleLine || dualRow) {
                                meter.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 0.5f)
                            }
                        } catch (e: Throwable) {
                            XposedBridge.log(e)
                        }
                        //网速加粗
                        if (mPrefsMap.getBoolean("system_ui_statusbar_network_speed_bold")) {
                            meter.typeface = Typeface.DEFAULT_BOLD
                        }
                        val res = meter.resources

                        //左侧间距
                        var leftMargin =
                            mPrefsMap.getInt("system_ui_statusbar_network_speed_left_margin", 0)
                        leftMargin = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            leftMargin * 0.5f,
                            res.displayMetrics
                        ).toInt()
                        //右侧间距
                        var rightMargin = mPrefsMap.getInt(
                            "system_ui_statusbar_network_speed_right_margin",
                            0
                        )
                        rightMargin = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            rightMargin * 0.5f,
                            res.displayMetrics
                        ).toInt()
                        //上下偏移量
                        var topMargin = 0
                        val verticalOffset = mPrefsMap.getInt(
                            "system_ui_statusbar_network_speed_vertical_offset",
                            8
                        )
                        if (verticalOffset != 8) {
                            val marginTop = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                (verticalOffset - 8) * 0.5f,
                                res.displayMetrics
                            )
                            topMargin = marginTop.toInt()
                        }
                        meter.setPaddingRelative(leftMargin, topMargin, rightMargin, 0)

                        //水平对齐
                        val align = mPrefsMap.getStringAsInt(
                            "system_ui_statusbar_network_speed_align",
                            1
                        )
                        when (align) {
                            2 -> meter.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                            3 -> meter.textAlignment = View.TEXT_ALIGNMENT_CENTER
                            4 -> meter.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
                        }
                        try {
                            if (doubleLine || dualRow) {
                                var spacing = 0.9f
                                meter.isSingleLine = false
                                meter.maxLines = 2
                                if (fontSize > 8.5f) {
                                    spacing = 0.9f
                                }
                                meter.setLineSpacing(0f, spacing)
                            }
                        } catch (e: Throwable) {
                            XposedBridge.log(e)
                        }
                    }
                }
            }
        )
    }
}