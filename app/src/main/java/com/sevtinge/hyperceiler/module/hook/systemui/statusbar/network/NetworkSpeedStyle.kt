package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network

import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.devicesdk.isAndroidR
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils
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

    override fun init() {
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
                           XposedLogUtils.logE(TAG, e)
                       }
                   }
               }
            }
        }

        hookAllConstructors("com.android.systemui.statusbar.views.NetworkSpeedView",
            object : MethodHook() {
                override fun after(param: MethodHookParam) {
                    // 值和单位双排显示 + 上下行网速双排显示
                    val meter = param.thisObject as TextView

                    if (dualRow) {
                        mResHook.setObjectReplacement(lpparam.packageName, "string", "network_speed_suffix", "%1\$s\n%2\$s")
                    }

                    if (meter.tag == null || "slot_text_icon" != meter.tag) {
                        // 网速字体大小调整
                        if (fontSizeEnable) {
                            try {
                                if (doubleLine || dualRow) {
                                    meter.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 0.5f)
                                } else {
                                    meter.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize.toFloat())
                                }
                            } catch (e: Exception) {
                                XposedLogUtils.logE(TAG, e)
                            }
                        }

                        // 网速加粗
                        if (bold) {
                            meter.typeface = Typeface.DEFAULT_BOLD
                        }
                        val res = meter.resources

                        // 左侧间距
                        var leftMargin =
                            mPrefsMap.getInt("system_ui_statusbar_network_speed_left_margin", 0)
                        leftMargin = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            leftMargin * 0.5f,
                            res.displayMetrics
                        ).toInt()

                        // 右侧间距
                        var rightMargin =
                            mPrefsMap.getInt("system_ui_statusbar_network_speed_right_margin", 0)
                        rightMargin = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            rightMargin * 0.5f,
                            res.displayMetrics
                        ).toInt()

                        // 上下偏移量
                        var topMargin = 0
                        val verticalOffset =
                            mPrefsMap.getInt("system_ui_statusbar_network_speed_vertical_offset", 8)
                        if (verticalOffset != 8) {
                            val marginTop = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                (verticalOffset - 8) * 0.5f,
                                res.displayMetrics
                            )
                            topMargin = marginTop.toInt()
                        }
                        meter.setPaddingRelative(leftMargin, topMargin, rightMargin, 0)

                        // 水平对齐
                        when (mPrefsMap.getStringAsInt("system_ui_statusbar_network_speed_align", 1)) {
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
                        } catch (e: Exception) {
                            XposedLogUtils.logE(TAG, e)
                        }
                    }
                }
            }
        )
    }
}
