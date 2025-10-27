package com.sevtinge.hyperceiler.hook.module.rules.home.gesture

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Message
import android.view.View
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.callStaticMethod
import com.sevtinge.hyperceiler.hook.utils.findClass
import com.sevtinge.hyperceiler.hook.utils.findClassOrNull
import com.sevtinge.hyperceiler.hook.utils.getIntField
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.getStaticObjectField
import com.sevtinge.hyperceiler.hook.utils.hookBeforeMethod
import com.sevtinge.hyperceiler.hook.utils.isStatic
import com.sevtinge.hyperceiler.hook.utils.replaceMethod
import com.sevtinge.hyperceiler.hook.utils.setObjectField
import de.robv.android.xposed.XposedHelpers
import io.github.kyuubiran.ezxhelper.core.extension.MemberExtension.paramCount
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import kotlin.math.abs

object QuickBackNew : BaseHook() {

    private val gestureBackArrowViewClass by lazy {
        "com.miui.home.recents.GestureBackArrowView".findClass()
    }
    private val gestureStubViewClass by lazy {
        "com.miui.home.recents.GestureStubView".findClass()
    }
    private val recentsModelClass by lazy {
        "com.miui.home.recents.RecentsModel".findClass()
    }
    private val activityManagerWrapperClass by lazy {
        "com.android.systemui.shared.recents.system.ActivityManagerWrapper".findClass()
    }
    private val activityManagerClass by lazy {
        "android.app.ActivityManagerNative".findClass()
    }
    private val readyStateEnumClass by lazy {
        $$"com.miui.home.recents.GestureBackArrowView$ReadyState".findClass()
    }
    private val getNextTaskMethod by lazy {
        gestureStubViewClass.methodFinder()
            .filterByParamTypes(Context::class.java, Boolean::class.java, Int::class.java)
            .first { name == "getNextTask" && isStatic }
    }

    private val setLaunchWindowingModeMethod by lazy {
        ActivityOptions::class.java.methodFinder()
            .first {
                name == "setLaunchWindowingMode" && parameterTypes[0] == Int::class.java
            }
    }
    private val getHapticInstanceMethod by lazy {
        "com.miui.home.launcher.common.HapticFeedbackCompat".findClassOrNull()?.methodFinder()
            ?.first { name == "getInstance" && isStatic }
    }
    private val performGestureBackHandUpMethod by lazy {
        "com.miui.home.launcher.common.HapticFeedbackCompatV2".findClassOrNull()?.methodFinder()
            ?.first { name == "performGestureBackHandUp" && paramCount == 0 }
    }
    private val readyStateRecent by lazy {
        "com.miui.home.recents.GestureBackArrowView\$ReadyState".findClass().enumConstants?.get(2)
    }

