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
 * 目标：让用户在贴纸面板里当场点一个「＋」按钮，从相册挑一张图，图片立刻变成
 * 可贴的贴纸落到画布上。不走模块设置预置，不重开编辑器。
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
 * 关键事实（反汇编铁证）：
 * 1. 面板容器 `d20/c0`（extends FrameLayout），构造器
 *    `(Context, ArrayList, boolean, boolean, d20/p)`；字段 `a`=d20/p 回调、
 *    `g`=root ViewGroup。竖屏 `d10/r0.onViewCreated` 和横屏 `d10/v0.onViewCreated`
 *    都会 `new Ld20/c0` 并 `iput-object this.J`，但只有横屏调 getStickerRootView()，
 *    因此 hook 构造器（而非 getter）才能横竖屏通吃。
 * 2. `d20/p.a(StickerData)` 是贴纸点击公共回调（PUBLIC ABSTRACT 接口方法），
 *    面板字段 `a` 即其实现实例（d10/r0$a / d10/v0$a）。直接 invoke 它即可落画布，
 *    无需拿到 Fragment。
 * 3. `StickerItem` 未混淆，构造器
 *    `(short priority, String name, long id, String icon, String content, String cateName)`，
 *    字段 `d`=content 是唯一解码输入，最终 BitmapFactory.decodeFile(content)，
 *    接受任意绝对路径。
 *
 * 方案：hook 面板构造器拿面板实例（横竖屏各触发一次）→ 注入「＋」按钮 →
 * 点按钮用面板 Activity 拉起系统相册选图 → 落盘 → new StickerItem(绝对路径) →
 * 调 panel.a(StickerItem) 落画布。
 */
object CustomSticker : BaseHook() {

    private const val TAG_NAME = "CustomSticker"
    private const val PKG_NAME = "com.miui.mediaeditor"

    private const val REQ_PICK = 0x7374 // 'st'

    /** StickerItem 的 id 取一个大的负数，避免与官方 item id 冲突。 */
    private const val CUSTOM_ITEM_ID_BASE = -900000L

    /** 图片最长边缩放目标（与官方贴纸素材一致 800px）。 */
    private const val TARGET_MAX_EDGE = 800

    private const val STICKER_ITEM_CLS =
        "com.miui.mediaeditor.photo.mark.sticker.model.StickerItem"
    private const val STICKER_DATA_CLS =
        "com.miui.mediaeditor.photo.mark.sticker.model.StickerData"

