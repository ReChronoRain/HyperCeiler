package com.sevtinge.hyperceiler.view

import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.widget.TextView

@SuppressLint("ViewConstructor")
class WeatherView(context: Context?, private val showCity: Boolean) : TextView(context) {

    private val mContext: Context
    private val weatherUri = Uri.parse("content://weather/weather")
    private val mHandler: Handler
    private val mWeatherObserver: ContentObserver?
    private val mWeatherRunnable: WeatherRunnable

    init {
        mHandler =
            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(message: Message) {
                    val str = message.obj as String
                    this@WeatherView.text = if (TextUtils.isEmpty(str)) " " else str
                }
            }
        mWeatherObserver = WeatherContentObserver(mHandler)
        mContext = context!!
        mWeatherRunnable = WeatherRunnable()
        context.contentResolver.registerContentObserver(weatherUri, true, mWeatherObserver)
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
                val query = mContext.contentResolver.query(weatherUri, null, null, null, null)
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

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (mWeatherObserver != null) {
            mContext.contentResolver.unregisterContentObserver(mWeatherObserver)
        }
    }

}
