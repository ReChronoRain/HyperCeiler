package com.sevtinge.cemiuiler.module.securitycenter

import android.content.Context
import android.graphics.*
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.ColorUtils
import com.sevtinge.cemiuiler.utils.HookUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object BlurSecurity : BaseHook() {
    val blurRadius = mPrefsMap.getInt("security_center_blurradius", 60)
    val backgroundColor = mPrefsMap.getInt("security_center_color", -1)
    val shouldInvertColor = !ColorUtils.isDarkColor(backgroundColor)

    private var appVersionCode = 40000727

    // 反色 同时保持红蓝色变化不大
    @RequiresApi(Build.VERSION_CODES.S)
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
    val invertColorWhiteList =
        arrayOf(
            "lv_main",
            "second_view"
        )

    // keepList 列表内元素及其子元素不会反色
    val keepColorList =
        arrayOf(
            "rv_information"
        )

    override fun init() {
        val TurboLayoutClass = findClassIfExists(
            "com.miui.gamebooster.windowmanager.newbox.TurboLayout"
        ) ?: return
        val NewToolBoxTopViewClass = findClassIfExists(
            "com.miui.gamebooster.windowmanager.newbox.NewToolBoxTopView"
        ) ?: return
        var VideoBoxViewClass =
            findClassIfExists("com.miui.gamebooster.videobox.adapter.i")
        var VideoBoxViewMethodName = "a"
        if (VideoBoxViewClass == null) {
            // v7.4.9
            appVersionCode = 40000749
            VideoBoxViewClass = findClassIfExists("t7.i") ?: return
            VideoBoxViewMethodName = "i"
        }

        var NewboxClass: Class<*>? = null
        TurboLayoutClass.methods.forEach {
            if (it.name == "getDockLayout") {
                NewboxClass = it.returnType
            }
        }
        if (NewboxClass == null) {
            return
        }

        XposedBridge.hookAllConstructors(NewboxClass, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val view = param.thisObject as View
                view.addOnAttachStateChangeListener(
                    object :
                        View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(view: View) {
                            // 已有背景 避免重复添加

                            if (view.background != null) {
                                if (HookUtils.isBlurDrawable(view.background)) {
                                    return;
                                }
                            }

                            view.background =
                                HookUtils.createBlurDrawable(
                                    view,
                                    blurRadius,
                                    40,
                                    backgroundColor
                                )
                        }

                        override fun onViewDetachedFromWindow(view: View) {
                            view.background = null
                        }
                    })
            }
        })

        XposedBridge.hookAllConstructors(NewToolBoxTopViewClass, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val view = param.thisObject as View
                view.addOnAttachStateChangeListener(
                    object :
                        View.OnAttachStateChangeListener {
                        @RequiresApi(Build.VERSION_CODES.S)
                        override fun onViewAttachedToWindow(view: View) {
                            val viewPaernt = view.parent as ViewGroup
                            val gameContentLayout = viewPaernt.parent as ViewGroup
                            if (gameContentLayout.background != null) {
                                if (HookUtils.isBlurDrawable(gameContentLayout.background)) {
                                    return;
                                }
                            }

                            gameContentLayout.background =
                                HookUtils.createBlurDrawable(
                                    gameContentLayout,
                                    blurRadius,
                                    40,
                                    backgroundColor
                                )

                            if (shouldInvertColor) {
                                invertViewColor(gameContentLayout)

                                //设置 RenderEffect 后会导致文字动画出现问题，故去除动画
                                val performanceTextView = XposedHelpers.callMethod(
                                    param.thisObject,
                                    "getPerformanceTextView"
                                ) as View
                                XposedHelpers.findAndHookMethod(
                                    performanceTextView.javaClass,
                                    if (appVersionCode >= 40000749) "e" else "a",
                                    Boolean::class.java,
                                    object :
                                        XC_MethodReplacement() {
                                        override fun replaceHookedMethod(param: MethodHookParam?) {
                                            param?.result = null
                                        }
                                    })
                            }

                            var headBackground =
                                HookUtils.getValueByField(param.thisObject, "j")
                            if (headBackground == null) {
                                headBackground = HookUtils.getValueByField(param.thisObject, "j")
                            } else if (!headBackground.javaClass.name.contains("ImageView")) {
                                headBackground = HookUtils.getValueByField(param.thisObject, "C")
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
                            gameContentLayout.background = null
                        }
                    })
            }
        })


        XposedHelpers.findAndHookMethod(
            VideoBoxViewClass,
            VideoBoxViewMethodName,
            Context::class.java,
            Boolean::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val mainContent = HookUtils.getValueByField(param.thisObject, "b") as ViewGroup
                    mainContent.addOnAttachStateChangeListener(
                        object :
                            View.OnAttachStateChangeListener {
                            @RequiresApi(Build.VERSION_CODES.S)
                            override fun onViewAttachedToWindow(view: View) {
                                if (view.background != null) {
                                    if (HookUtils.isBlurDrawable(view.background)) {
                                        return;
                                    }
                                }

                                view.background =
                                    HookUtils.createBlurDrawable(
                                        view,
                                        blurRadius,
                                        40,
                                        backgroundColor
                                    )

                                if (shouldInvertColor) {
                                    invertViewColor(mainContent)
                                }
                            }

                            override fun onViewDetachedFromWindow(view: View) {
                                view.background = null
                            }
                        })
                }
            })

        if (shouldInvertColor) {
            val DetailSettingsLayoutClass = findClassIfExists(
                "com.miui.gamebooster.videobox.view.DetailSettingsLayout"
            ) ?: return
            val SrsLevelSeekBarProClass = findClassIfExists(
                "com.miui.gamebooster.videobox.view.SrsLevelSeekBarPro"
            ) ?: return
            var SrsLevelSeekBarInnerViewClass = findClassIfExists(
                "com.miui.gamebooster.videobox.view.c"
            )
            if (SrsLevelSeekBarInnerViewClass == null) {
                SrsLevelSeekBarInnerViewClass = findClassIfExists(
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

            var SecondViewClass =
                findClassIfExists("com.miui.gamebooster.windowmanager.newbox.n")
            var SecondViewMethodName = "b"

            if (appVersionCode >= 40000749) {
                SecondViewClass = findClassIfExists(
                    "com.miui.gamebooster.windowmanager.newbox.j"
                ) ?: return
                SecondViewMethodName = "B"
            }
            val AuditionViewClass =
                findClassIfExists("com.miui.gamebooster.customview.AuditionView")
                    ?: return

            XposedBridge.hookAllMethods(
                DetailSettingsLayoutClass,
                "setFunctionType",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val marqueeTextView = HookUtils.getValueByField(param.thisObject, "d")
                        if (marqueeTextView != null) {
                            marqueeTextView as TextView
                            marqueeTextView.setTextColor(Color.GRAY)
                        }
                        val listView = HookUtils.getValueByField(param.thisObject, "c") as ListView
                        val listViewAdapterClassName = listView.adapter.javaClass.name
                        val listViewAdapterInnerClass =
                            findClassIfExists("$listViewAdapterClassName\$a")
                                ?: return
                        XposedBridge.hookAllMethods(
                            listViewAdapterInnerClass,
                            "a",
                            object : XC_MethodHook() {
                                @RequiresApi(Build.VERSION_CODES.S)
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
                SrsLevelSeekBarProClass,
                if (appVersionCode >= 40000749) "b" else "a", Context::class.java,
                AttributeSet::class.java, Int::class.java, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val bgColorField = SrsLevelSeekBarProClass.getDeclaredField("j")
                        bgColorField.isAccessible = true
                        bgColorField.setInt(
                            param.thisObject,
                            ColorUtils.addAlphaForColor(Color.GRAY, 150)
                        )

                        val selectTxtColorField =
                            SrsLevelSeekBarProClass.getDeclaredField("l")
                        selectTxtColorField.isAccessible = true
                        selectTxtColorField.setInt(
                            param.thisObject,
                            Color.WHITE
                        )

                        val normalTxtColorField =
                            SrsLevelSeekBarProClass.getDeclaredField("l")
                        normalTxtColorField.isAccessible = true
                        normalTxtColorField.setInt(
                            param.thisObject,
                            Color.WHITE
                        )
                    }
                }
            )

            XposedHelpers.findAndHookMethod(SrsLevelSeekBarInnerViewClass, "a", Context::class.java,
                AttributeSet::class.java, Int::class.java, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val bgColorField = SrsLevelSeekBarInnerViewClass.getDeclaredField("h")
                        bgColorField.isAccessible = true
                        bgColorField.setInt(
                            param.thisObject,
                            ColorUtils.addAlphaForColor(Color.WHITE, 150)
                        )
                    }
                }
            )

            XposedHelpers.findAndHookMethod(
                SecondViewClass,
                SecondViewMethodName,
                View::class.java,
                object : XC_MethodHook() {
                    @RequiresApi(Build.VERSION_CODES.S)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.args[0] as View
                        invertViewColor(view, gameBoxWhiteList, gameBoxKeepList)
                    }
                })

            // 让图标颜色更深一点
            XposedHelpers.findAndHookMethod(
                AuditionViewClass,
                if (appVersionCode >= 40000749) "M" else "a",
                Context::class.java,
                object : XC_MethodHook() {
                    @RequiresApi(Build.VERSION_CODES.S)
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = HookUtils.getValueByField(param.thisObject, "d") as View
                        val parentView = view.parent
                        HookUtils.log(parentView)
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
    @RequiresApi(Build.VERSION_CODES.S)
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
            HookUtils.log(e.message)
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
            HookUtils.log(e.message)
        }
        return true
    }

    private fun getId(view: View): String {
        return if (view.id == View.NO_ID) "no-id" else view.resources.getResourceName(view.id)
            .replace("com.miui.securitycenter:id/", "")
    }
}