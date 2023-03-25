package com.sevtinge.cemiuiler.utils

import android.graphics.Color
import moralnorm.annotation.ColorInt

object ColorUtils {
    // color转换不可靠，加一个默认值
    val defaultReturnColor = Color.argb(50, 0, 0, 0)

    fun colorToHex(color: Int): String {
        var originalColor = Color.valueOf(defaultReturnColor)
        try {
            originalColor = Color.valueOf(color)
        } catch (e: Throwable) {
            // 颜色转换失败
        }
        val alpha = (originalColor.alpha() * 255).toInt()
        val red = (originalColor.red() * 255).toInt()
        val green = (originalColor.green() * 255).toInt()
        val blue = (originalColor.blue() * 255).toInt()
        val alphaHex = if (alpha <= 15) {
            '0' + alpha.toString()
        } else {
            alpha.toString(16)
        }
        val redHex = if (red <= 15) {
            '0' + red.toString()
        } else {
            red.toString(16)
        }
        val greenHex = if (green <= 15) {
            '0' + green.toString()
        } else {
            green.toString(16)
        }
        val blueHex = if (blue <= 15) {
            '0' + blue.toString()
        } else {
            blue.toString(16)
        }
        return "#$alphaHex$redHex$greenHex$blueHex".uppercase()
    }

    fun hexToColor(hexString: String): Int {
        try {
            return Color.parseColor(hexString)
        } catch (e: Throwable) {
            return defaultReturnColor
        }
    }

    fun isDarkColor(color: Int): Boolean {
        val darkness =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness > 0.5
    }

    fun addAlphaForColor(color: Int, alpha: Int): Int {
        return Color.valueOf(Color.red(color) / 255f,Color.green(color)/ 255f,Color.blue(color)/ 255f,alpha/ 255f).toArgb()
    }

    /**
     * @color:  参数
     * 类型：int
     * 例如：-1272178
     *
     * @return 字符串
     */
    fun colorToRGBA(color: Int): String? {
        val alpha = color ushr 24
        val r = color and 0xff0000 shr 16
        val g = color and 0xff00 shr 8
        val b = color and 0xff
        return "$alpha, $r, $g, $b"
    }

    /**
     * @red     红色数值
     * @green   绿色数值
     * @blue    蓝色色数值
     *
     * @return 字符串
     */
    fun rgbToHex(red: Int, green: Int, blue: Int): String? {
        val hr = Integer.toHexString(red)
        val hg = Integer.toHexString(green)
        val hb = Integer.toHexString(blue)
        return "#$hr$hg$hb"
    }

    /**
     * 将 颜色值 转化为 #AARRGGBB
     *
     * @param color -1272178
     * @return #AARRGGBB
     */
    fun colorToHexARGB(@ColorInt color: Int): String? {
        // 转化为16进制字符串
        var A = Integer.toHexString(Color.alpha(color))
        var R = Integer.toHexString(Color.red(color))
        var G = Integer.toHexString(Color.green(color))
        var B = Integer.toHexString(Color.blue(color))

        // 判断获取到的R,G,B值的长度 如果长度等于1 给R,G,B值的前边添0
        A = if (A.length == 1) "0$A" else A
        R = if (R.length == 1) "0$R" else R
        G = if (G.length == 1) "0$G" else G
        B = if (B.length == 1) "0$B" else B
        //
        val sb = StringBuffer()
        sb.append("#")
        sb.append(A)
        sb.append(R)
        sb.append(G)
        sb.append(B)
        return sb.toString()
    }
}