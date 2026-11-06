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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.sidebar

import android.content.Context
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.ColorUtils
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.BlurUtils.createBlurDrawable
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.BlurUtils.isBlurDrawable
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.addMiBackgroundBlendColor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.clearMiBackgroundBlendColor
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.setBlurRoundRect
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.setMiBackgroundBlurRadius
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.MiBlurUtilsKt.setMiViewBlurMode
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.hookAllConstructors
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook
import java.lang.reflect.Method

object BlurSecurity : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        lottieAnimation
        gameManagerMethod
        return true
    }
    private val blurRadius by lazy {
        PrefsBridge.getInt("security_center_blurradius", 60)
    }
    private val blurSuper by lazy {
        PrefsBridge.getBoolean("security_center_blur_model_super")
    }
    private val backgroundColor by lazy {
        PrefsBridge.getInt("security_center_color", -1)
    }
    private val isInvertColor by lazy {
        PrefsBridge.getBoolean("security_center_invert_color")
    }
    private val shouldInvertColor = !ColorUtils.isDarkColor(backgroundColor)

    // 反色 同时保持红蓝色变化不大
    private val invertColorRenderEffect = RenderEffect.createColorFilterEffect(
        ColorMatrixColorFilter(
            floatArrayOf(
                1f, 1f, -2f, 0f, 16f,
                0f, 0f, 0f, 0f, 0f,
                -3f, 1f, 2f, 0f, 16f,
                0f, 0f, 0f, 0.85f, 0f
            )
        )
    )

    // 不反转颜色的名单ID或类名
    // whiteList 不在列表内子元素也会反色
    private val invertColorWhiteList = arrayOf("lv_main", "second_view")

    // keepList 列表内元素及其子元素不会反色
    private val keepColorList = arrayOf("rv_information")

    // 为什么必须逐候选验形状、三版实测结果都在 init() 里那个 hook 上方逐条列出；
    // 这里只做「候选类 + a(Context, AttributeSet, int/Integer) + int 字段 h」的整体筛选。
    private fun findInnerViewClassByShape(): Class<*>? = sequenceOf(
        "com.miui.gamebooster.videobox.view.c",
        "b8.c",
    ).mapNotNull { findClassIfExists(it) }.firstOrNull { clazz ->
        hasInnerViewShape(clazz)
    }

    private fun hasInnerViewShape(clazz: Class<*>): Boolean {
        val methodOk = clazz.declaredMethods.any { method ->
            method.name == "a" && method.parameterTypes.size == 3 &&
                Context::class.java.isAssignableFrom(method.parameterTypes[0]) &&
                AttributeSet::class.java.isAssignableFrom(method.parameterTypes[1]) &&
                isIntLike(method.parameterTypes[2])
        }
        val fieldOk = clazz.declaredFields.any { field ->
            field.name == "h" && isIntLike(field.type)
        }
        return methodOk && fieldOk
    }

    private fun isIntLike(type: Class<*>): Boolean =
        type == Int::class.javaPrimitiveType || type == Integer::class.java

    private val lottieAnimation by lazy<Method?> {
        // 该锚点只是 assets 目录名：assets/game_turbo_box_mode_change/{blue_light,red_light}.json
        // 在 OS3.0.319 / OS4.0.0.44 / OS4.0.5.0 三版 MIUISecurityCenter.apk 里都还在（各 2 个文件），
        // 但三版 classes*.dex 里对 "game_turbo_box_mode_change" 的引用都是 0 次（二进制 grep）,
        // 即这条查询在三版上必然 0 命中——因此这里必须是可降级的可选成员，
        // 否则 requiredMember 抛错会让整条 BlurSecurity（模糊 + 反色）被整体跳过。
        optionalMember("BlurSecurity1") {
            it.findMethod {
                searchPackages = listOf("com.miui.gamebooster.windowmanager.newbox")
                matcher {
                    addUsingString("game_turbo_box_mode_change")
                }
            }.singleOrNull()
        }
    }

    private val gameManagerMethod by lazy<Method> {
        requiredMember("BlurSecurity2") {
            it.findMethod {
                searchPackages = listOf("com.miui.gamebooster.windowmanager.newbox")
                matcher {
                    usingStrings = listOf("addView error")
                }
            }.single()
        }
    }

    override fun init() {
        val turboLayoutClass = findClassIfExists(
            "com.miui.gamebooster.windowmanager.newbox.TurboLayout"
        ) ?: return

        var dockLayoutClass: Class<*>? = null
        turboLayoutClass.methods.forEach {
            if (it.name == "getDockLayout") {
                dockLayoutClass = it.returnType
            }
        }
        if (dockLayoutClass == null) {
            return
        }

        // dock 应用栏
        dockLayoutClass.hookAllConstructors {
            after { param ->
                val view = param.thisObject as View
                val listener = object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(view: View) {
                        setBlurBg(view)
                    }

                    override fun onViewDetachedFromWindow(view: View) {
                        clearBlurBg(view)
                    }
                }
                view.addOnAttachStateChangeListener(listener)
                registerHotReloadCleanup {
                    view.removeOnAttachStateChangeListener(listener)
                }
            }
        }

        // 工具箱主体(这里只处理视频/会议/通话工具箱)
        findAndHookMethod(turboLayoutClass, "getTargetBox", object : IMethodHook {
            override fun after(param: HookParam) {
                val targetBox = param.result as? View ?: return
                val listener = object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(view: View) {
                        val mainContent: View? = (view as ViewGroup).getChildAt(0)
                        if (mainContent != null) {
                            /**
                             * 视频/会议/通话工具箱 ID 为 main_content
                             * 游戏工具箱无 ID, 但不要在此操作游戏工具箱
                             * 因为会导致游戏工具箱主体扩展时本该透明的区域却设置了背景, 例如“亮度”
                             */
                            if (mainContent.id != View.NO_ID) {
                                mainContent.background = null
                                setBlurBg(view)

                                if (shouldInvertColor && isInvertColor) {
                                    invertViewColor(view)
                                }
                            }
                        }
                    }

                    override fun onViewDetachedFromWindow(view: View) {
                        clearBlurBg(view)
                    }
                }
                targetBox.addOnAttachStateChangeListener(listener)
                registerHotReloadCleanup {
                    targetBox.removeOnAttachStateChangeListener(listener)
                }
            }
        })

        val newToolBoxTopViewClass = findClassIfExists(
            "com.miui.gamebooster.windowmanager.newbox.NewToolBoxTopView"
        ) ?: return
        // 游戏工具箱
        com.sevtinge.hyperceiler.libhook.base.BaseHook.hookAllConstructors(newToolBoxTopViewClass, object : IMethodHook {
            override fun after(param: HookParam) {
                val view = param.thisObject as View
                val listener = object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(view: View) {
                        val viewParent = view.parent as ViewGroup
                        val gameContentLayout = viewParent.parent as ViewGroup
                        setBlurBg(gameContentLayout)
                        if (shouldInvertColor && isInvertColor) {
                            invertViewColor(gameContentLayout)

                            /**
                             * 设置 RenderEffect 后会导致文字动画出现问题，故去除动画
                             * 暂时把整个动画(包括 lottie 动画和文字动画)去除, 仅去除文字动画可能因版本混淆而 hook 失败
                             * 在 40000727 版本号中去除文字动画：
                             * com.miui.gamebooster.windowmanager.newbox.NewToolBoxTopView
                             * ↳ getPerformanceTextView
                             *   ↳ e(boolean)
                             */
                            lottieAnimation?.createHook {
                                replace {
                                    null
                                }
                            }
                        }
                    }

                    override fun onViewDetachedFromWindow(v: View) {
                        val viewParent = view.parent as ViewGroup
                        val gameContentLayout = viewParent.parent as ViewGroup
                        clearBlurBg(gameContentLayout)
                    }
                }
                view.addOnAttachStateChangeListener(listener)
                registerHotReloadCleanup {
                    view.removeOnAttachStateChangeListener(listener)
                }
            }
        })

        // 隐藏视频/游戏工具箱顶部静态图
        hookAllConstructors(
            ImageView::class.java,
            object : IMethodHook {
                override fun after(param: HookParam) {
                    (param.thisObject as? ImageView?)?.let {
                        if (it.id != View.NO_ID) {
                            val id = getId(it)
                            if (id == "video_box_top_line_bg" || id == "game_turbo_top_line_bg") {
                                it.setImageDrawable(null)
                                it.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        )

        // ======================================
        if (shouldInvertColor && isInvertColor) {
            val detailSettingsLayoutClass = findClassIfExists(
                "com.miui.gamebooster.videobox.view.DetailSettingsLayout"
            ) ?: return
            val srsLevelSeekBarProClass = findClassIfExists(
                "com.miui.gamebooster.videobox.view.SrsLevelSeekBarPro"
            ) ?: return
            // 两个候选名在三版 MIUISecurityCenter.apk 里的实测状态（DexKit 索引 + dexdump 复核）：
            //   com.miui.gamebooster.videobox.view.c ：三版都不存在
            //   b8.c                                 ：OS3 存在（17 个方法，全是 a(Lb8/c;)V 这类
            //                                           合成/lambda 方法）、OS4.0.0.44 存在但是个无关的
            //                                           记录类（<init>(String,String,I)/a()String/b()I/
            //                                           equals/toString）、OS4.0.5 已不存在
            //   即三版都没有 a(Context, AttributeSet, int)；另外 videobox/view/SrsLevelSeekBar
            //   只有 OS3 还有（44/405 换成了 SrsLevelLabelView），只剩 Pro。
            // 旧写法这里的 `?: return` 与只补「类找不到」两种失效形态三版不一样：
            //   OS4.0.5：b8.c 也没了 -> 直接 return，从 gameManagerMethod 反色 hook 起的
            //            其余全部 hook（Pro、AuditionView）一起丢；
            //   OS3 / OS4.0.0.44：b8.c 找得到 -> 不 return，但到了下面 findAndHookMethod
            //            时 ezhooktool 的 resolveMethod 只能回 null（getDeclaredMethod 失配、
            //            BestMatchUtils 也没有三个参的 a），installCallback 随即抛
            //            IllegalStateException，本行之后的 AuditionView#M（图标加深）hook 丢。
            // 所以这里按「类 + 方法形状 + 字段形状」一起验：三版都过不了，就只跳过这一个 hook。
            // 另：三版里真正同时具备 (Context, AttributeSet, int) 与 int 字段 h 的只有
            // SrsLevelSeekBarPro#b，但 Pro 上的 h 只是同名字段，无法确认就是原 inner-view
            // 要改的背景色字段（上面那条 Pro hook 已在改 j/l），所以这里不做顶替，只降级；
            // 该 inner-view hook 目前三版都是空转（保留代码是为了类回来时自动复活）。
            val srsLevelSeekBarInnerViewClass = findInnerViewClassByShape()
            if (srsLevelSeekBarInnerViewClass == null) {
                XposedLog.d(TAG, this.lpparam.packageName,
                    "SrsLevelSeekBarInnerView shape not found, skip its color hook only")
            }
            val videoBoxWhiteList = arrayOf(
                "miuix.slidingwidget.widget.SlidingButton",
                "android.widget.ImageView",
                "android.widget.CompoundButton",
                "com.miui.common.widgets.gif.GifImageView",
                "com.miui.gamebooster.videobox.view.SrsLevelSeekBar",
                "com.miui.gamebooster.videobox.view.SrsLevelSeekBarPro",
                "com.miui.gamebooster.videobox.view.VideoEffectImageView",
                "com.miui.gamebooster.videobox.view.DisplayStyleImageView",
                "com.miui.gamebooster.videobox.view.c",
                "b8.c",
                "com.miui.gamebooster.videobox.view.VBIndicatorView"
            )

            val gameBoxWhiteList = arrayOf(
                "audition_view",
                "miuix.slidingwidget.widget.SlidingButton"
            )

            val videoBoxKeepList = arrayOf("img_wrapper2")
            val gameBoxKeepList = arrayOf(
                "rl_header",
                "tv_barrage_color_pick",
                "seekbar_text_size",
                "seekbar_text_speed"
            )

            gameManagerMethod.createAfterHook {
                val view = it.args[0] as View
                invertViewColor(view, gameBoxWhiteList, gameBoxKeepList)
            }

            val auditionViewClass =
                findClassIfExists("com.miui.gamebooster.customview.AuditionView") ?: return

            com.sevtinge.hyperceiler.libhook.base.BaseHook.hookAllMethods(
                detailSettingsLayoutClass,
                "setFunctionType",
                object : IMethodHook {
                    override fun after(param: HookParam) {
                        val marqueeTextView = getObjectField(param.thisObject, "d")
                        if (marqueeTextView != null) {
                            marqueeTextView as TextView
                            marqueeTextView.setTextColor(Color.GRAY)
                        }
                        val listView = getObjectField(param.thisObject, "c") as ListView
                        val listViewAdapterClassName = listView.adapter.javaClass.name
                        val listViewAdapterInnerClass =
                            findClassIfExists($$"$$listViewAdapterClassName$a") ?: return
                        com.sevtinge.hyperceiler.libhook.base.BaseHook.hookAllMethods(
                            listViewAdapterInnerClass,
                            "a",
                            object : IMethodHook {
                                override fun after(param: HookParam) {
                                    val isSetupFunction =
                                        param.args[0].toString().contains("BaseModel")
                                    if (isSetupFunction) {
                                        listViewAdapterInnerClass.declaredFields.forEach { field ->
                                            val currentObject = field.get(param.thisObject)
                                            if (currentObject is ImageView) {
                                                if (getId(currentObject) == "img1" || getId(
                                                        currentObject
                                                    ) == "img2"
                                                ) {
                                                    currentObject.setRenderEffect(
                                                        RenderEffect.createColorFilterEffect(
                                                            ColorMatrixColorFilter(
                                                                floatArrayOf(
                                                                    1f, 0f, 0f, 0f, 0f,
                                                                    0f, 1f, 0f, 0f, 0f,
                                                                    0f, 0f, 1f, 0f, 0f,
                                                                    0.5f, 0.5f, 0.5f, 0f, 0f
                                                                )
                                                            )
                                                        )
                                                    )
                                                }
                                            }
                                            if (currentObject is View) {
                                                invertViewColor(
                                                    currentObject,
                                                    videoBoxWhiteList,
                                                    videoBoxKeepList
                                                )
                                            }
                                        }
                                    }
                                }
                            })
                    }
                })

            findAndHookMethod(
                srsLevelSeekBarProClass,
                "b", Context::class.java,
                AttributeSet::class.java, Int::class.java, object : IMethodHook {
                    override fun after(param: HookParam) {
                        val bgColorField = srsLevelSeekBarProClass.getDeclaredField("j")
                        bgColorField.isAccessible = true
                        bgColorField.setInt(
                            param.thisObject,
                            ColorUtils.addAlphaForColor(Color.GRAY, 150)
                        )

                        val selectTxtColorField =
                            srsLevelSeekBarProClass.getDeclaredField("l")
                        selectTxtColorField.isAccessible = true
                        selectTxtColorField.setInt(
                            param.thisObject,
                            Color.WHITE
                        )

                        val normalTxtColorField =
                            srsLevelSeekBarProClass.getDeclaredField("l")
                        normalTxtColorField.isAccessible = true
                        normalTxtColorField.setInt(
                            param.thisObject,
                            Color.WHITE
                        )
                    }
                }
            )

            srsLevelSeekBarInnerViewClass?.let { innerViewClass ->
                findAndHookMethod(innerViewClass, "a", Context::class.java,
                    AttributeSet::class.java, Int::class.java, object : IMethodHook {
                        override fun after(param: HookParam) {
                            val bgColorField = innerViewClass.getDeclaredField("h")
                            bgColorField.isAccessible = true
                            bgColorField.setInt(
                                param.thisObject,
                                ColorUtils.addAlphaForColor(Color.WHITE, 150)
                            )
                        }
                    }
                )
            }

            // 让图标颜色更深一点
            findAndHookMethod(
                auditionViewClass,
                "M",
                Context::class.java,
                object : IMethodHook {
                    override fun after(param: HookParam) {
                        val view = getObjectField(param.thisObject, "d") as View
                        val parentView = view.parent
                        if (parentView is ViewGroup) {
                            val lastChild = parentView.getChildAt(parentView.childCount - 1)
                            if (lastChild is ImageView && lastChild.drawable is VectorDrawable) {
                                val oldDrawable = lastChild.drawable
                                val newDrawable = LayerDrawable(
                                    arrayOf(
                                        oldDrawable,
                                        oldDrawable,
                                        oldDrawable,
                                        oldDrawable,
                                        oldDrawable
                                    )
                                )
                                lastChild.setImageDrawable(newDrawable)
                            }
                        }
                        invertViewColor(view, gameBoxWhiteList, gameBoxKeepList)
                    }
                })

        }
    }

    private fun setBlurBg(view: View) {
        // 已有背景 避免重复添加
        if (!blurSuper) {
            if (view.background != null) {
                if (isBlurDrawable(view.background)) {
                    return
                }
            }

            view.background =
                createBlurDrawable(view, blurRadius, 40, backgroundColor)
        } else {
            view.apply {
                setBackgroundColor(backgroundColor)
                clearMiBackgroundBlendColor()
                setMiViewBlurMode(1)
                setMiBackgroundBlurRadius(40)
                setBlurRoundRect(40) // 给视频/通话/会议工具箱设置的圆角
                addMiBackgroundBlendColor(Color.argb(255, 0, 0, 0), 103)
            }
        }
    }

    private fun clearBlurBg(view: View) {
        if (!blurSuper) view.background = null
    }

    // 尽量给最外层加 RenderEffect 而不是 最内层
    // whiteList 不在名单内的子视图依旧反转
    // keepList 本身及子视图均不反转
    fun invertViewColor(
        view: View,
        whiteList: Array<String> = invertColorWhiteList,
        keepList: Array<String>? = keepColorList,
    ) {
        if (keepList != null) {
            if (keepList.contains(getId(view))) {
                return
            }
            if (keepList.contains(view.javaClass.name)) {
                return
            }
        }
        try {
            if (isChildNeedInvertColor(view, whiteList, keepList)) {
                view.setRenderEffect(invertColorRenderEffect)
            } else {
                if (view is ViewGroup) {
                    for (index in 0 until view.childCount) {
                        val childView = view.getChildAt(index)
                        if (childView != null) {
                            invertViewColor(childView, whiteList, keepList)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            XposedLog.w(TAG, this.lpparam.packageName, "invertViewColor err", e)
        }
    }

    private fun isChildNeedInvertColor(
        view: View,
        whiteList: Array<String>,
        keepList: Array<String>?,
    ): Boolean {
        val viewId = getId(view)
        if (whiteList.contains(viewId)) {
            return false
        }
        if (whiteList.contains(view.javaClass.name)) {
            return false
        }
        if (keepList != null) {
            if (keepList.contains(getId(view))) {
                return false
            }
            if (keepList.contains(view.javaClass.name)) {
                return false
            }
        }
        try {
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) {
                    val childView = view.getChildAt(index)
                    if (childView != null) {
                        if (!isChildNeedInvertColor(childView, whiteList, keepList)) {
                            return false
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            XposedLog.w(TAG, this.lpparam.packageName, "isChildNeedInvertColor err", e)
        }
        return true
    }

    private fun getId(view: View): String {
        return if (view.id == View.NO_ID) "no-id" else view.resources.getResourceName(view.id)
            .replace("com.miui.securitycenter:id/", "")
    }

}