    /** d20/c0 面板类。 */
    private lateinit var panelClass: Class<*>

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        return runCatching {
            // d20/c0 面板类：构造器首个 const-string 是 "onStickerClickListener"，
            // 且继承 FrameLayout。用 superClass 区分开同样含该字符串的 z10/g（RecyclerView$e）。
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

        // hook d20/c0 构造器 after：thisObject 即面板实例，字段 a/g 已就绪
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

    // ---------------- 注入「＋」按钮 ----------------

    private fun injectAddButton(panel: Any) {
        val rootView = panelRootView(panel) ?: return
        val activity = resolveActivity(panel) ?: return

        // 确保这个 Activity 的 onActivityResult 被 hook（幂等）
        hookActivityResult(activity)

        // 避免重复注入
        if (rootView.findViewWithTag<View>(TAG_BUTTON) != null) return

        val btn = ImageView(activity)
        btn.tag = TAG_BUTTON
        btn.setColorFilter(Color.argb(0xCC, 0xFF, 0xFF, 0xFF))
        btn.setImageResource(android.R.drawable.ic_input_add)
        btn.setBackgroundColor(Color.argb(0x66, 0x00, 0x00, 0x00))
        val size = dp(activity, 44)
        val lp = FrameLayout.LayoutParams(size, size, Gravity.END or Gravity.BOTTOM)
        lp.rightMargin = dp(activity, 16)
        lp.bottomMargin = dp(activity, 16)
        btn.scaleType = ImageView.ScaleType.CENTER
        btn.setOnClickListener { launchPicker(activity, panel) }
        rootView.addView(btn, lp)
        XposedLog.d(TAG_NAME, PKG_NAME, "custom sticker add button injected")
    }

    /** 已 hook 过 onActivityResult 的 Activity 类名，避免重复 hook。 */
    private val hookedActivityClasses = java.util.Collections.synchronizedSet(
        java.util.HashSet<String>()
    )

    /**
     * hook 指定 Activity 实例所属类的 onActivityResult(int,int,Intent)。
     * 在 before 里读参数：args[0]=requestCode, args[1]=resultCode, args[2]=data。
     */
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

    /** 面板根容器：优先公开 getter getStickerRootView()，退回字段 g。 */
    private fun panelRootView(panel: Any): ViewGroup? {
        val viaGetter = runCatching {
            val m = panel.javaClass.getMethod("getStickerRootView")
            m.invoke(panel) as? ViewGroup
        }.getOrNull()
        if (viaGetter != null) return viaGetter
        return getObjectField(panel, "g") as? ViewGroup
    }

    /** 从 panel 的 context 反推出宿主 Activity。 */
    private fun resolveActivity(panel: Any): Activity? {
        val ctx = runCatching { (panel as View).context }.getOrNull() ?: return null
        var c: Context? = ctx
        while (c != null) {
            if (c is Activity) return c
            c = runCatching { (c as android.content.ContextWrapper).baseContext }.getOrNull()
        }
        return null
    }

    // ---------------- 选图 ----------------

    /** 选图进行中记住面板实例，结果回来后用它落画布。 */
    private var pendingPanel: Any? = null

    private fun launchPicker(activity: Activity, panel: Any) {
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

    // ---------------- 落画布 ----------------

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
        val bitmap = decodeScaled(raw, TARGET_MAX_EDGE) ?: return null
        val ok = out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, it) }
        if (!bitmap.isRecycled) bitmap.recycle()
        return if (ok) out else null
    }

    private fun decodeScaled(raw: ByteArray, target: Int): Bitmap? {
        val probe = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, probe)
        if (probe.outWidth <= 0 || probe.outHeight <= 0) return null
        var sample = 1
        while (probe.outWidth / (sample * 2) >= target && probe.outHeight / (sample * 2) >= target) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(raw, 0, raw.size, opts)
    }

    /**
     * 构造 StickerItem。content 传绝对路径。
     * 构造器签名：(short priority, String name, long id, String icon, String content, String cateName)
     */
    private fun buildStickerItem(file: File): Any {
        val clazz = findClass(STICKER_ITEM_CLS)
        val ctor = clazz.getConstructor(
            Short::class.javaPrimitiveType, // short priority
            String::class.java,             // name
            Long::class.javaPrimitiveType,  // long id
            String::class.java,             // icon
            String::class.java,             // content
            String::class.java              // cateName
        )
        return ctor.newInstance(
            0.toShort(),
            "custom_live",
            CUSTOM_ITEM_ID_BASE - (System.currentTimeMillis() % 100000),
            file.absolutePath, // icon 也用绝对路径（面板格子预览）
            file.absolutePath, // content —— 唯一解码输入
            "custom"           // cateName
        )
    }

    /** 通过面板字段 a（d20/p 回调）把贴纸落到画布。 */
    private fun placeSticker(panel: Any, item: Any) {
        val callback = getObjectField(panel, "a") ?: run {
            XposedLog.w(TAG_NAME, PKG_NAME, "panel callback field 'a' is null")
            return
        }
        // d20/p.a(StickerData)：接口方法名 a（混淆后），参数是 StickerData
        val dataClass = findClass(STICKER_DATA_CLS)
        val method = runCatching {
            callback.javaClass.getMethod("a", dataClass)
        }.getOrElse {
            XposedLog.w(TAG_NAME, PKG_NAME, "locate d20/p.a failed", it)
            return
        }
        method.invoke(callback, item)
    }

    // ---------------- 工具 ----------------

    private const val TAG_BUTTON = "hyperceiler_custom_sticker_add"

    private fun dp(ctx: Context, value: Int): Int {
        return Math.round(value * ctx.resources.displayMetrics.density)
    }
}
