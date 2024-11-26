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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.v

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Rect
import android.service.notification.StatusBarNotification
import android.view.View
import android.widget.TextView
import cn.lyric.getter.api.data.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createBeforeHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.MusicBaseHook.Companion.CHANNEL_ID
import com.sevtinge.hyperceiler.utils.*
import de.robv.android.xposed.*
import kotlinx.coroutines.flow.*

@SuppressLint("StaticFieldLeak")
// author git@wuyou-123
// co-author git@lingqiqi5211
object HideFakeStatusBar : MusicBaseHook() {
    private var mStatusBarLeftContainer: View? = null
    private var mFocusedNotLine: View? = null
    private var mClockSeat: View? = null
    private var mBigTime: TextView? = null
    private var showCLock = false
    private val isBold by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_clock_big_bold")
    }
    // UI 上正在显示焦点通知(可能是其他应用的通知)
    private val isShowingFocused = MutableStateFlow(false)

    // 有歌词的通知(但是歌词可能没有显示在 UI 上,比如在音乐 App 中)
    private val isLyric = MutableStateFlow(false)

    // 正在显示焦点通知歌词(有歌词并且正在 UI 上显示)
    private var isShowingFocusedLyric: Boolean = false

    private fun updateLayout() {
        if (isShowingFocused.value && isLyric.value && !showCLock) {
            isShowingFocusedLyric = true
            // 如果在显示歌词,就隐藏时钟,占位布局和竖线
            mStatusBarLeftContainer?.visibility = View.INVISIBLE
            mClockSeat?.visibility = View.GONE
            mFocusedNotLine?.visibility = View.GONE
            // 设置大时钟颜色
            mBigTime?.setTextColor(Color.WHITE)
        } else {
            mStatusBarLeftContainer?.visibility = View.VISIBLE
            mClockSeat?.visibility = View.VISIBLE
            mFocusedNotLine?.visibility = View.VISIBLE
            isShowingFocusedLyric = false
        }

    }

    override fun onUpdate(lyricData: LyricData) {
    }

    override fun onStop() {
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun init() {
        // 点击显示时间
        loadClass("android.app.Application").methodFinder().first { name == "onCreate" }
            .createAfterHook {
                val mFilter = IntentFilter()
                mFilter.addAction("$CHANNEL_ID.actions.switchClockStatus")
                context.registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        showCLock = !showCLock
                        updateLayout()
                    }
                }, mFilter)
            }

        loadClass("com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView").methodFinder()
            .filterByName("onFinishInflate").first().createAfterHook {
                logI(lpparam.packageName, "onFinishInflate")
                // 通知栏左边部分(包含时间和通知图标)
                mStatusBarLeftContainer =
                    it.thisObject.getObjectField("mStatusBarLeftContainer") as View
                // mStatusBarLeftContainer!!.visibility = View.INVISIBLE
            }
        loadClass("com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment").methodFinder()
            .filterByName("onViewCreated").first().createAfterHook {
                // 焦点通知左边竖线
                mFocusedNotLine = it.thisObject.getObjectField("mFocusedNotLine") as View
                // 焦点通知左边占位布局
                mClockSeat = it.thisObject.getObjectField("mClockSeat") as View
            }

        loadClass("com.android.systemui.qs.MiuiNotificationHeaderView").methodFinder()
            .filterByName("onFinishInflate").first().createAfterHook {
                // 大时钟布局
                mBigTime = it.thisObject.getObjectField("mBigTime") as TextView
            }
        loadClass("com.android.systemui.qs.MiuiNotificationHeaderView").methodFinder()
            .filterByName("updateBigTimeColor").first().replaceMethod {
                if (isShowingFocusedLyric) {
                    // 显示歌词的时候取消设置大时钟颜色(假时钟动画会设置颜色,显示歌词的时候取消了假时钟动画,所以可能会下拉通知栏之后时间是黑色)
                    null
                } else {
                    it.invokeOriginalMethod()
                }
            }
        var unhook0: XC_MethodHook.Unhook? = null
        loadClass("com.android.systemui.controlcenter.shade.NotificationHeaderExpandController\$notificationCallback\$1").methodFinder()
            .filterByName("onExpansionChanged").first().createHook {
                before {
                    unhook0 = loadClass("com.miui.utils.configs.MiuiConfigs").methodFinder()
                        .filterByName("isVerticalMode").first().replaceMethod {
                            if (isShowingFocusedLyric) {
                                // 如果在显示歌词,就伪装成横屏,用来取消假时钟动画
                                false
                            } else {
                                it.invokeOriginalMethod()
                            }
                        }
                }
                after {
                    if (isShowingFocusedLyric) {
                        // 在显示歌词的时候固定通知栏顶部时间和日期的位置和缩放
                        val notificationHeaderExpandController =
                            it.thisObject.getObjectField("this\$0")
                        val combinedHeaderController =
                            notificationHeaderExpandController!!.getObjectField("headerController")!!
                                .callMethod("get")
                        val notificationBigTime =
                            combinedHeaderController!!.getObjectFieldAs<TextView>("notificationBigTime")
                        notificationBigTime.translationX = 0f
                        notificationBigTime.translationY = 0f
                        notificationBigTime.scaleX = 1f
                        notificationBigTime.scaleY = 1f
                        notificationBigTime.setTextColor(Color.WHITE)
                        val notificationDateTime =
                            combinedHeaderController.getObjectFieldAs<TextView>("notificationDateTime")
                        notificationDateTime.translationX = 0f
                        notificationDateTime.translationY = 0f
                        // 设置时钟的宽度
                        if (isBold) {
                            notificationHeaderExpandController.callMethod("updateWeight", 0.3f)
                        } else {
                            notificationHeaderExpandController.callMethod("updateWeight", 1.0f)
                        }
                        // 设置通知图标位置
                        combinedHeaderController.getObjectField("notificationShortcut")?.callMethod("setTranslationY", 0f)

                    }
                    unhook0?.unhook()
                }
            }
        loadClass("com.android.systemui.controlcenter.shade.NotificationHeaderExpandController\$notificationCallback\$1").methodFinder()
            .filterByName("onAppearanceChanged").first().createHook {
                before {
                }
                after {
                    if (isShowingFocusedLyric) {
                        // 显示歌词的时候手动调用动画,防止大时钟突然出现
                        val notificationHeaderExpandController =
                            it.thisObject.getObjectField("this\$0")
                        val combinedHeaderController =
                            notificationHeaderExpandController!!.getObjectField("headerController")!!
                                .callMethod("get")
                        loadClass("com.android.systemui.controlcenter.shade.NotificationHeaderExpandController")
                            .callStaticMethod(
                                "access\$startFolmeAnimationAlpha",
                                notificationHeaderExpandController,
                                combinedHeaderController!!.getObjectField("notificationBigTime"),
                                combinedHeaderController.getObjectField("notificationBigTimeFolme"),
                                if (!(it.args[0] as Boolean)) 0f else 1f,
                                true
                            )
                    }
                }
            }

        loadClass("com.android.systemui.statusbar.phone.FocusedNotifPromptController").methodFinder()
            .filterByName("notifyNotifBeanChanged").first().createHook {
                before {
                    // 焦点通知更新的事件,通过这个判断当前展示的焦点通知是不是歌词
                    val sbn = it.args[0]?.getObjectField("sbn") as StatusBarNotification?
                    isLyric.value = sbn?.notification?.channelId == CHANNEL_ID
                    updateLayout()
                }
            }

        loadClass("com.android.systemui.recents.OverviewProxyService").methodFinder()
            .filterByName("onFocusedNotifUpdate").first()
            .createBeforeHook { m ->
                // 代码中的动画目标位置
                val rect = m.args[2] as Rect
                /**
                 * 代码中的 20 是边距(可能在不同型号设备中会有不同的值,具体要去抓布局的资源文件,在小米 15pro 中是 20)
                 * @return 获取焦点通知正常情况下往右偏移的距离
                 */
                fun getWidth(): Int {
                    return (mClockSeat?.width?.plus(mFocusedNotLine?.width ?: 0) ?: 0) + 20
                }

                /**
                 * 动画的目标位置,只有两种情况,一种是隐藏时间之后的位置,一种是正常位置
                 * 如果目标位置左边减去时间那一块的宽度大于 0,则说明在右边
                 * @return 1: 当前动画目标位置在左边(隐藏时间之后的位置) 2:当前动画目标位置在右边(正常情况下的位置)
                 */
                fun getPos() = if (rect.left - getWidth() > 0) 2 else 1

                if (isLyric.value) {
                    // 如果减去左边的位置大于 0,就说明当前位置是时间右边,因为现在是在显示歌词,所以把目标位置往左偏移
                    if (getPos() == 2) {
                        rect.left -= getWidth()
                        rect.right -= getWidth()
                    }
                } else {
                    // 如果现在显示的不是歌词,并且隐藏了时钟,那么往右偏移到正常位置
                    // 当音乐在后台播放并且前台应用有焦点通知的时候会触发这种情况
                    if (mClockSeat?.visibility == 8) {
                        rect.left += getWidth()
                        rect.right += getWidth()
                    } else {
                        // 如果已经展示了时间,但是目标位置还是在左边,往右偏移
                        if (getPos() == 1) {
                            rect.left += getWidth()
                            rect.right += getWidth()
                        }
                    }
                }
            }

        loadClass("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment").methodFinder()
            .filterByName("updateStatusBarVisibilities").first().createAfterHook {
                // 获取是否在显示焦点通知
                // 更新一次 isShowingFocused
                isShowingFocused.value =
                    it.thisObject.getBooleanField("mIsFocusedNotifyViewShowing")
                updateLayout()
            }
    }
}