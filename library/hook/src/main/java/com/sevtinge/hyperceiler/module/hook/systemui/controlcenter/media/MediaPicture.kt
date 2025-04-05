package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.media

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
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.module.hook.systemui.base.controlcenter.PublicClass.miuiMediaControlPanel
import de.robv.android.xposed.XC_MethodHook
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.drawable.toDrawable

object MediaPicture : BaseHook() {
    private val albumPictureCorners by lazy {
        mPrefsMap.getBoolean("system_ui_control_center_media_control_album_picture_rounded_corners")
    }
    private val albumPictureIdentifier by lazy {
        mPrefsMap.getBoolean("system_ui_control_center_media_control_remove_album_audio_source_identifie")
    }

    override fun init() {
        miuiMediaControlPanel?.methodFinder()?.filterByName("bindPlayer")?.first()
            ?.createAfterHook {
                val context =
                    it.thisObject.objectHelper()
                        .getObjectOrNullUntilSuperclassAs<Context>("mContext")
                        ?: return@createAfterHook
                val mMediaViewHolder =
                    it.thisObject.objectHelper().getObjectOrNullUntilSuperclass("mMediaViewHolder")
                        ?: return@createAfterHook

                if (albumPictureIdentifier) {
                    val appIcon =
                        mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageView>("appIcon")
                    (appIcon?.parent as ViewGroup?)?.removeView(appIcon)
                }

                if (albumPictureCorners) {
                    optPicture(mMediaViewHolder, it, context)
                }
            }
    }

    private fun optPicture(
        mMediaViewHolder: Any,
        param: XC_MethodHook.MethodHookParam,
        context: Context
    ) {
        val albumView = mMediaViewHolder.objectHelper().getObjectOrNullAs<ImageView>("albumView")
        val artwork = param.args[0].objectHelper().getObjectOrNullAs<Icon>("artwork") ?: return
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
