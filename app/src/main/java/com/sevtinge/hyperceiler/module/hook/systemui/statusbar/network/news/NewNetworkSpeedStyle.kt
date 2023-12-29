package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network.news

import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.ResourcesHook
import com.sevtinge.hyperceiler.utils.devicesdk.dp2px
import com.sevtinge.hyperceiler.utils.getObjectField

object NewNetworkSpeedStyle : BaseHook() {
    private val viewInitedTag = ResourcesHook.getFakeResId("view_inited_tag")

    private val fixedWidth by lazy {
        mPrefsMap.getInt("system_ui_statusbar_network_speed_fixedcontent_width", 10)
    }
    private val networkStyle by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_network_speed_style", 0)
    }
    private val mNetworkCostomEnable by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_network_speed_all_status_enable")
    }

    override fun init() {
        hookAllConstructors("com.android.systemui.statusbar.views.NetworkSpeedView", lpparam.classLoader,
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun after(param: MethodHookParam) {
                    val meter = param.thisObject as View
                    val inited = meter.getTag(viewInitedTag)
                    if (inited == null && "slot_text_icon" != meter.tag) {
                        meter.setTag(viewInitedTag, true)

                        if (fixedWidth > 10) {
                            var lp = meter.layoutParams
                            val viewWidth =
                                (meter.resources.displayMetrics.density * fixedWidth).toInt()
                            if (lp == null) {
                                lp = ViewGroup.LayoutParams(viewWidth, -1)
                            } else {
                                lp.width = viewWidth
                            }
                            meter.setLayoutParams(lp)
                        }
                        meter.postDelayed({
                            val number = meter.getObjectField("mNetworkSpeedNumberText") as TextView
                            val unit = meter.getObjectField("mNetworkSpeedUnitText") as TextView

                            // 隐藏单位控件
                            if (mNetworkCostomEnable && networkStyle != 0) {
                                unit.visibility = View.GONE
                                if (networkStyle == 2 || networkStyle == 4) {
                                    number.isSingleLine = false
                                    number.maxLines = 2
                                }

                                // 加粗
                                textFont(number)

                                // 偏移量设置
                                margin(number)

                                // 水平对齐
                                align(number)

                                // 网速字体大小调整
                                textSize(number)
                            } else {
                                // 加粗
                                textFont(number)
                                textFont(unit)

                                // 偏移量设置
                                if (mNetworkCostomEnable) {
                                    margin(number)
                                    margin(unit)
                                }

                                // 水平对齐官方的寄，改不了

                                // 网速字体大小调整
                                textSize(number)
                                textSize(unit)
                            }
                        }, 200)
                    }
                }
            })
    }

    private fun textFont(id: TextView) {
        val bold by lazy {
            mPrefsMap.getStringAsInt("system_ui_statusbar_network_speed_font_style", 0)
        }
        when (bold) {
            1 -> id.typeface = Typeface.DEFAULT
            2 -> id.typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun textSize(id: TextView) {
        val fontSize by lazy {
            mPrefsMap.getInt("system_ui_statusbar_network_speed_font_size", 13)
        }
        val fontSizeEnable by lazy {
            mPrefsMap.getBoolean("system_ui_statusbar_network_speed_font_size_enable")
        }
        val networkStyle by lazy {
            mPrefsMap.getStringAsInt("system_ui_statusbar_network_speed_style", 0)
        }
        if (fontSizeEnable) {
            try {
                if (networkStyle == 0 || networkStyle == 2 || networkStyle == 4) {
                    id.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 0.5f)
                } else {
                    id.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize.toFloat())
                }
            } catch (_: Exception) {
            }
        }

        // 双排字体大小设置
        try {
            if (networkStyle == 0 || networkStyle == 2 || networkStyle == 4) {
                var spacing = 0.9f
                if (0.5 * fontSize > 8.5f) {
                    spacing = 0.85f
                }
                id.setLineSpacing(0f, spacing)
            }
        } catch (_: Exception) {
        }
    }

    private fun align(id: TextView) {
        val align by lazy {
            mPrefsMap.getStringAsInt("system_ui_statusbar_network_speed_align", 1)
        }
        when (align) {
            2 -> id.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            3 -> id.textAlignment = View.TEXT_ALIGNMENT_CENTER
            4 -> id.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
        }
    }

    private fun margin(id: TextView) {
        // 左侧间距
        var leftMargin =
            mPrefsMap.getInt("system_ui_statusbar_network_speed_left_margin", 0)
        leftMargin = dp2px(leftMargin * 0.5f)

        // 右侧间距
        var rightMargin =
            mPrefsMap.getInt("system_ui_statusbar_network_speed_right_margin", 0)
        rightMargin = dp2px(rightMargin * 0.5f)

        // 上下偏移量
        var topMargin = 0
        val verticalOffset =
            mPrefsMap.getInt("system_ui_statusbar_network_speed_vertical_offset", 8)
        if (verticalOffset != 8) {
            topMargin = dp2px((verticalOffset - 8) * 0.5f)
        }
        id.setPaddingRelative(leftMargin, topMargin, rightMargin, 0)
    }
}
