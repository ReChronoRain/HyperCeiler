package com.sevtinge.hyperceiler.libhook.rules.systemui.navigation

import android.content.Context
import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import android.view.Surface
import android.view.View
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.function.Supplier

/**
 * Android 16+（SDK >= 36）分支的旋转建议按钮规则。
 *
 * 本加载器（SystemUIB，minSdk = 36）要同时覆盖 Android 16（OS3）与 Android 17（OS4），
 * 两个世代的 ROM 又各自被 R8 反复脱糖/内联，所以这里不再钉死任何一类"版本特征"：
 *  - 不写脱糖 lambda 的编号（NavigationBarView 的方向 Supplier 在 OS3 上是
 *    $$ExternalSyntheticLambda1，在 OS4 的 44/405 上漂移到了 $$ExternalSyntheticLambda0，
 *    编号完全不可靠；硬编码 1 曾导致 OS4 上 findMethod("get") 落空、整个 init 中断）；
 *  - 不按 Android / ROM 版本号挑选挂钩目标；
 *  - 只按编译器保证的结构特征匹配：函数式接口 + 方法名与参数表 + 捕获字段类型 = 宿主类。
 *
 * 上述三条不变量已在 OS 3.0.319.0（Android 16）、OS 4.0.0.44 与 OS 4.0.5.0（Android 17）
 * 三份固件的 dexdump 上逐一核验，均命中且各自唯一。
 * 另外每个挂钩点独立注册、各自兜底：某一点在本版本缺失时只跳过它自己，
 * 绝不连带同一规则里的其余挂钩一起失败。
 */
object RotationButtonB : BaseHook() {

    private const val STATE_NAVIGATION_BAR = "RotationButtonB.navigationBar"
    private const val STATE_CONTEXT = "RotationButtonB.context"

    /** NavigationBar / NavigationBarView 在挪包重构前后各有一个包名，都试，与版本号无关 */
    private val NAV_BAR_CANDIDATES = arrayOf(
        "com.android.systemui.navigationbar.views.NavigationBar",
        "com.android.systemui.navigationbar.NavigationBar",
    )
    private val NAV_BAR_VIEW_CANDIDATES = arrayOf(
        "com.android.systemui.navigationbar.views.NavigationBarView",
        "com.android.systemui.navigationbar.NavigationBarView",
    )
    private const val ROTATION_CONTROLLER =
        "com.android.systemui.shared.rotation.RotationButtonController"
    /** 脱糖类的编号上界：只用来限制穷举范围，不作为任何版本判据 */
    private const val MAX_LAMBDA_INDEX = 64
    /** 编译期脱糖类的统一前缀；编号会随版本漂移，前缀本身是编译器写死的 */
    private const val DESUGARED_PREFIX = "\$\$ExternalSyntheticLambda"

    var isListen = false

    private val enable: Boolean
        get() = PrefsBridge.getStringAsInt("system_framework_other_rotation_button_int", 0) != 1

    override fun init() {
        if (enable) {
            val restoredNavigationBar = getHotReloadRuntimeState(
                STATE_NAVIGATION_BAR,
                Any::class.java
            )
            val restoredContext = getHotReloadRuntimeState(STATE_CONTEXT, Context::class.java)
            if (restoredNavigationBar != null && restoredContext != null) {
                ensureRotationObserver(restoredNavigationBar, restoredContext)
            }
        }

        val navigationBarClass = findFirstClass(*NAV_BAR_CANDIDATES)
        if (navigationBarClass != null) {
            hookAllConstructors(navigationBarClass, object : IMethodHook {
                override fun after(param: HookParam) {
                    if (!enable) return
                    ensureRotationObserver(param.thisObject, resolveContext(param.thisObject))
                }
            })
        }

        hookOrientationSupplier()

        // 显式参数表 (int, boolean)：三个基线上 NavigationBar 都只有这一个重载，
        // 比只按方法名匹配更稳（也避免 DSL 在缺失时把异常抛进 init 主流程）。
        tryHook("NavigationBar#onRotationProposal") {
            val clazz = navigationBarClass ?: error("NavigationBar 不存在")
            findAndHookMethod(
                clazz, "onRotationProposal",
                Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
                object : IMethodHook {
                    override fun before(param: HookParam) {
                        if (!enable) {
                            param.result = null
                        }
                    }
                }
            )
        }

        hookRotateSuggestionClick()
    }

