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
package com.sevtinge.hyperceiler.hook.view

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.widget.TextView
import androidx.core.net.toUri
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api.Dependency
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.api.MiuiStub
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isMoreHyperOSVersion

@SuppressLint("ViewConstructor")
class WeatherView(context: Context?, private val showCity: Boolean) : TextView(context) {

    private val mContext: Context = context!!
    private val weatherUri = "content://weather/weather".toUri()
    private val mHandler: Handler = object : Handler(mContext.mainLooper) {
        override fun handleMessage(message: Message) {
            val str = message.obj as String
            this@WeatherView.text = if (TextUtils.isEmpty(str)) " " else str
        }
    }
    private val mWeatherObserver = WeatherContentObserver(mHandler)
    private val mWeatherRunnable: WeatherRunnable

    init {
        mWeatherRunnable = WeatherRunnable()
        context?.contentResolver?.registerContentObserver(weatherUri, true, mWeatherObserver)
        updateWeatherInfo()
    }

    private inner class WeatherContentObserver(handler: Handler?) : ContentObserver(handler) {
        override fun onChange(z: Boolean) {
            updateWeatherInfo()
        }
    }

    inner class WeatherRunnable : Runnable {
        override fun run() {
            var str = ""
            mContext.contentResolver.query(weatherUri, null, null, null, null)?.use { query ->
                if (query.moveToFirst()) {
                    val city = query.getString(query.getColumnIndexOrThrow("city_name"))
                    val description = query.getString(query.getColumnIndexOrThrow("description"))
                    val temperature = query.getString(query.getColumnIndexOrThrow("temperature"))
                    str = if (showCity) {
                        "$city $description $temperature"
                    } else {
                        "$description $temperature"
                    }
                }
            }

            mHandler.obtainMessage().apply {
                what = 100
                obj = str
                mHandler.sendMessage(this)
            }
        }
    }

    private fun updateWeatherInfo() {
        mHandler.removeCallbacks(mWeatherRunnable)
        mHandler.postDelayed(mWeatherRunnable, 200)
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mContext.contentResolver.unregisterContentObserver(mWeatherObserver)
    }

    fun startWeatherApp() {
        runCatching {
            val intent = Intent().apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                component = ComponentName(
                    "com.miui.weather2",
                    "com.miui.weather2.ActivityWeatherMain"
                )
            }

            if (isMoreHyperOSVersion(2f)) {
                MiuiStub.sysUIProvider.activityStarter
            } else {
                Dependency.activityStarter
            }.startActivity(intent)
        }
    }

}
