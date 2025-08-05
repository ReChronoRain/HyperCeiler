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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter

import android.app.WallpaperColors
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.HardwareRenderer
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.hardware.HardwareBuffer
import android.media.ImageReader
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.get
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.miuiMediaControlPanel
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.miuiMediaViewControllerImpl
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.PublicClass.playerTwoCircleView
import com.sevtinge.hyperceiler.hook.module.hook.systemui.base.controlcenter.mediabackground.MediaViewColorConfig
import com.sevtinge.hyperceiler.hook.utils.findFieldOrNull
import io.github.kyuubiran.ezxhelper.core.finder.ConstructorFinder.`-Static`.constructorFinder
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClassOrNull
import kotlin.math.max
import kotlin.math.min

// https://github.com/HowieHChen/XiaomiHelper/blob/b1ab58484326372575a72f6509580cc60c272300/app/src/main/kotlin/dev/lackluster/mihelper/hook/rules/systemui/media/MediaControlBgFactory.kt
object MediaControlBgFactory : BaseHook() {

    val ColorSchemeClass by lazy {
        loadClassOrNull("com.android.systemui.monet.ColorScheme", lpparam.classLoader)
    }

    val defaultColorConfig = MediaViewColorConfig(
        Color.WHITE,
        Color.WHITE,
        Color.BLACK,
        Color.BLACK
    )

    val conColorScheme2 by lazy {
        ColorSchemeClass!!.constructorFinder().filterByParamCount(2).single()
    }
    val conColorScheme3 by lazy {
        ColorSchemeClass!!.constructorFinder().filterByParamCount(3).single()
    }
    val fldTonalPaletteAllShades by lazy {
        loadClass("com.android.systemui.monet.TonalPalette")
            .findFieldOrNull("allShades")
    }
    val fldColorSchemeNeutral1 by lazy {
        ColorSchemeClass!!.findFieldOrNull("mNeutral1") ?:
        ColorSchemeClass!!.findFieldOrNull("neutral1")
    }
    val fldColorSchemeNeutral2 by lazy {
        ColorSchemeClass!!.findFieldOrNull("mNeutral2") ?:
        ColorSchemeClass!!.findFieldOrNull("neutral2")
    }
    val fldColorSchemeAccent1 by lazy {
        ColorSchemeClass!!.findFieldOrNull("mAccent1") ?:
        ColorSchemeClass!!.findFieldOrNull("accent1")
    }
    val fldColorSchemeAccent2 by lazy {
        ColorSchemeClass!!.findFieldOrNull("mAccent2") ?:
        ColorSchemeClass!!.findFieldOrNull("accent2")
    }
    val enumStyleContent: Any? by lazy {
        loadClass("com.android.systemui.monet.Style", lpparam.classLoader).methodFinder()
            .filterByName("valueOf")
            .first().invoke(null, "CONTENT")
    }

    private val metIconGetBitmap by lazy {
        Icon::class.java.methodFinder()
            .filterByName("getBitmap")
            .first()
    }


    override fun init() {
        playerTwoCircleView
        miuiMediaControlPanel
        miuiMediaViewControllerImpl
        ColorSchemeClass
        runCatching {
            conColorScheme2
        }.onFailure {
            conColorScheme3
        }
        fldTonalPaletteAllShades
        fldColorSchemeNeutral1
        fldColorSchemeNeutral2
        fldColorSchemeAccent1
        fldColorSchemeAccent2
        enumStyleContent
        metIconGetBitmap
    }

    fun Context.getScaledBackground(icon: Icon?, width: Int, height: Int): Drawable? {
        val loadDrawable = icon?.loadDrawable(this) ?: return null
        val rect = Rect(0, 0, width, height)
        if (rect.width() > width || rect.height() > height) {
            rect.offset(
                ((width - rect.width()) / 2.0f).toInt(),
                ((height - rect.height()) / 2.0f).toInt()
            )
        }
        loadDrawable.bounds = rect
        return loadDrawable
    }

    fun Context.getWallpaperColor(icon: Icon?): WallpaperColors? {
        val iconType = icon?.type ?: return null
        if (iconType != Icon.TYPE_BITMAP && iconType != Icon.TYPE_ADAPTIVE_BITMAP) {
            val drawable = icon.loadDrawable(this) ?: return null
            return WallpaperColors.fromDrawable(drawable)
        } else {
            val bitmap = metIconGetBitmap.invoke(icon) as? Bitmap
            return if (bitmap?.isRecycled == false) {
                WallpaperColors.fromBitmap(bitmap)
            } else {
                null
            }
        }
    }

    fun Bitmap.brightness(): Float {
        var totalBrightness = 0f
        val totalPixels = this.width * this.height / 25

        for (x in 0 until this.width / 5) {
            for (y in 0 until this.height step 5) {
                val pixel = this[x, y]
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)
                val brightness =
                    0.299f * red + 0.587f * green + 0.114f * blue
                totalBrightness += brightness
            }
        }

        return totalBrightness / totalPixels
    }

    fun Bitmap.hardwareBlur(radius: Float): Bitmap {
        val imageReader = ImageReader.newInstance(
            this.width, this.height,
            PixelFormat.RGBA_8888, 1,
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        )
        val renderNode = RenderNode("BlurEffect")
        val hardwareRenderer = HardwareRenderer()

        hardwareRenderer.setSurface(imageReader.surface)
        hardwareRenderer.setContentRoot(renderNode)
        renderNode.setPosition(0, 0, imageReader.width, imageReader.height)
        val blurRenderEffect = RenderEffect.createBlurEffect(
            radius, radius,
            Shader.TileMode.MIRROR
        )
        renderNode.setRenderEffect(blurRenderEffect)

        val renderCanvas = renderNode.beginRecording()
        renderCanvas.drawBitmap(this, 0f, 0f, null)
        renderNode.endRecording()
        hardwareRenderer.createRenderRequest()
            .setWaitForPresent(true)
            .syncAndDraw()

        val image = imageReader.acquireNextImage() ?: throw RuntimeException("No Image")
        val hardwareBuffer = image.hardwareBuffer ?: throw RuntimeException("No HardwareBuffer")
        val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
            ?: throw RuntimeException("Create Bitmap Failed")

        hardwareBuffer.close()
        image.close()
        imageReader.close()
        renderNode.discardDisplayList()
        hardwareRenderer.destroy()
        return bitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    fun Drawable.toSquare(resources: Resources, fill: Boolean, backgroundColor: Int): Drawable {
        if (intrinsicWidth == intrinsicHeight || intrinsicWidth <= 0 || intrinsicHeight <= 0) {
            return this
        } else {
            val finalSize =
                if (fill) min(intrinsicWidth, intrinsicHeight)
                else max(intrinsicWidth, intrinsicHeight)
            val bitmap = createBitmap(finalSize, finalSize)
            val canvas = Canvas(bitmap)
            canvas.drawColor(backgroundColor)
            val deltaW = (intrinsicWidth - finalSize) / 2
            val deltaH = (intrinsicHeight - finalSize) / 2
            this.setBounds(-deltaW, -deltaH, finalSize + deltaW, finalSize + deltaH)
            this.draw(canvas)
            return bitmap.toDrawable(resources)
        }
    }
}
