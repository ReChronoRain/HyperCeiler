//val shortcutMenuBackgroundAlpha = getInt("shortcutMenuBackgroundAlpha",255)
package com.sevtinge.cemiuiler.module.wini.blur

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat.animate
import com.sevtinge.cemiuiler.module.wini.model.ConfigModel
import com.sevtinge.cemiuiler.utils.HookUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import kotlin.collections.ArrayList
import kotlin.math.sqrt

class BlurWhenShowShortcutMenu (private val classLoader: ClassLoader, config: ConfigModel) {

    val shortcutMenuBackgroundAlpha = config.BlurWhenShowShortcutMenu.shortcutMenuBackgroundAlpha

    /*
    两层视图alpha计算公式：2x-x^2=y
    x为单层视图alpha 0完全透明 1完全不透明
    y为双层混合后的透明度
    x与y在图层透明度这种情况下永远为正值
    将改公式转换为x=f(y)：x=1-√(1-y)
    */
    val singleLayerAlpha = ((1.0 - sqrt(1.0 - (shortcutMenuBackgroundAlpha / 255.0))) * 255.0).toInt()

    val BLUR_ICON_APP_NAME = arrayOf("锁屏", "手电筒", "数据", "飞行模式", "蓝牙", "WLAN 热点")
    val allBluredDrawable: MutableList<Drawable> = ArrayList()

