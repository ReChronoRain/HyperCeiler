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
package com.sevtinge.hyperceiler.utils.api.miuiStringToast

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import com.google.gson.Gson
import com.sevtinge.hyperceiler.utils.api.ProjectApi
import com.sevtinge.hyperceiler.utils.api.miuiStringToast.res.IconParams
import com.sevtinge.hyperceiler.utils.api.miuiStringToast.res.Left
import com.sevtinge.hyperceiler.utils.api.miuiStringToast.res.Right
import com.sevtinge.hyperceiler.utils.api.miuiStringToast.res.StringToastBean
import com.sevtinge.hyperceiler.utils.api.miuiStringToast.res.StringToastBundle
import com.sevtinge.hyperceiler.utils.api.miuiStringToast.res.TextParams
import java.lang.reflect.InvocationTargetException

object MiuiStringToast {
    fun newIconParams(
        category: String?,
        iconResName: String?,
        iconType: Int,
        iconFormat: String?
    ): IconParams {
        val params = IconParams()
        params.setCategory(category)
        params.setIconResName(iconResName)
        params.setIconType(iconType)
        params.setIconFormat(iconFormat)
        return params
    }

    @SuppressLint("WrongConstant")
    fun showStringToast(context: Context, text: String?, colorType: Int?) {
        try {
            val textParams = TextParams()
            textParams.setText(text)
            textParams.setTextColor(if (colorType == 1) colorToInt("#4CAF50") else colorToInt("#E53935"))
            val left = Left()
            left.setTextParams(textParams)
            val iconParams: IconParams =
                newIconParams(Category.DRAWABLE, "ic_hyperceiler", 1, FileType.SVG)
            val right = Right()
            right.setIconParams(iconParams)
            val stringToastBean = StringToastBean()
            stringToastBean.setLeft(left)
            stringToastBean.setRight(right)
            val gson = Gson()
            val str = gson.toJson(stringToastBean)
            val bundle: Bundle = StringToastBundle.Builder()
                .setPackageName(ProjectApi.mAppModulePkg)
                .setStrongToastCategory(StrongToastCategory.TEXT_BITMAP.value)
                .setTarget(null as PendingIntent?)
                .setDuration(3000L)
                .setLevel(0.0f)
                .setRapidRate(0.0f)
                .setCharge(null as String?)
                .setStringToastChargeFlag(0)
                .setParam(str)
                .setStatusBarStrongToast("show_custom_strong_toast")
                .onCreate()
            val service = context.getSystemService(Context.STATUS_BAR_SERVICE)
            service.javaClass.getMethod(
                "setStatus",
                Int::class.javaPrimitiveType,
                String::class.java,
                Bundle::class.java
            ).invoke(service, 1, "strong_toast_action", bundle)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        } catch (e: InvocationTargetException) {
            throw RuntimeException(e)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        }
    }

    private fun colorToInt(color: String?): Int {
        val color1 = Color.parseColor(color)
        val color2 = Color.parseColor("#FFFFFF")
        val color3 = color1 xor color2
        return color3.inv()
    }

    object Category {
        const val RAW = "raw"
        const val DRAWABLE = "drawable"
        const val FILE = "file"
        const val MIPMAP = "mipmap"
    }

    object FileType {
        const val MP4 = "mp4"
        const val PNG = "png"
        const val SVG = "svg"
    }

    enum class StrongToastCategory(var value: String) {
        VIDEO_TEXT("video_text"),
        VIDEO_BITMAP_INTENT("video_bitmap_intent"),
        TEXT_BITMAP("text_bitmap"),
        TEXT_BITMAP_INTENT("text_bitmap_intent"),
        VIDEO_TEXT_TEXT_VIDEO("video_text_text_video")

    }
}
