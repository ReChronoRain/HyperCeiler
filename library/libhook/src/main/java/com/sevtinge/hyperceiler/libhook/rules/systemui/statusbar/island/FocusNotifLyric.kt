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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.island

import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.view.Choreographer
import android.widget.TextView
import com.hchen.superlyricapi.SuperLyricData
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.appbase.systemui.MusicBaseHook
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getFloatField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.interceptHookConstructor
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.interceptHookMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setFloatField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setLongField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// author git@wuyou-123
// co-author git@lingqiqi5211
object FocusNotifLyric : MusicBaseHook() {
    private const val STATE_SCROLLING_TEXT_VIEW = "FocusNotifLyric.scrollingTextView"
    private var speed = -0.1f
    private var lastLyric: String? = ""
    private val runnablePool = mutableMapOf<Int, Runnable>()
    private val focusTextViewList = mutableListOf<TextView>()
    private val sCollectFocusedTextViews = ThreadLocal<Boolean>()
    private val textViewWidth by lazy {
        PrefsBridge.getInt("system_ui_statusbar_music_width", 0)
    }
    private val MARQUEE_DELAY by lazy {
        PrefsBridge.getInt("system_ui_statusbar_music_scroll_delay", 12) * 100L
    }
    private val SPEED_INCREASE by lazy {
        PrefsBridge.getInt("system_ui_statusbar_music_speed", 18) * 0.1f
    }
    private val isShowNotific by lazy {
        PrefsBridge.getBoolean("system_ui_statusbar_music_show_notific")
    }
    private val isShowApp by lazy {
        PrefsBridge.getBoolean("system_ui_statusbar_music_show_app")
    }

    override fun init() {
        BaseHook.getHotReloadRuntimeState(STATE_SCROLLING_TEXT_VIEW, TextView::class.java)
            ?.let(::installNoopRestartCallback)
        registerHotReloadCleanup {
            focusTextViewList.forEach { textView ->
                runnablePool.remove(textView.hashCode())?.let(textView::removeCallbacks)
            }
            runnablePool.clear()
            focusTextViewList.clear()
        }

        // 拦截构建通知的函数
        if (!isShowNotific) {
            loadClass("com.android.systemui.statusbar.notification.row.NotifBindPipeline").findMethod { name("requestPipelineRun") }.createBeforeHook {
                    val statusBarNotification =
                        it.args[0]?.getObjectFieldOrNullAs<StatusBarNotification>("mSbn")
                    if (statusBarNotification!!.notification.channelId == CHANNEL_ID) {
                        it.result = null
                    }
                }
        }

        loadClass("com.android.systemui.statusbar.widget.FocusedTextView").interceptHookConstructor(
            android.content.Context::class.java,
            android.util.AttributeSet::class.java,
            Int::class.java
        ) { chain ->
            val result = chain.proceed()
            if (sCollectFocusedTextViews.get() == true) {
                focusTextViewList += chain.thisObject as TextView
            }
            result
        }

        loadClass("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment").interceptHookMethod(
            "onCreateView"
        ) { chain ->
            sCollectFocusedTextViews.set(true)
            try {
                chain.proceed()
            } finally {
                sCollectFocusedTextViews.remove()
            }
        }

        // 构建通知栏通知函数
        // loadClass("com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector").findMethod { name("createRemoteViews") }
        // 重设 mLastAnimationTime，取消闪烁动画(让代码以为刚播放过动画，所以这次不播放)
        loadClass("com.android.systemui.statusbar.phone.FocusedNotifPromptView").findMethod { name("setData") }.createBeforeHook {
                it.thisObject.setLongField("mLastAnimationTime", System.currentTimeMillis())
            }

    }

    fun initLoader(classLoader: ClassLoader) {
        runCatching {
            loadClass("miui.systemui.notification.NotificationSettingsManager", classLoader).findMethod { name("canShowFocus") }.createHook {
                    // 允许全部应用发送焦点通知
                    returnConstant(true)
                }

        }.onFailure {
            XposedLog.e(TAG, "canShowFocus failed, ${it.message}")
        }
        runCatching {
            loadClass("miui.systemui.notification.NotificationSettingsManager", classLoader).findMethod { name("canCustomFocus") }.createHook {
                    // 允许全部应用发送自定义焦点通知
                    returnConstant(true)
                }

        }.onFailure {
            XposedLog.e(TAG, "canCustomFocus failed, ${it.message}")
        }

        if (!isMoreHyperOSVersion(3f)) return
        runCatching {
            loadClass($$"miui.systemui.notification.auth.AuthManager$AuthServiceCallback$onAuthResult$1",classLoader).findMethod { name("invokeSuspend") }.createHook {
                    before { param ->
                        val obj = param.thisObject
                        // 访问字段 "$authBundle"
                        val bundle = obj.getObjectField($$"$authBundle") as Bundle
                        XposedLog.d(TAG, "authBundle result_code:${bundle.getInt("result_code", 114514)}")
                        bundle.putInt("result_code",0)
                    }
                }
        }.onFailure {
            XposedLog.e(TAG, "invokeSuspend failed, ${it.message}")
        }
        // 启用debug日志
        // setStaticObject(loadClass("miui.systemui.notification.NotificationUtil", classLoader), "DEBUG", true)
    }

