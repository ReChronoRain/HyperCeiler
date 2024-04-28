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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar

import android.content.*
import android.graphics.*
import android.graphics.drawable.*
import android.util.*
import android.view.*
import android.widget.*
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createAfterHook
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.utils.*
import com.sevtinge.hyperceiler.utils.blur.BlurUtils.*
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.addMiBackgroundBlendColor
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.clearMiBackgroundBlendColor
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.setMiBackgroundBlurRadius
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt.setMiViewBlurMode
import com.sevtinge.hyperceiler.utils.color.*
import de.robv.android.xposed.*

object BlurSecurity : BaseHook() {
    private val blurRadius by lazy {
        mPrefsMap.getInt("security_center_blurradius", 60)
    }
    private val blurSuper by lazy {
        mPrefsMap.getBoolean("security_center_blur_model_super")
    }
    private val backgroundColor by lazy {
        mPrefsMap.getInt("security_center_color", -1)
    }
    private val isInvertColor by lazy {
        mPrefsMap.getBoolean("security_center_invert_color")
    }
    private val shouldInvertColor = !ColorUtils.isDarkColor(backgroundColor)

    private var appVersionCode = 40000727

    // 反色 同时保持红蓝色变化不大
    val invertColorRenderEffect = RenderEffect.createColorFilterEffect(
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

    override fun init() {
        val turboLayoutClass = findClassIfExists(
            "com.miui.gamebooster.windowmanager.newbox.TurboLayout"
        ) ?: return
        val newToolBoxTopViewClass = findClassIfExists(
            "com.miui.gamebooster.windowmanager.newbox.NewToolBoxTopView"
        ) ?: return

        var newBoxClass: Class<*>? = null
        turboLayoutClass.methods.forEach {
            if (it.name == "getDockLayout") {
                newBoxClass = it.returnType
            }
        }
        if (newBoxClass == null) {
            return
        }

        XposedBridge.hookAllConstructors(newBoxClass, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val view = param.thisObject as View
                view.addOnAttachStateChangeListener(
                    object :
                        View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(view: View) {
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
                                    addMiBackgroundBlendColor(Color.argb(255, 0, 0, 0), 103)
                                }
                            }
                        }

                        override fun onViewDetachedFromWindow(view: View) {
                            if (!blurSuper) view.background = null
                        }
                    })
            }
        })

        XposedBridge.hookAllConstructors(newToolBoxTopViewClass, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val view = param.thisObject as View
                view.addOnAttachStateChangeListener(
                    object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(view: View) {
                            val viewPaernt = view.parent as ViewGroup
                            val gameContentLayout = viewPaernt.parent as ViewGroup
                            if (!blurSuper) {
                                if (gameContentLayout.background != null) {
                                    if (isBlurDrawable(gameContentLayout.background)) {
                                        return
                                    }
                                }

                                gameContentLayout.background =
                                    createBlurDrawable(
                                        gameContentLayout,
                                        blurRadius,
                                        40,
                                        backgroundColor
                                    )
                            } else {
                                view.apply {
                                    setBackgroundColor(backgroundColor)
                                    clearMiBackgroundBlendColor()
                                    setMiViewBlurMode(1)
                                    setMiBackgroundBlurRadius(40)
                                    addMiBackgroundBlendColor(Color.argb(255, 0, 0, 0), 103)
                                }
                            }
                            if (shouldInvertColor && isInvertColor) {
                                invertViewColor(gameContentLayout)

                                // 设置 RenderEffect 后会导致文字动画出现问题，故去除动画
                                val performanceTextView =
                                    XposedHelpers.callMethod(
                                        param.thisObject,
                                        "getPerformanceTextView"
                                    ) as View

                                XposedHelpers.findAndHookMethod(
                                    performanceTextView.javaClass,
                                    if (appVersionCode >= 40000749) "e" else "a",
                                    Boolean::class.java, object : XC_MethodReplacement() {
                                        override fun replaceHookedMethod(param: MethodHookParam?) {
                                            param?.result = null
                                        }
                                    })
                            }

                            var headBackground =
                                getValueByField(param.thisObject, "j")
                            if (headBackground == null) {
                                headBackground = getValueByField(param.thisObject, "j")
                            } else if (!headBackground.javaClass.name.contains("ImageView")) {
                                headBackground = getValueByField(param.thisObject, "C")
                            }
                            if (headBackground == null) {
                                return
                            }
                            if (headBackground.javaClass.name.contains("ImageView")) {
                                headBackground as ImageView
                                headBackground.visibility = View.GONE
                            }
                        }

                        override fun onViewDetachedFromWindow(view: View) {
                            val viewPaernt = view.parent as ViewGroup
                            val gameContentLayout = viewPaernt.parent as ViewGroup
                            if (!blurSuper) gameContentLayout.background = null
                        }
                    })
            }
        })

        // if (getPackageVersionCode(lpparam) >= 40000754) {
        DexKit.getDexKitBridge().findMethod {
            matcher {
                returnType = "android.view.View"
                paramTypes = listOf("android.content.Context", "boolean", "boolean")
            }
        }.single().getMethodInstance(lpparam.classLoader).createAfterHook { param ->
            val mainContent = getValueByField(param.thisObject, "b") as ViewGroup
            mainContent.addOnAttachStateChangeListener(object :
                View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) {
                    if (!blurSuper) {
                        if (view.background != null) {
                            if (isBlurDrawable(view.background)) return
                        }
                        view.background =
                            createBlurDrawable(view, blurRadius, 40, backgroundColor)
                    } else {
                        view.apply {
                            setBackgroundColor(backgroundColor)
                            clearMiBackgroundBlendColor()
                            setMiViewBlurMode(1)
                            setMiBackgroundBlurRadius(40)
                            addMiBackgroundBlendColor(Color.argb(255, 0, 0, 0), 103)
                        }
                    }
                    if (shouldInvertColor && isInvertColor) invertViewColor(mainContent)
                }

                override fun onViewDetachedFromWindow(view: View) {
                    if (!blurSuper) view.background = null
                }
            })
        }

        if (shouldInvertColor && isInvertColor) {
            val detailSettingsLayoutClass = findClassIfExists(
                "com.miui.gamebooster.videobox.view.DetailSettingsLayout"
            ) ?: return
            val srsLevelSeekBarProClass = findClassIfExists(
                "com.miui.gamebooster.videobox.view.SrsLevelSeekBarPro"
            ) ?: return
            var srsLevelSeekBarInnerViewClass = findClassIfExists(
                "com.miui.gamebooster.videobox.view.c"
            )
            if (srsLevelSeekBarInnerViewClass == null) {
                srsLevelSeekBarInnerViewClass = findClassIfExists(
                    "b8.c"
                ) ?: return
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

            val gameManagerMethod = DexKit.getDexKitBridge().findMethod {
                searchPackages = listOf("com.miui.gamebooster.windowmanager.newbox")
                matcher {
                    usingStrings = listOf("addView error")
                }
            }.single().getMethodInstance(safeClassLoader)

            gameManagerMethod.createAfterHook {
                val view = it.args[0] as View
                invertViewColor(view, gameBoxWhiteList, gameBoxKeepList)
            }

            val auditionViewClass =
                findClassIfExists("com.miui.gamebooster.customview.AuditionView") ?: return

            XposedBridge.hookAllMethods(
                detailSettingsLayoutClass,
                "setFunctionType",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val marqueeTextView = getValueByField(param.thisObject, "d")
                        if (marqueeTextView != null) {
                            marqueeTextView as TextView
                            marqueeTextView.setTextColor(Color.GRAY)
                        }
                        val listView = getValueByField(param.thisObject, "c") as ListView
                        val listViewAdapterClassName = listView.adapter.javaClass.name
                        val listViewAdapterInnerClass =
                            findClassIfExists("$listViewAdapterClassName\$a") ?: return
                        XposedBridge.hookAllMethods(
                            listViewAdapterInnerClass,
                            "a",
                            object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam) {
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

            XposedHelpers.findAndHookMethod(
                srsLevelSeekBarProClass,
                if (appVersionCode >= 40000749) "b" else "a", Context::class.java,
                AttributeSet::class.java, Int::class.java, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
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

            XposedHelpers.findAndHookMethod(srsLevelSeekBarInnerViewClass, "a", Context::class.java,
                AttributeSet::class.java, Int::class.java, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val bgColorField = srsLevelSeekBarInnerViewClass.getDeclaredField("h")
                        bgColorField.isAccessible = true
                        bgColorField.setInt(
                            param.thisObject,
                            ColorUtils.addAlphaForColor(Color.WHITE, 150)
                        )
                    }
                }
            )

            // 让图标颜色更深一点
            XposedHelpers.findAndHookMethod(
                auditionViewClass,
                if (appVersionCode >= 40000749) "M" else "a",
                Context::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = getValueByField(param.thisObject, "d") as View
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
            logW(TAG, this.lpparam.packageName, "invertViewColor err", e)
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
            logW(TAG, this.lpparam.packageName, "isChildNeedInvertColor err", e)
        }
        return true
    }

    private fun getId(view: View): String {
        return if (view.id == View.NO_ID) "no-id" else view.resources.getResourceName(view.id)
            .replace("com.miui.securitycenter:id/", "")
    }

}
