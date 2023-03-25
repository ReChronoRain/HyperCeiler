package com.sevtinge.cemiuiler.utils.wini

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

object HookUtils {
    fun log(content: Any?) {
        XposedBridge.log("[WINI]" + content)
    }

    fun dip2px(context: Context, dpValue: Float): Float {
        val scale = context.resources.displayMetrics.density
        return dpValue * scale + 0.5f
    }

    fun getClass(className: String, classLoader: ClassLoader): Class<*>? {
        val result = XposedHelpers.findClassIfExists(
            className,
            classLoader
        )
        if (result == null) {
            log("'" + className + "' is NOT found.")
        }
        return result
    }

    fun replaceMethodResult(
        className: String,
        classLoader: ClassLoader,
        methodName: String,
        result: Any,
        vararg args: Any?
    ) {
        try {
            XposedHelpers.findAndHookMethod(
                className,
                classLoader,
                methodName,
                *args,
                XC_MethodReplacement.returnConstant(result)
            )
        } catch (e: Throwable) {
            log(e.message)
        }
    }

    fun getValueByField(target: Any, fieldName: String, clazz: Class<*>? = null): Any? {
        var targetClass = clazz
        if (targetClass == null) {
            targetClass = target.javaClass
        }
        return try {
            val field = targetClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.get(target)
        } catch (e: Throwable) {
            if (targetClass.superclass == null) {
                null
            } else {
                getValueByField(target, fieldName, targetClass.superclass)
            }
        }
    }

    fun createBlurDrawable(
        view: View,
        blurRadius: Int,
        cornerRadius: Int,
        color: Int? = null
    ): Drawable? {
        try {
            val mViewRootImpl = XposedHelpers.callMethod(
                view,
                "getViewRootImpl"
            ) ?: return null
            val blurDrawable = XposedHelpers.callMethod(
                mViewRootImpl,
                "createBackgroundBlurDrawable"
            ) as Drawable
            XposedHelpers.callMethod(blurDrawable, "setBlurRadius", blurRadius)
            XposedHelpers.callMethod(blurDrawable, "setCornerRadius", cornerRadius)
            if (color != null) {
                XposedHelpers.callMethod(
                    blurDrawable,
                    "setColor",
                    color
                )
            }
            return blurDrawable
        } catch (e: Throwable) {
            log("Create BlurDrawable Error:" + e)
            return null
        }
    }

    fun isBlurDrawable(drawable: Drawable?): Boolean {
        // 不够严谨，可以用
        if (drawable == null) {
            return false
        }
        val drawableClassName = drawable.javaClass.name
        return drawableClassName.contains("BackgroundBlurDrawable")
    }
}