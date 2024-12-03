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
package com.sevtinge.hyperceiler.module.base

import android.annotation.*
import android.app.*
import android.app.AndroidAppHelper.*
import android.content.*
import android.graphics.drawable.*
import android.os.*
import androidx.core.app.*
import androidx.core.graphics.drawable.*
import cn.lyric.getter.api.*
import cn.lyric.getter.api.data.*
import cn.lyric.getter.api.listener.*
import cn.lyric.getter.api.tools.Tools.registerLyricListener
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.tool.*
import com.sevtinge.hyperceiler.utils.api.ProjectApi.*
import org.json.*

abstract class MusicBaseHook : BaseHook() {
    val context: Application by lazy { currentApplication() }

    private val receiver = LyricReceiver(object : LyricListener() {
        override fun onUpdate(lyricData: LyricData) {
            try {
                this@MusicBaseHook.onUpdate(lyricData)
            } catch (e: Throwable) {
                logE(TAG, lpparam.packageName, e)
            }
        }

        override fun onStop(lyricData: LyricData) {
            try {
                this@MusicBaseHook.onStop()
            } catch (e: Throwable) {
                logE(TAG, lpparam.packageName, e)
            }
        }
    })

    init {
        loadClass("android.app.Application").methodFinder().filterByName("onCreate").first()
            .createAfterHook {
                registerLyricListener(context, API.API_VERSION, receiver)
                if (isDebug()) logD(TAG, lpparam.packageName, "registerLyricListener")
            }
    }

    abstract fun onUpdate(lyricData: LyricData)
    abstract fun onStop()

    @SuppressLint("NotificationPermission", "LaunchActivityFromNotification")
    fun sendNotification(text: String) {
        //  logE("sendNotification: " + context.packageName + ": " + text)
        createNotificationChannel()
        val isClickClock = mPrefsMap.getBoolean("system_ui_statusbar_music_click_clock")
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
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
        builder.setSmallIcon(IconCompat.createWithBitmap(bitmap))
        builder.setTicker(text).setPriority(NotificationCompat.PRIORITY_LOW)
        builder.setOngoing(true) // 设置为常驻通知
        builder.setContentIntent(pendingIntent)
        val jSONObject = JSONObject()
        val jSONObject3 = JSONObject()
        val jSONObject4 = JSONObject()
        jSONObject4.put("type", 1)
        jSONObject4.put("title", text)
        jSONObject3.put("baseInfo", jSONObject4)
        jSONObject3.put("ticker", text)
        jSONObject3.put("tickerPic", "miui.focus.pic_ticker")
        jSONObject3.put("tickerPicDark", "miui.focus.pic_ticker_dark")

        jSONObject.put("param_v2", jSONObject3)
        val bundle = Bundle()
        bundle.putString("miui.focus.param", jSONObject.toString())
        val bundle3 = Bundle()
        bundle3.putParcelable(
            "miui.focus.pic_ticker", Icon.createWithBitmap(bitmap)
        )
        bundle3.putParcelable(
            "miui.focus.pic_ticker_dark", Icon.createWithBitmap(bitmap)
        )
        bundle.putBundle("miui.focus.pics", bundle3)


        builder.addExtras(bundle)
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

    companion object {
        const val CHANNEL_ID: String = "channel_id_focusNotifLyrics"
    }
}