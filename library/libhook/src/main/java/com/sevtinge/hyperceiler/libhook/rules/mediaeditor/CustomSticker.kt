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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.mediaeditor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.io.File

/**
 * 自定义贴纸 —— 贴纸面板内「实时选图」。
 *
 * 在贴纸面板右下角注入一个「＋」按钮，点击后从相册任选一张图片，直接作为贴纸
 * 落到当前画布上。不走模块设置预置，不重开编辑器。
 *
 * 反汇编确认（com.miui.mediaeditor 2.10.40.4.8）的落画布链路：
 *
 * ```
 * 贴纸 item 点击（z10/f.c）
 *   → d20/p.a(StickerData)                 // 接口，实参是 StickerItem
 *   → d10/r0$a.a(StickerData)              // 接口实现（竖屏；横屏 d10/v0$a）
 *       check-cast StickerItem
 *   → d10/g0.M0(StickerItem)               // 落画布：setStickerItem + viewModel.d0
 * ```
 *
 * 面板容器 `d20/c0`（extends FrameLayout）在竖屏 `d10/r0.onViewCreated` 和横屏
 * `d10/v0.onViewCreated` 中各 `new` 一次，因此 hook 其构造器可横竖屏通吃。面板
 * 字段 `a`（d20/p 回调）可直接 invoke 落画布，无需拿到 Fragment。`StickerItem`
 * 未混淆，其 `content` 字段是唯一解码输入，接受任意绝对路径。
 */
object CustomSticker : BaseHook() {

    private const val TAG_NAME = "CustomSticker"
    private const val PKG_NAME = "com.miui.mediaeditor"

    private const val REQ_PICK = 0x7374 // 'st'

    private const val CUSTOM_ITEM_ID_BASE = -900000L

    private const val TARGET_MAX_EDGE = 800

    private const val STICKER_ITEM_CLS =
        "com.miui.mediaeditor.photo.mark.sticker.model.StickerItem"
    private const val STICKER_DATA_CLS =
        "com.miui.mediaeditor.photo.mark.sticker.model.StickerData"

    private const val TAG_BUTTON = "hyperceiler_custom_sticker_add"

    private lateinit var panelClass: Class<*>

    private val hookedActivityClasses = java.util.Collections.synchronizedSet(
        java.util.HashSet<String>()
    )

