package com.sevtinge.cemiuiler.titles

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.sevtinge.cemiuiler.utils.exec

class HighBrightnessMode : TileService() {
    private var isActive = false
    private var originalBrightness: String? = null
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                // 设备被息屏，关闭最高亮度模式
                qsTile.state = Tile.STATE_INACTIVE
                isActive = false
                qsTile.updateTile()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenStateReceiver)
    }

    override fun onClick() {
        super.onClick()
        if (isActive) {
            // 关闭最高亮度模式
            originalBrightness?.let { exec("echo $it > /sys/class/mi_display/disp-DSI-0/brightness_clone") }
            qsTile.state = Tile.STATE_INACTIVE
            isActive = false
        } else {
            // 开启最高亮度模式
            originalBrightness =
                exec("cat /sys/class/mi_display/disp-DSI-0/brightness_clone").trim()
            val maxBrightness =
                exec("cat /sys/class/mi_display/disp-DSI-0/max_brightness_clone").trim()
            exec("echo $maxBrightness > /sys/class/mi_display/disp-DSI-0/brightness_clone")
            qsTile.state = Tile.STATE_ACTIVE
            isActive = true
        }
        qsTile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        if (isActive) {
            qsTile.state = Tile.STATE_ACTIVE
        } else {
            qsTile.state = Tile.STATE_INACTIVE
        }
        qsTile.updateTile()
    }
}
