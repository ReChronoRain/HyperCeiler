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
package com.sevtinge.hyperceiler.hook.module.rules.systemui.statusbar.clock

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Choreographer
import android.view.View
import android.widget.TextView
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.api.LazyClass.mNewClockClass
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreAndroidVersion
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import de.robv.android.xposed.XC_MethodHook
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object StatusBarClockNew : BaseHook() {
    private val statusBarClass by lazy {
        loadClass("com.android.systemui.statusbar.views.MiuiClock")
    }

    private val ssRegex by lazy { Regex("(ss|s)") }

    private val secondsFrameCallback = SecondsFrameCallback().initial()

    private val updateTimeMethodCache = ConcurrentHashMap<Class<*>, Method>()

    private val formatExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    private data class CachedClockData(
        val controllerRef: WeakReference<Any>,
        val calendarRef: WeakReference<Any>,
        val calendarClass: Class<*>?,
        val setTimeMethod: Method?,
        val formatMethod: Method?
    )

    private val clockDataCache: MutableMap<TextView, CachedClockData> =
        Collections.synchronizedMap(WeakHashMap())

    private data class StyleSnapshot(
        val bold: Boolean,
        val textSize: Float,
        val textAlignment: Int,
        val lineSpacingMult: Float,
        val paddingLeft: Int,
        val paddingTop: Int,
        val paddingRight: Int,
        val width: Int
    )

    private val styleSnapshotMap: MutableMap<TextView, StyleSnapshot> =
        Collections.synchronizedMap(WeakHashMap())

    private val sBold by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_clock_bold")
    }
    private val bBold by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_clock_big_bold")
    }
    private val nBold by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_clock_small_bold")
    }
    private val pBold by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_clock_pad_bold")
    }
    private val isSync by lazy {
        mPrefsMap.getBoolean("system_ui_disable_clock_synch")
    }
    private val isHidePClock by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_clock_pad_hide")
    }
    private val clockSizeS by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_size_1", 12)
    }
    private val clockSizeB by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_size_2", 50)
    }
    private val clockSizeN by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_size_3", 12)
    }
    private val clockSizeP by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_size_4", 12)
    }
    private val clockTextSpacing by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_double_spacing_margin_1", 16)
    }
    private val sClockLeftMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_left_margin_1", 0)
    }
    private val sClockRightMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_right_margin_1", 0)
    }
    private val sClockVerticalOffset by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_vertical_offset_1", 12)
    }
    private val fixedWidth by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_fixedcontent_width_1", 30)
    }
    private val bClockLeftMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_left_margin_2", 0)
    }
    private val bClockRightMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_right_margin_2", 0)
    }
    private val bClockVerticalOffset by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_vertical_offset_2", 12)
    }
    private val nClockLeftMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_left_margin_3", 0)
    }
    private val nClockRightMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_right_margin_3", 0)
    }
    private val nClockVerticalOffset by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_vertical_offset_3", 12)
    }
    private val pClockLeftMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_left_margin_4", 0)
    }
    private val pClockRightMargin by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_right_margin_4", 0)
    }
    private val pClockVerticalOffset by lazy {
        mPrefsMap.getInt("system_ui_statusbar_clock_vertical_offset_4", 12)
    }
    private val clockAlign by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_clock_double_1", 0)
    }

    // 时钟格式
    private val getFormatS by lazy {
        mPrefsMap.getString("system_ui_statusbar_clock_editor_s", "HH:mm")
    }
    private val getFormatB by lazy {
        mPrefsMap.getString("system_ui_statusbar_clock_editor_b", "HH:mm")
    }
    private val getFormatN by lazy {
        mPrefsMap.getString("system_ui_statusbar_clock_editor_n", "")
    }
    private val getFormatP by lazy {
        mPrefsMap.getString("system_ui_statusbar_clock_editor_p", "")
    }
    private val getClockStyle by lazy {
        mPrefsMap.getStringAsInt("system_ui_statusbar_clock_style", 0)
    }

    private val safeFormatS by lazy {
        safeSplitFirst(getFormatS)
    }
    private val safeFormatB by lazy {
        safeSplitFirst(getFormatB)
    }
    private val safeFormatN by lazy {
        safeSplitFirst(getFormatN)
    }
    private val safeFormatP by lazy {
        safeSplitFirst(getFormatP)
    }
    private val sClockName by lazy {
        if (getFormatN.isNullOrEmpty()) {
            when (getClockStyle) {
                0 -> safeFormatS
                1 -> "$safeFormatS\nM/d E"
                else -> "M/d E\n$safeFormatS"
            }
        } else {
            when (getClockStyle) {
                0 -> safeFormatS
                1 -> "$safeFormatS\n$safeFormatN"
                else -> "$safeFormatN\n$safeFormatS"
            }
        }
    }

    override fun init() {
        statusBarClass.constructorFinder()
            .filterByParamCount(3)
            .filterByParamTypes {
                it[0] == Context::class.java
            }.first().createAfterHook { param ->
                runCatching {
                    val miuiClock = param.thisObject as TextView
                    val miuiClockName = miuiClock.resources.getResourceEntryName(miuiClock.id)
                        ?: return@createAfterHook

                    val isSec = setOf(
                        ssRegex.containsMatchIn(getFormatS) && isSync && miuiClockName == "clock",
                        ssRegex.containsMatchIn(getFormatS) && !isSync && miuiClockName in setOf(
                            "clock",
                            "big_time"
                        ),
                        ssRegex.containsMatchIn(getFormatB) && isSync && miuiClockName == "big_time",
                        ssRegex.containsMatchIn(getFormatN) && miuiClockName == "date_time",
                        ssRegex.containsMatchIn(getFormatP) && miuiClockName == "pad_clock"
                    ).any { it }

                    // miuiClockName 内部标签分类如下
                    // clock 竖屏状态栏时钟
                    // big_time 通知中心时钟
                    // horizontal_time 横屏通知中心时钟
                    // date_time 通知中心日期时钟
                    // pad_clock Pad 状态栏日期时钟
                    if (miuiClockName == "pad_clock" && isHidePClock) {
                        miuiClock.apply {
                            setTextSize(
                                TypedValue.COMPLEX_UNIT_DIP,
                                0f
                            )
                            setPaddingRelative(1, 0, 0, 0)
                        }
                    }

                    if (getClockStyle != 0 && miuiClockName == "clock")
                        miuiClock.isSingleLine = false

                    if (isSec) {
                        val updateTimeMethod =
                            updateTimeMethodCache.computeIfAbsent(miuiClock.javaClass) { cls ->
                                runCatching {
                                    findMethodInHierarchy(cls, "updateTime")
                                }.getOrNull()!!
                            }

                        secondsFrameCallback.registerClock(miuiClock, updateTimeMethod)
                    }
                }
            }

        // 设置格式
        statusBarClass.methodFinder()
            .filterByName("updateTime")
            .single().createBeforeHook {
                runCatching {
                    applyMiuiClockStyleAndFormat(it)
                }
            }

        if (!isMoreAndroidVersion(36)) {
            // 在 Android 16 的 OS2.0.230.12.WOCCNXM 版本中，未发现此类名
            mNewClockClass.methodFinder()
                .filterByName("updateTime")
                .single().createBeforeHook {
                    runCatching {
                        applyMiuiClockStyleAndFormat(it)
                    }
                }
        }
    }

    private fun applyMiuiClockStyleAndFormat(hook: XC_MethodHook.MethodHookParam) {
        val textV = hook.thisObject as TextView
        val context = textV.context
        val miuiClockName = textV.resources.getResourceEntryName(textV.id) ?: return
        val isHide =
            (isHidePClock && miuiClockName == "pad_clock") || miuiClockName == "normal_control_center_date_view"
        val isEmpty = setOf(
            getFormatN.isEmpty() && miuiClockName == "date_time",
            getFormatP.isEmpty() && miuiClockName == "pad_clock",
            miuiClockName == "horizontal_time"
        ).any { it }

        if (isHide) return
        setMiuiClockStyle(miuiClockName, textV)

        if (isEmpty) return
        setMiuiClockFormat(context, miuiClockName, textV)
        hook.result = null
    }

    private fun setMiuiClockStyle(name: String, text: TextView) {
        val shouldUseBold = setOf(
            sBold && name == "clock",
            bBold && name == "big_time",
            nBold && name in setOf("date_time", "horizontal_time"),
            pBold && name == "pad_clock"
        ).any { it }

        val expectedTextSize = when {
            clockSizeS != 12 && name == "clock" -> clockSizeS.toFloat()
            // TODO：通知中心大时钟无法调整字体大小，待排查
            clockSizeB != 50 && name == "big_time" -> clockSizeB.toFloat()
            clockSizeN != 12 && name in setOf(
                "date_time",
                "horizontal_time"
            ) -> clockSizeN.toFloat()
            clockSizeP != 12 && name == "pad_clock" -> clockSizeP.toFloat()
            else -> -1f
        }

        val expectedAlign = if (getClockStyle != 0 && name == "clock") {
            when (clockAlign) {
                1 -> View.TEXT_ALIGNMENT_CENTER
                2 -> View.TEXT_ALIGNMENT_TEXT_END
                else -> View.TEXT_ALIGNMENT_TEXT_START
            }
        } else null

        val expectedLineSpacingMult =
            if (getClockStyle != 0 && name == "clock") clockTextSpacing * 0.05f else null

        val left = when (name) {
            "clock" -> dp2px(sClockLeftMargin.toFloat())
            "big_time" -> dp2px(bClockLeftMargin.toFloat())
            "pad_clock" -> dp2px(pClockLeftMargin.toFloat())
            else -> dp2px(nClockLeftMargin.toFloat())
        }
        val right = when (name) {
            "clock" -> dp2px(sClockRightMargin.toFloat())
            "big_time" -> dp2px(bClockRightMargin.toFloat())
            "pad_clock" -> dp2px(pClockRightMargin.toFloat())
            else -> dp2px(nClockRightMargin.toFloat())
        }
        val top = when (name) {
            "clock" -> if (sClockVerticalOffset != 12) dp2px((sClockVerticalOffset - 12) * 0.5f) else 0
            "big_time" -> if (bClockVerticalOffset != 12) dp2px((bClockVerticalOffset - 12) * 0.5f) else 0
            "pad_clock" -> if (pClockVerticalOffset != 12) dp2px((pClockVerticalOffset - 12) * 0.5f) else 0
            else -> if (nClockVerticalOffset != 12) dp2px((nClockVerticalOffset - 12) * 0.5f) else 0
        }
        val expectedWidth = if (name == "clock" && fixedWidth > 30) {
            (text.resources.displayMetrics.density * fixedWidth).toInt()
        } else text.width

        val old = styleSnapshotMap[text]
        val newSnapshot = StyleSnapshot(
            bold = shouldUseBold,
            textSize = if (expectedTextSize > 0) expectedTextSize else text.textSize,
            textAlignment = expectedAlign ?: text.textAlignment,
            lineSpacingMult = expectedLineSpacingMult ?: 0.8f,
            paddingLeft = left,
            paddingTop = top,
            paddingRight = right,
            width = expectedWidth
        )

        if (old == newSnapshot) return

        if (newSnapshot.bold) text.typeface = Typeface.DEFAULT_BOLD
        if (expectedTextSize > 0) text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, expectedTextSize)
        if (expectedAlign != null) text.textAlignment = expectedAlign
        if (expectedLineSpacingMult != null) text.setLineSpacing(0f, expectedLineSpacingMult)
        text.setPaddingRelative(left, top, right, 0)
        if (expectedWidth != text.width && expectedWidth > 0) text.width = expectedWidth

        styleSnapshotMap[text] = newSnapshot
    }

    private fun setMiuiClockFormat(context: Context?, name: String, textV: TextView?) {
        if (context == null || textV == null) return

        // 尝试从缓存获取 controller / calendar / method
        val cached = clockDataCache[textV]
        val cachedData = runCatching {
            if (cached != null) return@runCatching cached
            // 通过反射取 controller & calendar，仅在首次或缓存失效时执行
            val controller =
                textV.getObjectField("mMiuiStatusBarClockController") ?: return@runCatching null
            val calendar = controller.getObjectField("mCalendar") ?: return@runCatching null
            val calClass = calendar.javaClass
            val setTime = findMethodInHierarchy(calClass, "setTimeInMillis", Long::class.java)
            val format = findMethodInHierarchy(
                calClass,
                "format",
                Context::class.java,
                StringBuilder::class.java,
                StringBuilder::class.java
            )
            val cd = CachedClockData(
                WeakReference(controller),
                WeakReference(calendar),
                calClass,
                setTime,
                format
            )
            clockDataCache[textV] = cd
            cd
        }.getOrNull() ?: return

        val localFormatSb = StringBuilder(64)
        when (name) {
            "clock" -> localFormatSb.append(sClockName)
            "big_time" -> if (isSync) localFormatSb.append(safeFormatB) else localFormatSb.append(safeFormatS)
            "pad_clock" -> localFormatSb.append(safeFormatP)
            else -> localFormatSb.append(safeFormatN)
        }

        val calendarObj = cachedData.calendarRef.get() ?: run {
            clockDataCache.remove(textV)
            return
        }
        val setTimeMethod = cachedData.setTimeMethod
        val formatMethod = cachedData.formatMethod

        formatExecutor.submit {
            try {
                if (setTimeMethod != null) {
                    setTimeMethod.isAccessible = true
                    setTimeMethod.invoke(calendarObj, System.currentTimeMillis())
                } else {
                    calendarObj.callMethod("setTimeInMillis", System.currentTimeMillis())
                }

                if (formatMethod != null) {
                    val tb = StringBuilder(128)
                    formatMethod.isAccessible = true
                    formatMethod.invoke(calendarObj, context, tb, localFormatSb)
                    val final = tb.toString()
                    textV.post { textV.text = final }
                } else {
                    val tb = StringBuilder(128)
                    calendarObj.callMethod("format", context, tb, localFormatSb)
                    val final = tb.toString()
                    textV.post { textV.text = final }
                }
            } catch (_: Throwable) {
                // 若失败，移除缓存以后续重试
                clockDataCache.remove(textV)
            }
        }
    }


    private fun safeSplitFirst(str: String?): String {
        return str?.split("\n")?.firstOrNull() ?: ""
    }

    private fun findMethodInHierarchy(
        cls: Class<*>,
        name: String,
        vararg params: Class<*>
    ): Method? {
        var c: Class<*>? = cls
        while (c != null) {
            try {
                val m = c.getDeclaredMethod(name, *params)
                return m
            } catch (_: NoSuchMethodException) {
                // continue to superclass
            }
            c = c.superclass
        }
        return null
    }

    private class SecondsFrameCallback : Choreographer.FrameCallback {
        private val choreographer = Choreographer.getInstance()!!
        private val clockMap = HashMap<TextView, Method>()

        fun initial(): SecondsFrameCallback {
            choreographer.postFrameCallback(this)
            return this
        }

        override fun doFrame(frameTimeNanos: Long) {
            if (clockMap.isEmpty()) {
                choreographer.postFrameCallbackDelayed(
                    this,
                    1000 - (System.currentTimeMillis() % 1000)
                )
                return
            }

            for (entry in clockMap) {
                val view = entry.key
                val method = entry.value
                if (!view.isAttachedToWindow)
                    continue

                runCatching {
                    method.invoke(view)
                }
            }

            choreographer.postFrameCallbackDelayed(
                this,
                1000 - (System.currentTimeMillis() % 1000)
            )
        }

        fun registerClock(textView: TextView, updateTimeMethod: Method) {
            if (!clockMap.contains(textView)) {
                updateTimeMethod.isAccessible = true
                clockMap[textView] = updateTimeMethod
                textView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {}

                    override fun onViewDetachedFromWindow(v: View) {
                        clockMap.remove(v)
                        v.removeOnAttachStateChangeListener(this)
                    }
                })
            }
        }
    }
}
