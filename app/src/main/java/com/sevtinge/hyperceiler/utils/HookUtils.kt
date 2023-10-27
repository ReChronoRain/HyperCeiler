package com.sevtinge.hyperceiler.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logW
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers

object HookUtils {

    fun dip2px(context: Context, dpValue: Float): Float {
        val scale = context.resources.displayMetrics.density
        return dpValue * scale + 0.5f
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
            logW("replaceMethodResult", e)
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

    @RequiresApi(Build.VERSION_CODES.S)
    fun createBlurDrawable(
        view: View,
        blurRadius: Int,
        cornerRadius: Int,
        color: Int? = null
    ): Drawable? {
        try {
            val mViewRootImpl =
                XposedHelpers.callMethod(view, "getViewRootImpl") ?: return null
            val blurDrawable =
                XposedHelpers.callMethod(mViewRootImpl, "createBackgroundBlurDrawable") as Drawable
            XposedHelpers.callMethod(blurDrawable, "setBlurRadius", blurRadius)
            XposedHelpers.callMethod(blurDrawable, "setCornerRadius", cornerRadius)
            if (color != null) {
                XposedHelpers.callMethod(blurDrawable, "setColor",color)
            }
            return blurDrawable
        } catch (e: Throwable) {
            logW("createBlurDrawable", "Create BlurDrawable Error", e)
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
