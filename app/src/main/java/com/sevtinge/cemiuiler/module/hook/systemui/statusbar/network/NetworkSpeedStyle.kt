package com.sevtinge.cemiuiler.module.hook.systemui.statusbar.network

import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.module.hook.systemui.statusbar.StatusResFindHook.newStyle
import com.sevtinge.cemiuiler.module.hook.systemui.statusbar.StatusResFindHook.viewInitedTag
import com.sevtinge.cemiuiler.utils.devicesdk.dp2px
import com.sevtinge.cemiuiler.utils.devicesdk.isAndroidR
import de.robv.android.xposed.XposedHelpers

object NetworkSpeedStyle : BaseHook() {
    private val doubleLine by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_network_speed_detailed") &&
        mPrefsMap.getBoolean("system_ui_statusbar_network_speed_show_up_down")
    }
    private val dualRow by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_network_speed_fakedualrow")
    }
    private val fontSize by lazy {
        mPrefsMap.getInt("system_ui_statusbar_network_speed_font_size", 13)
    }
    private val fontSizeEnable by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_network_speed_font_size_enable")
    }
    private val bold by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_network_speed_bold")
    }

    private val fixedWidth =
        mPrefsMap.getInt("system_ui_statusbar_network_speed_fixedcontent_width", 10)
    private var leftMargin =
        mPrefsMap.getInt("system_ui_statusbar_network_speed_left_margin", 0)
    private var rightMargin =
        mPrefsMap.getInt("system_ui_statusbar_network_speed_right_margin", 0)
    private val verticalOffset =
        mPrefsMap.getInt("system_ui_statusbar_network_speed_vertical_offset", 8)
    private var topMargin = 0

    override fun init() {
        if (dualRow) {
            mResHook.setObjectReplacement(lpparam.packageName, "string", "network_speed_suffix", "%1\$s\n%2\$s")
        }

        if (isAndroidR()) {
            // Android 11 or MIUI12.5 Need to hook Statusbar in Screen Lock interface, to set front size
            // Thanks for CustoMIUIzerMod
            loadClass("com.android.systemui.statusbar.phone.MiuiKeyguardStatusBarView").methodFinder().first {
                name == "onDensityOrFontScaleChanged"
            }.createHook {
               after { params ->
                   val meter = XposedHelpers.getObjectField(params.thisObject, "mNetworkSpeedView") as TextView

                    // 网速字体大小调整
                    if (fontSizeEnable) {
                        try {
                            if (doubleLine || dualRow) {
                                meter.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 0.5f)
                            } else {
                                meter.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize.toFloat())
                            }
                        } catch (e: Exception) {
                            logE(e)
                        }
                    }
                }
            }
        }

        hookAllConstructors("com.android.systemui.statusbar.views.NetworkSpeedView",
            object : MethodHook() {
                @Throws(Throwable::class)
                override fun after(param: MethodHookParam) {
                    val meter = param.thisObject as View
                    val invited = meter.getTag(viewInitedTag)
                    if (invited == null && "slot_text_icon" != meter.tag) {
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
                        meter.postDelayed({ initNetSpeedStyle(meter) }, 200)
                    }
                }
            }
        )
    }

    private fun getIconTextView(iconView: View): TextView {
        return if (newStyle) {
            XposedHelpers.getObjectField(iconView, "mNetworkSpeedNumberText") as TextView
        } else iconView as TextView
    }

    private fun initNetSpeedStyle(meter: View) {
        val iconTextView: TextView = getIconTextView(meter)
        // 值和单位双排显示 + 上下行网速双排显示 + 网速字体大小调整
        if (fontSizeEnable) {
            try {
                if (doubleLine || dualRow) {
                    if (newStyle) {
                        val unitView =
                            XposedHelpers.getObjectField(meter, "mNetworkSpeedUnitText") as View
                        unitView.visibility = View.GONE
                    }
                    iconTextView.setTextSize(
                        TypedValue.COMPLEX_UNIT_DIP,
                        fontSize * 0.5f
                    )
                } else {
                    iconTextView.setTextSize(
                        TypedValue.COMPLEX_UNIT_DIP,
                        fontSize.toFloat()
                    )
                }
            } catch (e: Exception) {
                logE(e)
            }
        }

        // 网速加粗
        if (bold) {
            iconTextView.typeface = Typeface.DEFAULT_BOLD
        }

        // 边距调整
        leftMargin = dp2px(leftMargin * 0.5f)
        rightMargin = dp2px(rightMargin * 0.5f)
        if (verticalOffset != 8) {
            topMargin = dp2px((verticalOffset - 8) * 0.5f)
        }
        iconTextView.setPaddingRelative(leftMargin, topMargin, rightMargin, 0)

        // 水平对齐
        when (mPrefsMap.getStringAsInt("system_ui_statusbar_network_speed_align", 1)) {
            2 -> iconTextView.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            3 -> iconTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            4 -> iconTextView.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
        }
        try {
            if (doubleLine || dualRow) {
                var spacing = 0.9f
                iconTextView.isSingleLine = false
                iconTextView.maxLines = 2
                if (0.5 * fontSize > 8.5f) {
                    spacing = 0.85f
                }
                iconTextView.setLineSpacing(0f, spacing)
            }
        } catch (e: Exception) {
            logE("setLineSpacing", e)
        }
    }
}
