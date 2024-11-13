package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.v

import android.service.notification.*
import android.view.*
import android.widget.*
import cn.lyric.getter.api.data.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.setStaticObject
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.api.ProjectApi.*
import de.robv.android.xposed.*

object FocusNotifLyric : MusicBaseHook() {
    private val focusTextViewList = mutableListOf<TextView>()
    override fun init() {
        // 拦截构建通知的函数
        loadClass("com.android.systemui.statusbar.notification.row.NotifBindPipeline").methodFinder()
            .filterByName("requestPipelineRun").first().createBeforeHook {
                val statusBarNotification =
                    it.args[0].getObjectField("mSbn") as StatusBarNotification
                    if (statusBarNotification.notification.channelId == CHANNEL_ID) {
                        it.result = null
                    }
            }

        // 拦截初始化状态栏焦点通知文本布局
        var unhook: XC_MethodHook.Unhook? = null
        loadClass("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment").methodFinder()
            .filterByName("onCreateView")
            .first().createHook {
                before {
                    unhook =
                        loadClass("com.android.systemui.statusbar.widget.FocusedTextView").constructorFinder()
                            .filterByParamCount(3)
                            .first().createAfterHook {
                                focusTextViewList += it.thisObject as TextView
                            }
                }
                after {
                    unhook?.unhook()
                }
            }

        // 构建通知栏通知函数
        // loadClass("com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector").methodFinder()
        //     .filterByName("createRemoteViews").first())
        // 重设 mLastAnimationTime，取消闪烁动画(让代码以为刚播放过动画，所以这次不播放)
        loadClass("com.android.systemui.statusbar.phone.FocusedNotifPromptView").methodFinder()
            .filterByName("setData")
            .first().createBeforeHook {
                it.thisObject.setLongField("mLastAnimationTime", System.currentTimeMillis())
            }

        // 拿到插件的 classloader
        loadClass("com.android.systemui.shared.plugins.PluginInstance").methodFinder()
            .filterByName("loadPlugin")
            .first().createAfterHook { p0 ->
                val mPlugin = XposedHelpers.getObjectField(p0.thisObject, "mPlugin")
                val pluginClassLoader = mPlugin::class.java.classLoader
                try {
                    val cl =
                        loadClass("miui.systemui.notification.FocusNotificationPluginImpl", pluginClassLoader)
                    // 过滤 系统界面组件
                    if (cl.isInstance(mPlugin)) {
                        loadClass("miui.systemui.notification.NotificationSettingsManager", pluginClassLoader)
                            .methodFinder().filterByName("canShowFocus")
                            .first().createHook {
                                // 允许全部应用发送焦点通知
                                returnConstant(true)
                            }
                    }

                } catch (e: Exception) {
                    return@createAfterHook
                }
                // 启用debug日志
                if (isDebug()) {
                    setStaticObject(
                        loadClass("miui.systemui.notification.NotificationUtil", pluginClassLoader),
                        "DEBUG",
                        true
                    )
                }
            }
    }

    private const val MARQUEE_DELAY = 1200L
    private var speed = -0.1f
    private const val SPEED_INCREASE = 1.8f

    private val runnablePool = mutableMapOf<Int, Runnable>()
    override fun onUpdate(lyricData: LyricData) {
        val lyric = lyricData.lyric
        focusTextViewList.forEach {
            it.text = lyric
            if (XposedHelpers.getAdditionalStaticField(it, "is_scrolling") == 1) {
                val m0 = it.getObjectField("mMarquee")
                if (m0 != null) {
                    // 设置速度并且调用停止函数,重置歌词位置
                    m0.setFloatField("mPixelsPerMs", 0f)
                    m0.callMethod("stop")
                }
            }
            val startScroll = runnablePool.getOrPut(it.hashCode()) {
                Runnable { startScroll(it) }
            }
            it.handler?.removeCallbacks(startScroll)
            it.postDelayed(startScroll, MARQUEE_DELAY)
        }
    }

    private fun startScroll(textView: TextView) {
        try {
            // 开始滚动
            textView.callMethod("setMarqueeRepeatLimit", 1)
            textView.callMethod("startMarqueeLocal")

            val m = textView.getObjectField("mMarquee") ?: return
            if (speed == -0.1f) {
                // 初始化滚动速度
                speed = m.getFloatField("mPixelsPerMs") * SPEED_INCREASE
            }
            val width =
                (textView.width - textView.compoundPaddingLeft - textView.compoundPaddingRight).toFloat()
            val lineWidth = textView.layout.getLineWidth(0)
            // 重设最大滚动宽度,只能滚动到文本结束
            m.setFloatField("mMaxScroll", lineWidth - width)
            // 重设速度
            m.setFloatField("mPixelsPerMs", speed)
            // 移除回调,防止滚动结束之后重置滚动位置
            m.setObjectField("mRestartCallback", Choreographer.FrameCallback {})
            XposedHelpers.setAdditionalStaticField(textView, "is_scrolling", 1)
        } catch (e: Throwable) {
            logE(TAG, lpparam.packageName, "error: $e")
        }
    }

    override fun onStop() {
        focusTextViewList.forEach {
            it.visibility = View.GONE
        }
    }
}