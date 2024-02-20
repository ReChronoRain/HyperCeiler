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
package com.sevtinge.hyperceiler.view

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.widget.Toast
import com.sevtinge.hyperceiler.utils.api.argTypes
import com.sevtinge.hyperceiler.utils.api.args
import com.sevtinge.hyperceiler.utils.callStaticMethod

@SuppressLint("ViewConstructor", "SetTextI18n")
class WeatherData(val context: Context?, private val showCity: Boolean) {

    private val mContext: Context
    private val mWeatherUri = Uri.parse("content://weather/weather")
    private val mHandler: Handler
    private val mWeatherObserver: ContentObserver?
    private val mWeatherRunnable: WeatherRunnable
    var weatherData: String = "\n"
    var callBacks: () -> Unit = {}

    init {
        mHandler =
            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(message: Message) {
                    val str = message.obj as String
                    weatherData = if (TextUtils.isEmpty(str)) "\n" else "$str\n"
                    callBacks()
                }
            }
        mWeatherObserver = WeatherContentObserver(mHandler)
        mContext = context!!
        mWeatherRunnable = WeatherRunnable()
        context.contentResolver.registerContentObserver(mWeatherUri, true, mWeatherObserver)
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
            try {
                val query = mContext.contentResolver.query(mWeatherUri, null, null, null, null)
                if (query != null) {
                    if (query.moveToFirst()) {
                        str = if (showCity) {
                            query.getString(query.getColumnIndexOrThrow("city_name")) + " " + query.getString(
                                query.getColumnIndexOrThrow(
                                    "description"
                                )
                            ) + " " + query.getString(query.getColumnIndexOrThrow("temperature"))
                        } else {
                            query.getString(query.getColumnIndexOrThrow("description")) + " " + query.getString(
                                query.getColumnIndexOrThrow(
                                    "temperature"
                                )
                            )
                        }
                    }
                    query.close()
                }
            } catch (_: Exception) {

            }
            val obtainMessage2 = mHandler.obtainMessage()
            obtainMessage2.what = 100
            obtainMessage2.obj = str
            mHandler.sendMessage(obtainMessage2)
        }
    }

    private fun updateWeatherInfo() {
        mHandler.removeCallbacks(mWeatherRunnable)
        mHandler.postDelayed(mWeatherRunnable, 200)
    }

    fun onDetachedFromWindow() {
        if (mWeatherObserver != null) {
            mContext.contentResolver.unregisterContentObserver(mWeatherObserver)
        }
    }

    fun startCalendarApp() {
        mContext.classLoader.loadClass("com.miui.systemui.util.CommonUtil")
            .callStaticMethod("startCalendarApp", args(context), argTypes(Context::class.java))
    }

    fun startWeatherApp() {
        try {
            val intent = Intent().apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                component = ComponentName(
                    "com.miui.weather2",
                    "com.miui.weather2.ActivityWeatherMain"
                )
            }
            mContext.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "启动失败", Toast.LENGTH_LONG).show()
        }
    }
}
