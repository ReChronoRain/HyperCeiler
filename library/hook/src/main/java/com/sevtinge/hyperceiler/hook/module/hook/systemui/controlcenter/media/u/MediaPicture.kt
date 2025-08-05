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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter.media.u

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.miuiMediaControlPanel
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNull
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldOrNullAs
import de.robv.android.xposed.XC_MethodHook
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

object MediaPicture : BaseHook() {
    private val albumPictureCorners by lazy {
        mPrefsMap.getBoolean("system_ui_control_center_media_control_album_picture_rounded_corners")
    }
    private val mode by lazy {
        mPrefsMap.getStringAsInt("system_ui_control_center_media_control_media_album_mode", 0)
    }

    override fun init() {

        miuiMediaControlPanel?.methodFinder()?.filterByName("bindPlayer")?.first()
            ?.createAfterHook {
                val context =
                    it.thisObject.getObjectFieldOrNullAs<Context>("mContext")
                        ?: return@createAfterHook
                val mMediaViewHolder =
                    it.thisObject.getObjectFieldOrNull("mMediaViewHolder")
                        ?: return@createAfterHook

                if (mode == 1) {
                    val appIcon =
                        mMediaViewHolder.getObjectFieldOrNullAs<ImageView>("appIcon")
                    (appIcon?.parent as ViewGroup?)?.removeView(appIcon)
                }

                if (albumPictureCorners && mode != 2) {
                    optPicture(mMediaViewHolder, it, context)
                }
        }
    }

    fun optPicture(
        mMediaViewHolder: Any,
        param: XC_MethodHook.MethodHookParam,
        context: Context
    ) {
        val albumView = mMediaViewHolder.getObjectFieldOrNullAs<ImageView>("albumView")
        val artwork = param.args[0].getObjectFieldOrNullAs<Icon>("artwork") ?: return
        val artworkLayer = artwork.loadDrawable(context) ?: return

        val artworkBitmap = createBitmap(artworkLayer.intrinsicWidth, artworkLayer.intrinsicHeight)
        val canvas = Canvas(artworkBitmap)
        artworkLayer.setBounds(0, 0, artworkLayer.intrinsicWidth, artworkLayer.intrinsicHeight)
        artworkLayer.draw(canvas)
        val minDimen = artworkBitmap.width.coerceAtMost(artworkBitmap.height)
        val left = (artworkBitmap.width - minDimen) / 2
        val top = (artworkBitmap.height - minDimen) / 2
        val rect = Rect(left, top, left + minDimen, top + minDimen)
        val croppedBitmap = createBitmap(minDimen, minDimen)
        val canvasCropped = Canvas(croppedBitmap)
        canvasCropped.drawBitmap(artworkBitmap, rect, Rect(0, 0, minDimen, minDimen), null)
        // 300px & 45f rounded corners are necessary，otherwise the rounded corners are not drawn correctly.
        val resizedBitmap = croppedBitmap.scale(300, 300)
        val bitmapNew =
            createBitmap(resizedBitmap.width, resizedBitmap.height, Bitmap.Config.ARGB_8888)
        val canvasNew = Canvas(bitmapNew)
        val paint = Paint()
        val rectF = RectF(0f, 0f, resizedBitmap.width.toFloat(), resizedBitmap.height.toFloat())
        paint.isAntiAlias = true
        canvasNew.drawARGB(0, 0, 0, 0)
        paint.color = Color.BLACK
        canvasNew.drawRoundRect(rectF, 45f, 45f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvasNew.drawBitmap(resizedBitmap, 0f, 0f, paint)
        albumView?.setImageDrawable(bitmapNew.toDrawable(context.resources))
    }
}