    /**
     * 方向建议按钮的取值来自一个 lambda。编译器可能把它内联回宿主（形如 lambda$xxx，
     * 无参、返回 int/Integer/Object），也可能脱糖成独立的 $$ExternalSyntheticLambdaN 类；
     * 两种形态的编号都会随版本漂移，所以都只按结构特征去找。
     */
    private fun hookOrientationSupplier() {
        for (candidate in NAV_BAR_VIEW_CANDIDATES) {
            val viewClass = findClassIfExists(candidate) ?: continue

            // 形态一：lambda 被内联回宿主类
            var hooked = false
            for (method in viewClass.declaredMethods) {
                val returnType = method.returnType
                if (method.parameterTypes.isEmpty()
                    && (returnType == Int::class.javaPrimitiveType
                        || returnType == Integer::class.java
                        || returnType == Any::class.java)
                    // 不猜编号：名字带 lambda$ 前缀才算内联 lambda，避免误挂普通 getter
                    && method.name.startsWith("lambda$")
                ) {
                    hookMethod(method, object : IMethodHook {
                        override fun before(param: HookParam) {
                            forceOrientation(param, param.thisObject)
                        }
                    })
                    hooked = true
                }
            }
            if (hooked) {
                return
            }

            // 形态二：脱糖成独立类。三条结构特征同时成立才算命中：
            //   ① 实现 java.util.function.Supplier（接口由编译器写死，最稳）
            //   ② 无参 get()
            //   ③ 捕获字段类型 = 宿主类本身
            // 44/405 上命中 Lambda0，OS3 上命中 Lambda1，均为唯一命中；
            // 三者的 get() 字节码同为 return Integer.valueOf(mCurrentRotation)，语义一致。
            val supplier = findDesugaredLambda(
                viewClass, Supplier::class.java, "get", emptyArray()
            ) ?: continue
            hookMethod(supplier.method, object : IMethodHook {
                override fun before(param: HookParam) {
                    if (!enable) return
                    val owner = getObjectField(param.thisObject, supplier.capturedField)
                        ?: return
                    forceOrientation(param, owner)
                }
            })
            return
        }
    }

    /** 把 Supplier 提供的当前方向改写为"翻转后的方向"（保持与原实现相同的取值映射） */
    private fun forceOrientation(param: HookParam, owner: Any) {
        if (!enable) return
        val lightContext = resolveContext(owner, "mLightContext") ?: return
        val intValue = when (getScreenOrientation(lightContext)) {
            0 -> 1
            1 -> 0
            else -> -1
        }
        if (intValue == -1) {
            XposedLog.e(TAG, lpparam.packageName, "Unknown parameters, unable to continue execution, execute the original method!")
            return
        }
        // setResult 会替换被挂方法(get/内联 lambda)的返回值
        param.result = intValue
    }

    /**
     * 旋转建议按钮的点击回调：先按方法名找宿主类里的真实方法（AOSP 曾改名
     * onRotateSuggestionClick -> onRotateSuggestionClicked，前缀匹配都认），
     * 找不到再退回脱糖出来的 onClick(View)。两边都不写死编号；
     * 44/405/OS3 三份固件上 R8 都已把点击 lambda 内联，真实目标是
     * RotationButtonController$$ExternalSyntheticLambda5（编号只是巧合一致，
     * 由结构特征保证换版本后仍能找到）。
     */
    private fun hookRotateSuggestionClick() {
        val controllerClass = findClassIfExists(ROTATION_CONTROLLER) ?: return

        var hooked = false
        for (method in controllerClass.declaredMethods) {
            if (method.name.startsWith("onRotateSuggestionClick")
                && method.parameterTypes.isNotEmpty()
                && method.parameterTypes[0] == View::class.java
            ) {
                hookMethod(method, object : IMethodHook {
                    override fun after(param: HookParam) {
                        if (enable) {
                            showRotateSuggestion(param.thisObject)
                        }
                    }
                })
                hooked = true
            }
        }
        if (hooked) {
            return
        }

        val click = findDesugaredLambda(
            controllerClass, View.OnClickListener::class.java, "onClick",
            arrayOf(View::class.java)
        )
        if (click == null) {
            XposedLog.w(TAG, "未定位到旋转建议的点击回调（内联形态与脱糖形态都没匹配上）")
            return
        }
        hookMethod(click.method, object : IMethodHook {
            override fun before(param: HookParam) {
                if (!enable) return
                val controller = getObjectField(param.thisObject, click.capturedField)
                    ?: return
                showRotateSuggestion(controller)
            }
        })
    }

    /**
     * 点完之后把建议按钮状态置为 (true, false)。三个基线上该重载都是 (boolean, boolean)；
     * 万一哪个版本只剩别的重载，退回单参再试，两种都没有才记错误，绝不抛出去。
     */
    private fun showRotateSuggestion(controller: Any) {
        try {
            BaseHook.callMethod(controller, "setRotateSuggestionButtonState", true, false)
        } catch (ignored: Throwable) {
            try {
                BaseHook.callMethod(controller, "setRotateSuggestionButtonState", true)
            } catch (alsoIgnored: Throwable) {
                XposedLog.e(TAG, "setRotateSuggestionButtonState 两种重载都没找到")
            }
        }
    }

    /** 一次结构匹配的产物：脱糖类本身、要挂的方法、以及持有宿主实例的捕获字段名 */
    private class DesugaredLambda(
        val clazz: Class<*>,
        val method: Method,
        val capturedField: String
    )

