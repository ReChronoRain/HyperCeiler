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
package com.sevtinge.hyperceiler.module.base

import android.annotation.SuppressLint
import android.app.AndroidAppHelper.currentApplication
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Base64
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import cn.lyric.getter.api.API
import cn.lyric.getter.api.data.ExtraData
import cn.lyric.getter.api.data.LyricData
import cn.lyric.getter.api.listener.LyricListener
import cn.lyric.getter.api.listener.LyricReceiver
import cn.lyric.getter.api.tools.Tools.registerLyricListener
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.R
import com.sevtinge.hyperceiler.module.base.tool.OtherTool
import com.sevtinge.hyperceiler.module.base.tool.OtherTool.getModuleRes
import com.sevtinge.hyperceiler.utils.api.ProjectApi
import com.sevtinge.hyperceiler.utils.callMethodAs
import org.json.JSONObject

abstract class MusicBaseHook : BaseHook() {
    val context: Application by lazy { currentApplication() }

    private val receiver = LyricReceiver(object : LyricListener() {
        override fun onUpdate(lyricData: LyricData) {
            runCatching {
                this@MusicBaseHook.onUpdate(lyricData)
            }.onFailure {
                logE(TAG, lpparam.packageName, it)
            }
        }

        override fun onStop(lyricData: LyricData) {
            runCatching {
                this@MusicBaseHook.onStop()
            }.onFailure {
                logE(TAG, lpparam.packageName, it)
            }
        }
    })

    init {
        loadClass("android.app.Application").methodFinder().filterByName("onCreate").first()
            .createAfterHook {
                runCatching {
                    registerLyricListener(context, API.API_VERSION, receiver)
                    // if (isDebug()) logD(TAG, lpparam.packageName, "registerLyricListener")
                }.onFailure {
                    logE(TAG, "registerLyricListener is no found")
                }
            }
    }

    abstract fun onUpdate(lyricData: LyricData)
    abstract fun onStop()

    @SuppressLint("NotificationPermission", "LaunchActivityFromNotification")
    fun sendNotification(text: String, extraData: ExtraData) {
        //  logE("sendNotification: " + context.packageName + ": " + text)
        createNotificationChannel()
        val modRes = getModuleRes(context)
        val isClickClock = mPrefsMap.getBoolean("system_ui_statusbar_music_click_clock")
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val base64icon = extraData.base64Icon
        val bitmap = context.packageManager.getActivityIcon(launchIntent!!).toBitmap()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        val intent = Intent("$CHANNEL_ID.actions.switchClockStatus")
        // 需要重启音乐软件生效
        val pendingIntent = if (isClickClock) {
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_MUTABLE)
        }
        builder.setContentTitle(text)
        if (base64icon != "") {
            val bitmapBase64Icon = base64ToDrawable(base64icon)
            if (bitmapBase64Icon != null) {
                builder.setSmallIcon(IconCompat.createWithBitmap(bitmapBase64Icon))
            } else {
                builder.setSmallIcon(IconCompat.createWithBitmap(bitmap))
            }
        } else {
            builder.setSmallIcon(IconCompat.createWithBitmap(bitmap))
        }
        builder.setTicker(text).setPriority(NotificationCompat.PRIORITY_LOW)
        builder.setOngoing(true) // 设置为常驻通知
        builder.setContentIntent(pendingIntent)

        val focuslyric_layout = modRes.getIdentifier("focuslyric_layout", "layout", ProjectApi.mAppModulePkg)
        val focuslyric = modRes.getIdentifier("focuslyric", "layout", ProjectApi.mAppModulePkg)
        val remoteViews = RemoteViews(context.packageName,focuslyric_layout)
        remoteViews.setTextViewText(focuslyric,text )
        val focus = Bundle()
        val pics = Bundle()
        if (base64icon != "") {
            val bitmapBase64Icon = base64ToDrawable(base64icon)
            if (bitmapBase64Icon != null) {
                pics.putParcelable("pro_a",Icon.createWithBitmap(bitmapBase64Icon))
            } else {
                pics.putParcelable("pro_a",Icon.createWithBitmap(bitmap))
            }
        } else {
            pics.putParcelable("pro_a",Icon.createWithBitmap(bitmap))
        }
        val cus = Bundle()
        cus.putString("ticker","Ticker")
        cus.putString("tickerPic","pro_a")
        cus.putBoolean("enableFloat",false) //通知是否弹出
        focus.putParcelable("miui.focus.param.custom",cus)
        focus.putParcelable("miui.focus.pics",pics)
        focus.putParcelable("miui.focus.rv",remoteViews)
        focus.putParcelable("miui.focus.rvAod",remoteViews)
        focus.putParcelable("miui.focus.rvNight",remoteViews)
        builder.addExtras(focus)
        val notification = builder.build()
        (context.getSystemService("notification") as NotificationManager).notify(
            CHANNEL_ID.hashCode(), notification
        )
    }

    private fun createNotificationChannel() {
        val modRes = OtherTool.getModuleRes(context)
        val notificationManager = context.getSystemService("notification") as NotificationManager
        val notificationChannel = NotificationChannel(
            CHANNEL_ID, modRes.getString(com.sevtinge.hyperceiler.R.string.system_ui_statusbar_music_notification), NotificationManager.IMPORTANCE_DEFAULT
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
     * @return [Bitmap] 返回图片的 Bitmap?，传入 Base64 无法转换则为 null
     */
    fun base64ToDrawable(base64: String): Bitmap? {
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