    override fun onSuperLyric(packageName: String?, data: SuperLyricData) {
        val lyric = data.lyric
        focusTextViewList.forEach { textView ->
            textView.post {
                if (lastLyric == textView.text) {
                    if (com.sevtinge.hyperceiler.libhook.base.BaseHook.getAdditionalInstanceField(textView, "is_scrolling") == 1) {
                        val m0 = textView.getObjectFieldOrNull("mMarquee")
                        m0?.apply {
                            // 设置速度并且调用停止函数,重置歌词位置
                            setFloatField("mPixelsPerMs", 0f)
                            callMethod("stop")
                        }
                    }
                    textView.text = lyric?.text
                    lastLyric = lyric?.text
                }
                val key = textView.hashCode()
                val startScroll = runnablePool.getOrPut(key) {
                    Runnable {
                        startScroll(textView)
                        runnablePool.remove(key)
                    }
                }
                textView.handler?.removeCallbacks(startScroll)
                textView.postDelayed(startScroll, MARQUEE_DELAY)
            }
        }

        if (!isShowApp && data.lyric!!.text.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                sendNotification(data.lyric!!.text, data)
            }
        }
    }

    private fun startScroll(textView: TextView) {
        runCatching {
            // 开始滚动
            textView.callMethod("setMarqueeRepeatLimit", 1)
            textView.callMethod("startMarqueeLocal")
            val key = textView.hashCode()
            val m = textView.getObjectFieldOrNull("mMarquee") ?: return
            if (speed == -0.1f) {
                // 初始化滚动速度
                speed = m.getFloatField("mPixelsPerMs") * SPEED_INCREASE
            }

            val width = (textView.width - textView.compoundPaddingLeft - textView.compoundPaddingRight - textViewWidth).toFloat()
            val lineWidth = textView.layout?.getLineWidth(0)

            if (lineWidth != null) {
                // 重设最大滚动宽度,只能滚动到文本结束
                m.setFloatField("mMaxScroll", lineWidth - width)
                // 重设速度
                m.setFloatField("mPixelsPerMs", speed)
                // 移除回调,防止滚动结束之后重置滚动位置。
                // 重载时会用新 generation 的 callback 覆盖它，避免宿主 Marquee 持有旧 classloader。
                installNoopRestartCallback(textView)
                BaseHook.putHotReloadRuntimeState(STATE_SCROLLING_TEXT_VIEW, textView)
                // 滚动完成后清理状态
                val finishScroll = Runnable {
                    com.sevtinge.hyperceiler.libhook.base.BaseHook.setAdditionalInstanceField(textView, "is_scrolling", 1)
                    runnablePool.remove(key) //移除任务引用
                }
                textView.postDelayed(finishScroll, computeScrollDuration(lineWidth, width, speed)) // 根据速度和距离计算时长
                BaseHook.registerHotReloadCleanup { textView.removeCallbacks(finishScroll) }
            }
        }.onFailure {
            XposedLog.e(TAG, lpparam.packageName, "error: ${it.message}")
        }
    }

    private fun computeScrollDuration(lineWidth: Float, width: Float, speed: Float): Long {
        val maxScroll = (lineWidth - width).coerceAtLeast(0f) // 与 mMaxScroll 一致
        val pixelsPerMs = speed // 与 mPixelsPerMs 一致
        return if (pixelsPerMs > 0) (maxScroll / pixelsPerMs).toLong() else 0L
    }

    private fun installNoopRestartCallback(textView: TextView) {
        textView.getObjectFieldOrNull("mMarquee")?.setObjectField(
            "mRestartCallback", Choreographer.FrameCallback {}
        )
    }


    override fun onStop() {
        if (!isShowApp) cancelNotification()
    }
}