    /**
     * 穷举宿主类的脱糖类，按"函数式接口 + 方法名与参数表 + 捕获字段类型 = 宿主类"
     * 三条结构特征找目标。编号只是遍历边界，任何版本都不会因为编号变化而失效。
     * 命中多于一个时只取编号最小的那个，并把数量记进日志，便于换版本后复核。
     */
    private fun findDesugaredLambda(
        ownerClass: Class<*>,
        functionalInterface: Class<*>?,
        methodName: String,
        paramTypes: Array<Class<*>>,
    ): DesugaredLambda? {
        val classLoader = ownerClass.classLoader ?: return null
        val ownerName = ownerClass.name
        var matched: DesugaredLambda? = null
        var hits = 0
        for (index in 0 until MAX_LAMBDA_INDEX) {
            val lambdaClass = try {
                Class.forName(ownerName + DESUGARED_PREFIX + index, false, classLoader)
            } catch (ignored: Throwable) {
                continue
            }
            if (functionalInterface != null && !functionalInterface.isAssignableFrom(lambdaClass)) {
                continue
            }
            val target = lambdaClass.declaredMethods.firstOrNull { method ->
                method.name == methodName && method.parameterTypes.contentEquals(paramTypes)
            } ?: continue
            // 捕获字段不保证叫 f$0，按类型找出持有宿主实例的那个
            val capturedFieldName = findCapturedField(lambdaClass, ownerClass) ?: continue
            hits++
            if (matched == null) {
                matched = DesugaredLambda(lambdaClass, target, capturedFieldName)
            }
        }
        val m = matched
        if (m != null && hits > 1) {
            XposedLog.w(
                TAG,
                "结构匹配到 $hits 个脱糖类，取用 ${m.clazz.name}（请复核这个版本上是否真有多个同形 lambda）"
            )
        }
        return matched
    }

    /** 找出脱糖类里持有宿主实例的那个字段名（f$0、f$1 都可能是） */
    private fun findCapturedField(lambdaClass: Class<*>, ownerClass: Class<*>): String? {
        for (field in lambdaClass.declaredFields) {
            if (field.type == ownerClass) {
                return field.name
            }
        }
        return null
    }

    /**
     * 注册单个挂钩点：这个版本上没有目标时只记一条日志并跳过它，
     * 不打断同一规则里的其它挂钩。
     */
    private inline fun tryHook(what: String, register: () -> Unit) {
        try {
            register()
        } catch (e: Throwable) {
            XposedLog.w(TAG, "跳过挂钩 $what：${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /**
     * 按"类型"而不是"字段名"拿 Context：名字改成什么都不影响。
     * 先试 prefer 里的名字（语义更准，例如主题化的 mLightContext），
     * 再按声明顺序扫实例字段找第一个 Context。都拿不到就返回 null，由调用方跳过本次改写。
     */
    private fun resolveContext(host: Any?, vararg prefer: String): Context? {
        host ?: return null
        for (name in prefer) {
            try {
                val value = getObjectField(host, name)
                if (value is Context) {
                    return value
                }
            } catch (ignored: Throwable) {
                // 字段不存在时反射层会抛异常，按"没有这个字段"处理
            }
        }
        var clazz: Class<*>? = host.javaClass
        while (clazz != null && clazz != Any::class.java) {
            for (field in clazz.declaredFields) {
                if (Modifier.isStatic(field.modifiers)
                    || !Context::class.java.isAssignableFrom(field.type)
                ) {
                    continue
                }
                try {
                    field.isAccessible = true
                    val value = field.get(host)
                    if (value is Context) {
                        return value
                    }
                } catch (ignored: Throwable) {
                }
            }
            clazz = clazz.superclass
        }
        return null
    }

    private fun findFirstClass(vararg candidateNames: String): Class<*>? {
        for (name in candidateNames) {
            val clazz = findClassIfExists(name)
            if (clazz != null) {
                return clazz
            }
        }
        return null
    }

    private fun ensureRotationObserver(navigationBar: Any, context: Context?) {
        if (isListen) return
        if (context == null) {
            XposedLog.e(TAG, lpparam.packageName, "context can't is null!")
            return
        }
        val contentObserver: ContentObserver =
            object : ContentObserver(Handler(context.mainLooper)) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    val isShow = getBoolean(context)
                    val rotation = getInt(context)
                    navigationBar.callMethod("onRotationProposal", rotation, isShow)
                }
            }

        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor("rotation_button_data"), false, contentObserver
        )
        isListen = true
        BaseHook.registerContentObserverHotReloadCleanup(context.contentResolver, contentObserver)
        BaseHook.putHotReloadRuntimeState(STATE_NAVIGATION_BAR, navigationBar)
        BaseHook.putHotReloadRuntimeState(STATE_CONTEXT, context)
    }

    fun getScreenOrientation(context: Context): Int {
        val display = context.display
        val rotation = display.rotation
        // 获取屏幕方向
        return when (rotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            Surface.ROTATION_90, Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private fun getData(context: Context): String? {
        return Settings.System.getString(context.contentResolver, "rotation_button_data")
    }

    private fun getBoolean(context: Context): Boolean {
        val data = getData(context) ?: return false
        val sp = data.split(",")
        return sp.getOrNull(1).orEmpty().contains("true")
    }

    private fun getInt(context: Context): Int {
        val data = getData(context) ?: return -1
        val sp = data.split(",")
        return sp.getOrNull(0)?.trim()?.toIntOrNull() ?: -1
    }
}