    private var pendingPanel: Any? = null

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        return runCatching {
            panelClass = requiredMember("StickerPanelClass") {
                it.findClass {
                    matcher {
                        addUsingString("onStickerClickListener", StringMatchType.Equals)
                        superClass = "android.widget.FrameLayout"
                    }
                }.single()
            }
        }.onFailure {
            XposedLog.w(TAG_NAME, PKG_NAME, "DexKit locate failed, custom sticker disabled", it)
        }.isSuccess
    }

    override fun init() {
        if (!::panelClass.isInitialized) return

        runCatching {
            hookAllConstructors(panelClass, object : IMethodHook {
                override fun after(param: HookParam) {
                    val panel = param.thisObject
                    runCatching {
                        injectAddButton(panel)
                    }.onFailure { t ->
                        XposedLog.w(TAG_NAME, PKG_NAME, "inject add button failed", t)
                    }
                }
            })
        }.onFailure { t ->
            XposedLog.w(TAG_NAME, PKG_NAME, "hook panel constructor failed", t)
        }
    }

    private fun injectAddButton(panel: Any) {
        val rootView = runCatching {
            panel.javaClass.getMethod("getStickerRootView").invoke(panel) as? ViewGroup
        }.getOrNull() ?: (getObjectField(panel, "g") as? ViewGroup) ?: return
        val activity = resolveActivity(panel) ?: return

        hookActivityResult(activity)

        if (rootView.findViewWithTag<View>(TAG_BUTTON) != null) return

        val btn = ImageView(activity)
        btn.tag = TAG_BUTTON
        btn.setColorFilter(Color.argb(0xCC, 0xFF, 0xFF, 0xFF))
        btn.setImageResource(android.R.drawable.ic_input_add)
        btn.setBackgroundColor(Color.argb(0x66, 0x00, 0x00, 0x00))
        val density = activity.resources.displayMetrics.density
        val size = Math.round(44 * density)
        val lp = FrameLayout.LayoutParams(size, size, Gravity.END or Gravity.BOTTOM)
        lp.rightMargin = Math.round(16 * density)
        lp.bottomMargin = Math.round(16 * density)
        btn.scaleType = ImageView.ScaleType.CENTER
        btn.setOnClickListener {
            pendingPanel = panel
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            runCatching {
                activity.startActivityForResult(intent, REQ_PICK)
            }.onFailure { t ->
                XposedLog.w(TAG_NAME, PKG_NAME, "start picker failed", t)
            }
        }
        rootView.addView(btn, lp)
        XposedLog.d(TAG_NAME, PKG_NAME, "custom sticker add button injected")
    }

    private fun hookActivityResult(activity: Activity) {
        val cls = activity.javaClass
        if (!hookedActivityClasses.add(cls.name)) return

        val method = runCatching {
            cls.getMethod(
                "onActivityResult",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Intent::class.java
            )
        }.getOrNull() ?: return

        runCatching {
            hookMethod(method, object : IMethodHook {
                override fun before(param: HookParam) {
                    val args = param.args
                    if (args.size < 3) return
                    val req = (args[0] as? Int) ?: return
                    val res = (args[1] as? Int) ?: return
                    val data = args[2] as? Intent
                    handlePickResult(activity, req, res, data)
                }
            })
        }.onFailure { t ->
            XposedLog.w(TAG_NAME, PKG_NAME, "hook onActivityResult failed", t)
        }
    }

    private fun resolveActivity(panel: Any): Activity? {
        val ctx = runCatching { (panel as View).context }.getOrNull() ?: return null
        var c: Context? = ctx
        while (c != null) {
            if (c is Activity) return c
            c = runCatching { (c as android.content.ContextWrapper).baseContext }.getOrNull()
        }
        return null
    }

    private fun handlePickResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQ_PICK || resultCode != Activity.RESULT_OK || data == null) return
        val panel = pendingPanel ?: return
        pendingPanel = null

        val uri = data.data ?: return
        runCatching {
            val file = saveImage(activity, uri) ?: return
            val item = buildStickerItem(file)
            placeSticker(panel, item)
            XposedLog.d(TAG_NAME, PKG_NAME, "custom sticker placed: ${file.path}")
        }.onFailure { t ->
            XposedLog.w(TAG_NAME, PKG_NAME, "place custom sticker failed", t)
        }
    }

    private fun saveImage(activity: Activity, uri: Uri): File? {
        val dir = File(activity.filesDir, "photo_editor/stickers/custom_live")
        dir.mkdirs()
        val out = File(dir, "live_${System.currentTimeMillis()}.webp")

        val raw = activity.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

        val probe = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, probe)
        if (probe.outWidth <= 0 || probe.outHeight <= 0) return null
        var sample = 1
        while (probe.outWidth / (sample * 2) >= TARGET_MAX_EDGE &&
            probe.outHeight / (sample * 2) >= TARGET_MAX_EDGE
        ) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size, opts) ?: return null
        val ok = out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, it) }
        if (!bitmap.isRecycled) bitmap.recycle()
        return if (ok) out else null
    }

    private fun buildStickerItem(file: File): Any {
        val clazz = findClass(STICKER_ITEM_CLS)
        val ctor = clazz.getConstructor(
            Short::class.javaPrimitiveType,
            String::class.java,
            Long::class.javaPrimitiveType,
            String::class.java,
            String::class.java,
            String::class.java
        )
        return ctor.newInstance(
            0.toShort(),
            "custom_live",
            CUSTOM_ITEM_ID_BASE - (System.currentTimeMillis() % 100000),
            file.absolutePath,
            file.absolutePath,
            "custom"
        )
    }

    private fun placeSticker(panel: Any, item: Any) {
        val callback = getObjectField(panel, "a") ?: run {
            XposedLog.w(TAG_NAME, PKG_NAME, "panel callback field 'a' is null")
            return
        }
        val dataClass = findClass(STICKER_DATA_CLS)
        val method = runCatching {
            callback.javaClass.getMethod("a", dataClass)
        }.getOrElse {
            XposedLog.w(TAG_NAME, PKG_NAME, "locate d20/p.a failed", it)
            return
        }
        method.invoke(callback, item)
    }
}