    override fun init() {
        "com.miui.home.recents.GestureStubView$3".findClass().methodFinder()
            .filterByName("onSwipeStop").first().hookBeforeMethod {
                /*if (it.thisObject as Boolean) {*/
                if (it.args[0] as Boolean) {
                    val mGestureStubView = it.thisObject.getObjectField("this$0")
                    val mGestureBackArrowView =
                        mGestureStubView?.getObjectField("mGestureBackArrowView")
                    val getCurrentState = mGestureBackArrowView?.callMethod("getCurrentState")
                    if (getCurrentState == readyStateRecent) {
                        XposedHelpers.callMethod(mGestureStubView, "onBackCancelled")
                        performGestureBackHandUpMethod?.invoke(getHapticInstanceMethod)
                    }
                }
            }

        $$"com.miui.home.recents.GestureStubView$H".findClass().methodFinder()
            .filterByName("handleMessage").first().hookBeforeMethod { param ->
                val msg = param.args[0] as Message
                val what = msg.what

                if (what != 259 && what != 261) return@hookBeforeMethod

                val stubViewInstance = param.thisObject.getObjectField("this$0")

                if (what == 259) {
                    // GestureStubView.access$2700(this.this$0)
                    gestureStubViewClass.callStaticMethod("access$2700", stubViewInstance)
                    param.result = null
                    return@hookBeforeMethod
                }

                // what == 261
                gestureStubViewClass.callStaticMethod("access$3000", stubViewInstance)

                val arrowViewInstance = gestureStubViewClass.callStaticMethod("access$100", stubViewInstance)
                val readyStateBack = readyStateEnumClass.getStaticObjectField("READY_STATE_BACK")
                val readyStateRecent = readyStateEnumClass.getStaticObjectField("READY_STATE_RECENT")

                val isActive1400 = gestureStubViewClass.callStaticMethod("access$1400", stubViewInstance) as Boolean
                val isActive1300 = gestureStubViewClass.callStaticMethod("access$1300", stubViewInstance, 20) as Boolean

                if (isActive1400) {
                    if (isActive1300) {
                        val posStart = gestureStubViewClass.callStaticMethod("access$800", stubViewInstance) as Float
                        val posEnd = gestureStubViewClass.callStaticMethod("access$1200", stubViewInstance) as Float
                        val threshold = gestureStubViewClass.callStaticMethod("access$3102", stubViewInstance) as Float
                        val diff = abs(posStart - posEnd)
                        if (diff > threshold * 0.33f) {
                            arrowViewInstance?.callMethod("setReadyFinish", readyStateRecent)
                        } else {
                            arrowViewInstance?.callMethod("setReadyFinish", readyStateBack)
                        }
                    } else {
                        val currentState = arrowViewInstance?.callMethod("getCurrentState")
                        if (currentState != readyStateRecent) {
                            arrowViewInstance?.callMethod("setReadyFinish", readyStateBack)
                        }
                    }
                } else if (isActive1300) {
                    val posStart = gestureStubViewClass.callStaticMethod("access$800", stubViewInstance) as Float
                    val posEnd = gestureStubViewClass.callStaticMethod("access$1200", stubViewInstance) as Float
                    val threshold = gestureStubViewClass.callStaticMethod("access$3102", stubViewInstance) as Float
                    val diff = abs(posStart - posEnd)
                    if (diff < threshold * 0.33f) {
                        arrowViewInstance?.callMethod("setReadyFinish", readyStateBack)
                    } else {
                        arrowViewInstance?.callMethod("setReadyFinish", readyStateRecent)
                    }
                }

                val handler = gestureStubViewClass.callStaticMethod("access$200", stubViewInstance) as Handler
                handler.sendEmptyMessageDelayed(261, 17L)
                param.result = null
            }

        gestureBackArrowViewClass.replaceMethod("loadRecentTaskIcon") {
            val mNoneTaskIcon = it.thisObject.getObjectFieldAs<Drawable?>("mNoneTaskIcon")
            val obj = it.thisObject as View
            val context = obj.context ?: return@replaceMethod mNoneTaskIcon
            val mPosition = obj.getIntField("mPosition")
            val icon = getNextTaskMethod.invoke(context, false, mPosition)
                ?.getObjectFieldAs<Drawable?>("icon")

            return@replaceMethod icon ?: mNoneTaskIcon
        }

        gestureStubViewClass.apply {
            getNextTaskMethod.hookBeforeMethod { param ->
                val context = param.args[0] as Context
                val switch = param.args[1] as Boolean

                val recentsModel = recentsModelClass.callStaticMethod("getInstance", context)
                val taskLoader = recentsModel?.callMethod("getTaskLoader")
                val taskLoadPlan = taskLoader?.callMethod("createLoadPlan", context)
                taskLoader?.callMethod("preloadTasks", taskLoadPlan, -1)
                val taskStack = taskLoadPlan?.callMethod("getTaskStack")
                var runningTask: ActivityManager.RunningTaskInfo? = null
                // var activityOptions: ActivityOptions? = null
                if (
                    taskStack == null ||
                    taskStack.callMethod("getTaskCount") as Int == 0 ||
                    (recentsModel.callMethod("getRunningTask") as ActivityManager.RunningTaskInfo?)?.also {
                        runningTask = it
                    } == null
                ) {
                    param.result = null
                    return@hookBeforeMethod
                }
                val stackTasks = taskStack.callMethod("getStackTasks") as ArrayList<*>
                val size = stackTasks.size
                var task: Any? = null
                for (index in stackTasks.indices) {
                    if (stackTasks[index].getObjectField("key")
                            ?.getObjectField("id") as Int == runningTask!!.taskId
                    ) {
                        task = stackTasks[index + 1]
                        break
                    }
                }
                if (task == null && size >= 1 && "com.miui.home" == runningTask!!.baseActivity!!.packageName) {
                    task = stackTasks[0]
                }

                if (task != null && task.getObjectField("icon") == null) {
                    task.setObjectField(
                        "icon",
                        taskLoader.callMethod(
                            "getAndUpdateActivityIcon",
                            task.getObjectField("key"),
                            task.getObjectField("taskDescription"),
                            context.resources, true
                        )
                    )
                }
                if (!switch || task == null) {
                    param.result = task
                    return@hookBeforeMethod
                }
                val activityManagerWrapper =
                    activityManagerWrapperClass.getStaticObjectField("sInstance")
                if (activityManagerWrapper == null) {
                    param.result = task
                    return@hookBeforeMethod
                }
                val key = XposedHelpers.getObjectField(task, "key")
                val taskId = XposedHelpers.getObjectField(key, "id")
                val windowingMode = XposedHelpers.getObjectField(key, "windowingMode") as Int
                val activityOptions = ActivityOptions.makeBasic()
                if (windowingMode == 3) {
                    setLaunchWindowingModeMethod.invoke(activityOptions, 4)
                }
                try {
                    val clz =
                        "com.android.systemui.shared.recents.utilities.RemoteAnimationFinishCallbackManager".findClass()
                    val remoteAnimationFinishCallbackManager = clz.callStaticMethod("getInstance")
                    if (remoteAnimationFinishCallbackManager?.callMethod("isQuickSwitchApp") as? Boolean == true) {
                        remoteAnimationFinishCallbackManager.callMethod("setQuickSwitchApp", false)
                        remoteAnimationFinishCallbackManager.callMethod("setOpenTaskId", taskId)
                    }
                    if (remoteAnimationFinishCallbackManager?.callMethod("isQuickSwitchApp") as? Boolean == true) {
                        remoteAnimationFinishCallbackManager.callMethod("finishMergeCallback")
                    }
                    remoteAnimationFinishCallbackManager?.callMethod("directExecuteWorkHandlerFinishRunnableIfNeed")
                    val activityManager = activityManagerClass.callStaticMethod("getDefault")
                    activityManager?.callMethod(
                        "startActivityFromRecents",
                        taskId, activityOptions.toBundle()
                    )
                    param.result = task
                } catch (t: Throwable) {
                    logE(TAG, lpparam.packageName, "$t")
                    param.result = task
                }
            }
        }

    }
}
