package com.sevtinge.cemiuiler.utils

import android.graphics.Color
import de.robv.android.xposed.XposedBridge

object ColorUtils {
    // color转换不可靠，加一个默认值
    private val defaultReturnColor = Color.argb(50, 0, 0, 0)

    fun colorToHex(color: Int): String {
        var originalColor = Color.valueOf(defaultReturnColor)
        try {
            originalColor = Color.valueOf(color)
        } catch (e: Throwable) {
            // 颜色转换失败
            XposedBridge.log("Cemiuiler: ColorUtils colorToHex Hook failed by: $e")
        }
        val alpha = (originalColor.alpha() * 255).toInt()
        val red = (originalColor.red() * 255).toInt()
        val green = (originalColor.green() * 255).toInt()
        val blue = (originalColor.blue() * 255).toInt()
        val alphaHex = if (alpha <= 15) {
            "0$alpha"
        } else {
            alpha.toString(16)
        }
        val redHex = if (red <= 15) {
            "0$red"
        } else {
            red.toString(16)
        }
        val greenHex = if (green <= 15) {
            "0$green"
        } else {
            green.toString(16)
        }
        val blueHex = if (blue <= 15) {
            "0$blue"
        } else {
            blue.toString(16)
        }
        return "#$alphaHex$redHex$greenHex$blueHex".uppercase()
    }

    fun hexToColor(hexString: String): Int {
        return try {
            Color.parseColor(hexString)
        } catch (e: Throwable) {
            defaultReturnColor
        }
    }

    fun isDarkColor(color: Int): Boolean {
        val darkness =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness > 0.5
    }

    fun addAlphaForColor(color: Int, alpha: Int): Int {
        return Color.valueOf(Color.red(color) / 255f, Color.green(color) / 255f, Color.blue(color) / 255f, alpha / 255f)
            .toArgb()
    }

}
