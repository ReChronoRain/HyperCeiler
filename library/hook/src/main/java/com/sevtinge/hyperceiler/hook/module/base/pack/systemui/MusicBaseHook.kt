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
package com.sevtinge.hyperceiler.hook.module.base.pack.systemui

import android.annotation.SuppressLint
import android.app.AndroidAppHelper
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Icon
import android.media.session.PlaybackState
import android.util.Base64
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.hchen.superlyricapi.ISuperLyric
import com.hchen.superlyricapi.SuperLyricData
import com.hchen.superlyricapi.SuperLyricTool
import com.hyperfocus.api.FocusApi
import com.sevtinge.hyperceiler.hook.R
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool
import com.sevtinge.hyperceiler.hook.utils.api.ProjectApi
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

abstract class MusicBaseHook : BaseHook() {
    val context: Application by lazy { AndroidAppHelper.currentApplication() }
    private val nSize by lazy {
        mPrefsMap.getInt("system_ui_statusbar_music_size_n", 15).toFloat()
    }
    private val isAodShow by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_music_hide_aod")
    }
    private val isAodMode by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_music_show_aod_mode")
    }

    private val receiver = object : ISuperLyric.Stub() {
        override fun onSuperLyric(data: SuperLyricData) {
            runCatching {
                this@MusicBaseHook.onSuperLyric(data)
            }.onFailure {
                logE(TAG, lpparam.packageName, it)
            }
        }

        override fun onStop(data: SuperLyricData) {
            runCatching {
                if (data.playbackState?.state == PlaybackState.STATE_BUFFERING) return
                this@MusicBaseHook.onStop()
            }.onFailure {
                logE(TAG, lpparam.packageName, it)
            }
        }
    }

    init {
        loadClass("android.app.Application").methodFinder().filterByName("onCreate").first()
            .createAfterHook {
                runCatching {
                    SuperLyricTool.registerSuperLyric(context, receiver)
                    // if (isDebug()) logD(TAG, lpparam.packageName, "registerLyricListener")
                }.onFailure {
                    logE(TAG, "registerLyricListener is no found")
                }
            }
    }

    abstract fun onSuperLyric(data: SuperLyricData)
    abstract fun onStop()

    @SuppressLint("NotificationPermission", "LaunchActivityFromNotification")
    fun sendNotification(text: String, extraData: SuperLyricData) {
        createNotificationChannel()
        val modRes = OtherTool.getModuleRes(context)
        val isClickClock = mPrefsMap.getBoolean("system_ui_statusbar_music_click_clock")
        val launchIntent = context.packageManager.getLaunchIntentForPackage(extraData.packageName)
        // 图标处理
        val basebitmap = base64ToDrawable(extraData.base64Icon)
        val bitmap = basebitmap ?: context.packageManager.getActivityIcon(launchIntent!!).toBitmap()
        val icon: Icon = Icon.createWithBitmap(bitmap).apply { if (basebitmap != null) setTint(Color.WHITE) }
        val dartIcon : Icon = Icon.createWithBitmap(bitmap).apply { if (basebitmap != null) setTint(Color.BLACK) }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        val intent = Intent("$CHANNEL_ID.actions.switchClockStatus")
        // 翻译
        val tf = extraData.translation
        // 对唱对齐方式，有性能问题放弃
        // val dule = extraData.extra?.getBoolean(KEY_DUTE,false)?:false

        // 需要重启音乐软件生效
        val pendingIntent = if (isClickClock) {
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_MUTABLE)
        }
        builder.setContentTitle(text)
            .setSmallIcon(IconCompat.createWithBitmap(bitmap))
            .setTicker(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        fun buildRemoteViews(textColor: Int): RemoteViews {
            val layoutId = modRes.getIdentifier("focuslyric_layout", "layout", ProjectApi.mAppModulePkg)
            val textId = modRes.getIdentifier("focuslyric", "id", ProjectApi.mAppModulePkg)
            val tf_text_id = modRes.getIdentifier("focustflyric", "id", ProjectApi.mAppModulePkg)
            return RemoteViews(ProjectApi.mAppModulePkg, layoutId).apply {
                if (tf != null){
                    setViewVisibility(tf_text_id, View.VISIBLE)
                    setTextViewText(tf_text_id, tf)
                    setTextColor(tf_text_id, textColor)
                    setTextViewTextSize(tf_text_id, TypedValue.COMPLEX_UNIT_SP, nSize)
                } else {
                    setViewVisibility(tf_text_id, View.GONE)
                }
                setTextViewText(textId, text)
                setTextColor(textId, textColor)
                setTextViewTextSize(textId, TypedValue.COMPLEX_UNIT_SP, nSize)
            }
        }

        fun buildAodRemoteViews(textColor: Int): RemoteViews {
            val layoutId = modRes.getIdentifier("focusaodlyric_layout", "layout", ProjectApi.mAppModulePkg)
            val textId = modRes.getIdentifier("focuslyric", "id", ProjectApi.mAppModulePkg)
            val iconId = modRes.getIdentifier("focusicon", "id", ProjectApi.mAppModulePkg)
            val tf_text_id = modRes.getIdentifier("focustflyric", "id", ProjectApi.mAppModulePkg)
            return RemoteViews(ProjectApi.mAppModulePkg, layoutId).apply {
                if (tf != null){
                    setViewVisibility(tf_text_id, View.VISIBLE)
                    setTextViewText(tf_text_id, tf)
                    setTextColor(tf_text_id, textColor)
                    setTextViewTextSize(tf_text_id, TypedValue.COMPLEX_UNIT_SP, nSize)
                } else {
                    setViewVisibility(tf_text_id, View.GONE)
                }
                setTextViewText(textId, text)
                setTextColor(textId, textColor)
                setTextViewTextSize(textId, TypedValue.COMPLEX_UNIT_SP, nSize)
                setTextViewTextSize(textId, TypedValue.COMPLEX_UNIT_SP, nSize)
                setImageViewBitmap(iconId, icon.loadDrawable(context)?.toBitmap())
            }
        }

        runCatching {
            val remoteViewsNight = buildRemoteViews(Color.WHITE)
            val remoteViewsDay = buildRemoteViews(Color.BLACK)
            val remoteViewsAod = buildAodRemoteViews(Color.WHITE)
            val api = if (!isAodShow) {
                if (isAodMode) {
                    FocusApi.senddiyFocus(
                        ticker = text,
                        updatable = true,
                        rvAod = remoteViewsAod,
                        enableFloat = false,
                        rv = remoteViewsDay,
                        rvNight = remoteViewsNight,
                        timeout = 999999,
                        picticker = icon,
                        pictickerdark = dartIcon
                    )
                } else {
                    FocusApi.senddiyFocus(
                        ticker = text,
                        updatable = true,
                        aodPic = icon,
                        aodTitle = text,
                        enableFloat = false,
                        rv = remoteViewsDay,
                        rvNight = remoteViewsNight,
                        timeout = 999999,
                        picticker = icon,
                        pictickerdark = dartIcon
                    )
                }
            } else {
                FocusApi.senddiyFocus(
                    ticker = text,
                    updatable = true,
                    enableFloat = false,
                    rv = remoteViewsDay,
                    rvNight = remoteViewsNight,
                    timeout = 999999,
                    picticker = icon,
                    pictickerdark = dartIcon
                )
            }
            builder.addExtras(api)
            builder.extras.putString("app_package", extraData.packageName)
            val notification = builder.build()
            (context.getSystemService("notification") as NotificationManager)
                .notify(CHANNEL_ID.hashCode(), notification)
        }.onFailure {
            logE(TAG, lpparam.packageName, "send focus failed, ${it.message}")
            val baseinfo = FocusApi.baseinfo(
                basetype = 1,
                title = text
            )
            val api = if (!isAodShow) {
                FocusApi.sendFocus(
                    ticker = text,
                    aodTitle = text,
                    aodPic = icon,
                    baseInfo = baseinfo,
                    updatable = true,
                    enableFloat = false,
                    timeout = 999999,
                    picticker = icon,
                    pictickerdark = dartIcon
                )
            } else {
                FocusApi.sendFocus(
                    ticker = text,
                    baseInfo = baseinfo,
                    updatable = true,
                    enableFloat = false,
                    timeout = 999999,
                    picticker = icon,
                    pictickerdark = dartIcon
                )
            }
            builder.addExtras(api)
            builder.extras.putString("app_package", extraData.packageName)
            val notification = builder.build()
            (context.getSystemService("notification") as NotificationManager)
                .notify(CHANNEL_ID.hashCode(), notification)
        }
    }

    private fun createNotificationChannel() {
        val modRes = OtherTool.getModuleRes(context)
        val notificationManager = context.getSystemService("notification") as NotificationManager
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            modRes.getString(R.string.system_ui_statusbar_music_notification),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationChannel.setSound(null, null)
        notificationManager.createNotificationChannel(notificationChannel)
    }


    @SuppressLint("NotificationPermission")
    fun cancelNotification() {
        (context.getSystemService("notification") as NotificationManager).cancel(CHANNEL_ID.hashCode())
    }

    /**
     *
     * @param [base64] 图片的 Base64
     * @return [android.graphics.Bitmap] 返回图片的 Bitmap?，传入 Base64 无法转换则为 null
     */
    private fun base64ToDrawable(base64: String): Bitmap? {
        return try {
            val bitmapArray: ByteArray = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.size)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val CHANNEL_ID: String = "channel_id_focusNotifLyrics"
    }
}
