package com.sevtinge.cemiuiler.module.home.recent

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.text.format.Formatter
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.getObjectField

@SuppressLint("StaticFieldLeak")
object RealMemory : BaseHook() {
    var context: Context? = null

    @SuppressLint("DiscouragedApi")
    override fun init() {
        var context: Context? = null
        fun Any.formatSize(): String = Formatter.formatFileSize(context, this as Long)
        val mRecentsContainerClass = loadClass("com.miui.home.recents.views.RecentsContainer")

        mRecentsContainerClass.declaredConstructors.constructorFinder().first {
            parameterCount == 2
        }.createHook {
            after {
                context = it.args[0] as Context
            }
        }

        mRecentsContainerClass.methodFinder().first {
            name == "refreshMemoryInfo"
        }.createHook {
            before {
                it.result = null
                val memoryInfo = ActivityManager.MemoryInfo()
                val activityManager =
                    context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.getMemoryInfo(memoryInfo)
                val totalMem = memoryInfo.totalMem.formatSize()
                val availMem = memoryInfo.availMem.formatSize()

                (it.thisObject.getObjectField("mTxtMemoryInfo1") as TextView).text =
                    context!!.getString(
                        context!!.resources.getIdentifier(
                            "status_bar_recent_memory_info1",
                            "string",
                            "com.miui.home"
                        ), availMem, totalMem
                    )

                (it.thisObject.getObjectField("mTxtMemoryInfo2") as TextView).text =
                    context!!.getString(
                        context!!.resources.getIdentifier(
                            "status_bar_recent_memory_info2",
                            "string",
                            "com.miui.home"
                        ), availMem, totalMem
                    )
            }
        }
    }
}
