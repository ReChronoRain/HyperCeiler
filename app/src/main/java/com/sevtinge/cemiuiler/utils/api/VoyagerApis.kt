package com.sevtinge.cemiuiler.utils.api

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.utils.isStatic
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Field
import java.lang.reflect.Method

@JvmInline
value class Args(val args: Array<out Any?>)

@JvmInline
value class ArgTypes(val argTypes: Array<out Class<*>>)

@Suppress("NOTHING_TO_INLINE")
inline fun args(vararg args: Any?) = Args(args)

@Suppress("NOTHING_TO_INLINE")
inline fun argTypes(vararg argTypes: Class<*>) = ArgTypes(argTypes)

typealias MethodCondition = Method.() -> Boolean

/**
 * 扩展函数 通过类或者对象获取单个属性
 * @param fieldName 属性名
 * @param isStatic 是否静态类型
 * @param fieldType 属性类型
 * @return 符合条件的属性
 * @throws IllegalArgumentException 属性名为空
 * @throws NoSuchFieldException 未找到属性
 */
fun Any.field(
    fieldName: String,
    isStatic: Boolean = false,
    fieldType: Class<*>? = null
): Field {
    if (fieldName.isBlank()) throw IllegalArgumentException("Field name must not be empty!")
    var c: Class<*> = if (this is Class<*>) this else this.javaClass
    do {
        c.declaredFields
            .filter { isStatic == it.isStatic }
            .firstOrNull { (fieldType == null || it.type == fieldType) && (it.name == fieldName) }
            ?.let { it.isAccessible = true;return it }
    } while (c.superclass?.also { c = it } != null)
    throw NoSuchFieldException("Name: $fieldName,Static: $isStatic, Type: ${if (fieldType == null) "ignore" else fieldType.name}")
}

/**
 * 扩展函数 调用对象中符合条件的方法
 * @param args 参数
 * @param condition 条件
 * @return 方法的返回值
 * @throws NoSuchMethodException 未找到方法
 */
fun Any.invokeMethod(vararg args: Any?, condition: MethodCondition): Any? {
    this::class.java.declaredMethods.firstOrNull { it.condition() }
        ?.let { it.isAccessible = true;return it(this, *args) }
    throw NoSuchMethodException()
}

/**
 * 判断运行模块的机型是否是平板
 * @return 一个 Boolean 值，true 代表是平板，false 代表不是平板
 * @author Voyager
 */
fun isPad() =
    loadClass("miui.os.Build")
        .getField("IS_TABLET")
        .getBoolean(null)

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
        Log.e("Create BlurDrawable Error:$e")
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

/**
 * 扩展函数 通过遍历方法数组 返回符合条件的方法数组
 * @param condition 条件
 * @return 符合条件的方法数组
 */
fun Array<Method>.findAllMethods(condition: MethodCondition): Array<Method> {
    return this.filter { it.condition() }.onEach { it.isAccessible = true }.toTypedArray()
}

/**
 * 通过条件获取方法数组
 * @param clz 类
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的方法数组
 */
fun findAllMethods(
    clz: Class<*>,
    findSuper: Boolean = false,
    condition: MethodCondition
): List<Method> {
    var c = clz
    val arr = ArrayList<Method>()
    arr.addAll(c.declaredMethods.findAllMethods(condition))
    if (findSuper) {
        while (c.superclass?.also { c = it } != null) {
            arr.addAll(c.declaredMethods.findAllMethods(condition))
        }
    }
    return arr
}

/**
 * 通过条件获取方法数组
 * @param clzName 类名
 * @param classLoader 类加载器
 * @param findSuper 是否查找父类
 * @param condition 条件
 * @return 符合条件的方法数组
 */
fun findAllMethods(
    clzName: String,
    classLoader: ClassLoader = EzXHelper.classLoader,
    findSuper: Boolean = false,
    condition: MethodCondition
): List<Method> {
    return findAllMethods(loadClass(clzName, classLoader), findSuper, condition)
}

fun dp2px(context: Context, dpValue: Float): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    dpValue,
    context.resources.displayMetrics
).toInt()

/**
 * 可以从 SystemUI 里获取 SystemUIPlugin 的 AppInfo 和 ClassLoader
 *
 * 使用方法:
 *
 *     hookPluginClassLoader { appInfo, classLoader ->
 *         // 在这里使用 appInfo 和 classLoader 变量
 *     }
 *
 * 仅适用于 Mix4 A13 Miui14 V14.0.23.5.22.Dev 开发版
 * @author Voyager
 * @return appInfo & classLoaderP
 */
fun hookPluginClassLoader(onGetClassLoader: (appInfo: ApplicationInfo, classLoader: ClassLoader) -> Unit) {
    val classLoaderClass = loadClass("com.android.systemui.shared.plugins.PluginInstance\$Factory")
    classLoaderClass.methodFinder().first {
        name == "getClassLoader"
            && parameterCount == 2
            && parameterTypes[0] == ApplicationInfo::class.java
            && parameterTypes[1] == ClassLoader::class.java
    }.createHook {
        after { getClassLoader ->
            val appInfo = getClassLoader.args[0] as ApplicationInfo
            val classLoaderP = getClassLoader.result as ClassLoader
//            Log.i("get classLoader: $appInfo $classLoaderP")
            if (appInfo.packageName == "miui.systemui.plugin") {
                onGetClassLoader(appInfo, classLoaderP)
            }
        }
    }
}
