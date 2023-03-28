package com.sevtinge.cemiuiler.module.home.recent

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.findConstructor
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.getObjectField
import android.text.format.Formatter

@SuppressLint("StaticFieldLeak")
object RealMemory : BaseHook(){
    var context: Context? = null

    @SuppressLint("DiscouragedApi")
    override fun init() {
        findConstructor("com.miui.home.recents.views.RecentsContainer") { parameterCount == 2 }.hookAfter {
            context = it.args[0] as Context
        }
        findMethod("com.miui.home.recents.views.RecentsContainer") { name == "refreshMemoryInfo" }.hookBefore {
            it.result = null
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
            val totalMem = memoryInfo.totalMem.formatSize()
            val availMem = memoryInfo.availMem.formatSize()
            (it.thisObject.getObjectField("mTxtMemoryInfo1") as TextView).text = context!!.getString(context!!.resources.getIdentifier("status_bar_recent_memory_info1", "string", "com.miui.home"), availMem, totalMem)
            (it.thisObject.getObjectField("mTxtMemoryInfo2") as TextView).text = context!!.getString(context!!.resources.getIdentifier("status_bar_recent_memory_info2", "string", "com.miui.home"), availMem, totalMem)
        }
    }

    private fun Any.formatSize(): String = Formatter.formatFileSize(context, this as Long)

}