    fun addBlurEffectToFolderIcon() {
        val FolderIconClass = HookUtils.getClass(
            "com.miui.home.launcher.FolderIcon",
            classLoader
        ) ?: return
        val ThumbnailContainerClass = HookUtils.getClass(
            "com.miui.home.launcher.ThumbnailContainer",
            classLoader
        ) ?: return
        val DragViewClass = HookUtils.getClass(
            "com.miui.home.launcher.DragView",
            classLoader
        ) ?: return
        val ItemIconClass = FolderIconClass.superclass
        val FolderClass = HookUtils.getClass(
            "com.miui.home.launcher.Folder",
            classLoader
        ) ?: return
        val ItemInfoClass = HookUtils.getClass(
            "com.miui.home.launcher.ItemInfo",
            classLoader
        ) ?: return

        // 禁止修改图标
        XposedBridge.hookAllMethods(
            FolderIconClass,
            "onWallpaperColorChanged",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null;
                }
            })

        // 禁止使用软件加速 setLayerType
        XposedBridge.hookAllMethods(
            ThumbnailContainerClass,
            "onFinishInflate",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null;
                }
            })

        XposedBridge.hookAllMethods(
            FolderIconClass,
            "onFinishInflate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {

                    val mIconImageView = HookUtils.getValueByField(
                        param.thisObject,
                        "mIconImageView",
                        ItemIconClass
                    ) as ImageView

                    val mFolderBackgroundField =
                        FolderIconClass.getDeclaredField("mFolderBackground")
                    mFolderBackgroundField.isAccessible = true

                    mIconImageView.addOnAttachStateChangeListener(object :
                        View.OnAttachStateChangeListener {

                        override fun onViewAttachedToWindow(view: View) {
                            val blurDrawable =
                                HookUtils.createBlurDrawable(mIconImageView, 40, 38) ?: return
                            allBluredDrawable.add(blurDrawable)
                            mFolderBackgroundField.set(param.thisObject, blurDrawable)
                            val backgroundDrawable =
                                LayerDrawable(arrayOf(blurDrawable))
                            val paddingValue =
                                HookUtils.dip2px(mIconImageView.context, 2.5f).toInt()
                            backgroundDrawable.setLayerInsetTop(0, paddingValue)
                            backgroundDrawable.setLayerInsetRight(0, paddingValue)
                            backgroundDrawable.setLayerInsetBottom(0, paddingValue)
                            backgroundDrawable.setLayerInsetLeft(0, paddingValue)
                            mIconImageView.background = backgroundDrawable
                            if (mIconImageView.drawable == null) {
                                XposedHelpers.callMethod(
                                    blurDrawable,
                                    "setColor",
                                    Color.argb(60, 255, 255, 255)
                                )
                            }
                        }

                        override fun onViewDetachedFromWindow(view: View) {}
                    })

                    XposedBridge.hookAllMethods(
                        FolderClass,
                        "tellItemIconIsOnAnimation",
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val isStart = param.args[0] as Boolean
                                val mClosing =
                                    HookUtils.getValueByField(
                                        param.thisObject,
                                        "mClosing"
                                    ) as Boolean
                                if (isStart) {
                                    if (mClosing) {
                                        showBlurDrawable()
                                    } else {
                                        hideBlurDrawable()
                                    }
                                }
                            }
                        })
                }
            }
        )

        // 禁止使用软件加速 setLayerType
        XposedBridge.hookAllMethods(
            DragViewClass,
            "shouldShowDeleteHint",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = true
                }
            })

        // 为拖动时的图标添加背景
        XposedBridge.hookAllMethods(
            DragViewClass,
            "showWithAnim",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val dragView = param.thisObject as View
                    dragView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    val mDragInfo =
                        HookUtils.getValueByField(param.thisObject, "mDragInfo") ?: return

                    // 只有itemType为2是文件
                    val itemType =
                        HookUtils.getValueByField(mDragInfo, "itemType", ItemInfoClass) as Int
                    val mTitle =
                        HookUtils.getValueByField(mDragInfo, "mTitle", ItemInfoClass) as String

                    val mLauncher = HookUtils.getValueByField(param.thisObject, "mLauncher")

                    val isFolderShowing =
                        XposedHelpers.callMethod(mLauncher, "isFolderShowing") as Boolean

                    val mDragSource = XposedHelpers.callMethod(param.thisObject, "getDragSource")
                    val isFromHotSeats = mDragSource.javaClass.name.contains("HotSeats")

                    if (!isFolderShowing && (itemType == 2 || BLUR_ICON_APP_NAME.contains(mTitle))) {
                        val blurDrawable = HookUtils.createBlurDrawable(dragView, 38, 40) ?: return
                        allBluredDrawable.add(blurDrawable)
                        val backgroundDrawable =
                            LayerDrawable(arrayOf(blurDrawable))
                        backgroundDrawable.setLayerInsetTop(
                            0,
                            HookUtils.dip2px(dragView.context, 8f).toInt()
                        )
                        backgroundDrawable.setLayerInsetRight(
                            0,
                            HookUtils.dip2px(dragView.context, 18f).toInt()
                        )
                        if (isFromHotSeats) {
                            // 从底部拖动出来的时候没有文字，故缩小该区域
                            backgroundDrawable.setLayerInsetBottom(
                                0,
                                HookUtils.dip2px(dragView.context, 24f).toInt()
                            )
                            XposedHelpers.callMethod(
                                blurDrawable,
                                "setColor",
                                Color.argb(60, 255, 255, 255)
                            )
                        } else {
                            backgroundDrawable.setLayerInsetBottom(
                                0,
                                HookUtils.dip2px(dragView.context, 48f).toInt()
                            )
                        }
                        backgroundDrawable.setLayerInsetLeft(
                            0,
                            HookUtils.dip2px(dragView.context, 18f).toInt()
                        )
                        dragView.background = backgroundDrawable
                    }
                }
            })
    }

    // 与文件夹类似
    fun addBlurEffectToAlphaIcon() {
        val ShortcutIconClass = HookUtils.getClass(
            "com.miui.home.launcher.ShortcutIcon",
            classLoader
        ) ?: return
        val ItemIconClass = HookUtils.getClass(
            "com.miui.home.launcher.ItemIcon",
            classLoader
        ) ?: return

        XposedBridge.hookAllMethods(ShortcutIconClass, "drawOutLine", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = null
            }
        })

        XposedBridge.hookAllMethods(
            ItemIconClass,
            "setTitle",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val mTitle =
                        HookUtils.getValueByField(
                            param.thisObject,
                            "mTitle",
                            ItemIconClass
                        ) as TextView
                    val mTitleText = mTitle.text
                    val mIconImageView =
                        HookUtils.getValueByField(
                            param.thisObject,
                            "mIconImageView",
                            ItemIconClass
                        ) as ImageView

                    if (BLUR_ICON_APP_NAME.contains(mTitleText) && mIconImageView.background == null && mIconImageView.isAttachedToWindow) {
                        val blurDrawable =
                            HookUtils.createBlurDrawable(mIconImageView, 38, 40) ?: return
                        allBluredDrawable.add(blurDrawable)
                        val backgroundDrawable =
                            LayerDrawable(arrayOf(blurDrawable))
                        val paddingValue = HookUtils.dip2px(mIconImageView.context, 2.5f).toInt()
                        backgroundDrawable.setLayerInsetTop(0, paddingValue)
                        backgroundDrawable.setLayerInsetRight(0, paddingValue)
                        backgroundDrawable.setLayerInsetBottom(0, paddingValue)
                        backgroundDrawable.setLayerInsetLeft(0, paddingValue)
                        mIconImageView.background = backgroundDrawable
                    }
                }
            })
    }

    fun addBlurEffectToShortcutLayer() {
        val ShortcutMenuLayerClass = HookUtils.getClass(
            "com.miui.home.launcher.ShortcutMenuLayer",
            classLoader
        ) ?: return
        val ShortcutMenuClass = HookUtils.getClass(
            "com.miui.home.launcher.shortcuts.ShortcutMenu",
            classLoader
        ) ?: return
        val BlurUtilsClass = HookUtils.getClass(
            "com.miui.home.launcher.common.BlurUtils",
            classLoader
        ) ?: return
        val ApplicationClass = HookUtils.getClass(
            "com.miui.home.launcher.Application",
            classLoader
        ) ?: return
        val UtilitiesClass = HookUtils.getClass(
            "com.miui.home.launcher.common.Utilities",
            classLoader
        ) ?: return
        val DragViewClass = HookUtils.getClass(
            "com.miui.home.launcher.DragView",
            classLoader
        ) ?: return

        var isShortcutMenuLayerBlurred = false
        var targetView: ViewGroup? = null
        var dragView: View? = null
        var blurBackground = true

        XposedBridge.hookAllMethods(
            ShortcutMenuLayerClass,
            "showShortcutMenu",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val dragObject = param.args[0]
                    val dragViewInfo = XposedHelpers.callMethod(dragObject, "getDragInfo")
                    val iconIsInFolder =
                        XposedHelpers.callMethod(dragViewInfo, "isInFolder") as Boolean
                    if (iconIsInFolder) {
                        try {
                            val isUserBlurWhenOpenFolder = XposedHelpers.callStaticMethod(
                                BlurUtilsClass,
                                "isUserBlurWhenOpenFolder"
                            ) as Boolean
                            blurBackground = !isUserBlurWhenOpenFolder
                        } catch (e: Throwable) {
                            // Do Nothing.
                        }
                    } else {
                        blurBackground = true
                    }
                    val mLauncher = XposedHelpers.callStaticMethod(ApplicationClass, "getLauncher")
                    val systemUiController =
                        XposedHelpers.callMethod(mLauncher, "getSystemUiController")
                    val mWindow = HookUtils.getValueByField(systemUiController, "mWindow")
                    val targetBlurView = XposedHelpers.callMethod(mLauncher, "getScreen") as View

                    val renderEffectArray = arrayOfNulls<RenderEffect>(51)
                    for (index in 0..50) {
                        renderEffectArray[index] = RenderEffect.createBlurEffect(
                            (index + 1).toFloat(),
                            (index + 1).toFloat(),
                            Shader.TileMode.MIRROR
                        )
                    }

                    val valueAnimator = ValueAnimator.ofInt(0, 50)
                    valueAnimator.addUpdateListener { animator ->
                        val value = animator.animatedValue as Int
                        targetBlurView.setRenderEffect(renderEffectArray[value])
                        if (blurBackground) {
                            XposedHelpers.callStaticMethod(
                                BlurUtilsClass,
                                "fastBlurDirectly",
                                value / 50f,
                                mWindow
                            )
                        }
                    }
                    dragView =
                        XposedHelpers.callMethod(dragObject, "getDragView") as View
                    targetView = XposedHelpers.callMethod(dragView, "getContent") as ViewGroup
                    valueAnimator.duration = 200
                    valueAnimator.start()
                    hideBlurDrawable()
                    isShortcutMenuLayerBlurred = true
                }
            })

        XposedBridge.hookAllMethods(
            ShortcutMenuLayerClass,
            "onDragStart",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (isShortcutMenuLayerBlurred) {
                        if (targetView != null) {
                            targetView!!.transitionAlpha = 0f
                        }
                    }
                }
            })

        XposedBridge.hookAllMethods(
            ShortcutMenuLayerClass,
            "onDragEnd",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (isShortcutMenuLayerBlurred) {
                        val isLocked = XposedHelpers.callStaticMethod(
                            UtilitiesClass,
                            "isScreenCellsLocked"
                        ) as Boolean
                        if (isLocked && dragView != null) {
                            animate(dragView!!).scaleX(1f).scaleY(1f).setDuration(200).start()
                        }
                    }
                }
            })

        XposedBridge.hookAllMethods(DragViewClass, "remove", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (isShortcutMenuLayerBlurred) {
                    param.result = null
                }
            }
        })

        XposedBridge.hookAllMethods(
            ShortcutMenuClass,
            "reset",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (isShortcutMenuLayerBlurred) {
                        isShortcutMenuLayerBlurred = false
                        if (targetView != null) {
                            targetView!!.transitionAlpha = 1f
                        }
                        val mLauncher =
                            XposedHelpers.callStaticMethod(ApplicationClass, "getLauncher")
                        val systemUiController =
                            XposedHelpers.callMethod(mLauncher, "getSystemUiController")
                        val mWindow = HookUtils.getValueByField(systemUiController, "mWindow")
                        XposedHelpers.callStaticMethod(
                            BlurUtilsClass,
                            "fastBlurDirectly",
                            0f,
                            mWindow
                        )
                    }
                }
            })

        XposedBridge.hookAllMethods(
            ShortcutMenuLayerClass,
            "hideShortcutMenu",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (isShortcutMenuLayerBlurred) {
                        val editStateChangeReason = param.args[0]
                        val shortcutMenuLayer = param.thisObject as FrameLayout
                        val mLauncher =
                            XposedHelpers.callStaticMethod(ApplicationClass, "getLauncher")
                        val systemUiController =
                            XposedHelpers.callMethod(mLauncher, "getSystemUiController")
                        val mWindow = HookUtils.getValueByField(systemUiController, "mWindow")

                        val targetBlurView =
                            XposedHelpers.callMethod(mLauncher, "getScreen") as View

                        val valueAnimator = ValueAnimator.ofInt(50, 0)
                        val renderEffectArray = arrayOfNulls<RenderEffect>(51)
                        for (index in 0..50) {
                            renderEffectArray[index] = RenderEffect.createBlurEffect(
                                (index + 1).toFloat(),
                                (index + 1).toFloat(),
                                Shader.TileMode.MIRROR
                            )
                        }
                        valueAnimator.addUpdateListener { animator ->
                            val value = animator.animatedValue as Int
                            targetBlurView.setRenderEffect(renderEffectArray[value])
                            if (blurBackground) {
                                XposedHelpers.callStaticMethod(
                                    BlurUtilsClass,
                                    "fastBlurDirectly",
                                    value / 50f,
                                    mWindow
                                )
                            }
                        }
                        valueAnimator.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                shortcutMenuLayer.background = null
                                showBlurDrawable()
                                targetView!!.transitionAlpha = 1f
                                targetBlurView.setRenderEffect(null)
                                isShortcutMenuLayerBlurred = false
                                if (editStateChangeReason != null && editStateChangeReason.toString() != "drag_over_threshold") {
                                    XposedHelpers.callMethod(dragView, "remove")
                                }
                            }
                        })
                        valueAnimator.duration = 200
                        valueAnimator.start()

                        if (editStateChangeReason != null) {
                            HookUtils.log(editStateChangeReason)
                        } else {
                            isShortcutMenuLayerBlurred = false
                            XposedHelpers.callMethod(dragView, "remove")
                        }
                    }
                }
            })

        XposedBridge.hookAllMethods(
            BlurUtilsClass,
            "fastBlurDirectly",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val blurRatio = param.args[0] as Float
                    if (isShortcutMenuLayerBlurred && blurRatio == 0.0f) {
                        param.result = null
                    }
                }
            })


        if (shortcutMenuBackgroundAlpha != 255) {
            XposedBridge.hookAllMethods(
                ShortcutMenuClass,
                "setMenuBg",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isShortcutMenuLayerBlurred) {
                            return
                        }

                        val mAppShortcutMenu = HookUtils.getValueByField(
                            param.thisObject,
                            "mAppShortcutMenu"
                        ) as ViewGroup
                        val mAppShortcutMenuBackground =
                            mAppShortcutMenu.background as GradientDrawable
                        mAppShortcutMenuBackground.alpha = singleLayerAlpha
                        val mSystemShortcutMenu = HookUtils.getValueByField(
                            param.thisObject,
                            "mSystemShortcutMenu"
                        ) as ViewGroup
                        val mSystemShortcutMenuBackground =
                            mSystemShortcutMenu.background as GradientDrawable
                        mSystemShortcutMenuBackground.alpha = singleLayerAlpha

                        val mWidgetShortcutMenu = HookUtils.getValueByField(
                            param.thisObject,
                            "mWidgetShortcutMenu"
                        ) as ViewGroup
                        val mWidgetShortcutMenuBackground =
                            mWidgetShortcutMenu.background as GradientDrawable
                        mWidgetShortcutMenuBackground.alpha = singleLayerAlpha

                        for (index in 0..mAppShortcutMenu.childCount) {
                            val child = mAppShortcutMenu.getChildAt(index)
                            if (child != null && child.background != null) {
                                if (child.background is Drawable) {
                                    val childBackground = child.background as Drawable
                                    childBackground.alpha = singleLayerAlpha
                                }
                            }
                        }

                        for (index in 0..mSystemShortcutMenu.childCount) {
                            val child = mSystemShortcutMenu.getChildAt(index)
                            if (child != null && child.background != null) {
                                if (child.background is Drawable) {
                                    val childBackground = child.background as Drawable
                                    childBackground.alpha = singleLayerAlpha
                                }
                            }
                        }

                        for (index in 0..mWidgetShortcutMenu.childCount) {
                            val child = mWidgetShortcutMenu.getChildAt(index)
                            if (child != null && child.background != null) {
                                if (child.background is Drawable) {
                                    val childBackground = child.background as Drawable
                                    childBackground.alpha = singleLayerAlpha
                                }
                            }
                        }
                    }
                })
            XposedBridge.hookAllMethods(
                ShortcutMenuClass,
                "addArrow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (!isShortcutMenuLayerBlurred) {
                            return
                        }
                        val mArrow = HookUtils.getValueByField(
                            param.thisObject,
                            "mArrow"
                        ) as View
                        val mArrowBackground = mArrow.background as ShapeDrawable
                        mArrowBackground.alpha = shortcutMenuBackgroundAlpha
                    }
                })
        }
    }

    fun hideBlurIconWhenEnterRecents() {
        val BlurUtilsClass = HookUtils.getClass(
            "com.miui.home.launcher.common.BlurUtils",
            classLoader
        ) ?: return
        XposedBridge.hookAllMethods(
            BlurUtilsClass,
            "fastBlurWhenEnterRecents",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val launcher = param.args[0]
                    if (launcher != null) {
                        XposedHelpers.callMethod(launcher, "hideShortcutMenuWithoutAnim")
                    }
                    hideBlurDrawable()
                }
            })
        XposedBridge.hookAllMethods(
            BlurUtilsClass,
            "fastBlurWhenExitRecents",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    showBlurDrawable()
                }
            })
    }

    fun showBlurDrawable() {
        allBluredDrawable.forEach { drawable ->
            XposedHelpers.callMethod(drawable, "setVisible", true, false)
        }
    }

    fun hideBlurDrawable() {
        allBluredDrawable.forEach { drawable ->
            XposedHelpers.callMethod(drawable, "setVisible", false, false)
        }
    }

}