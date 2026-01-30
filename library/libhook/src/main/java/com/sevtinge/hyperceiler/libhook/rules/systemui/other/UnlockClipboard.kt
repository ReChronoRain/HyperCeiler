package com.sevtinge.hyperceiler.libhook.rules.systemui.other

import android.content.ClipboardManager
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.getObjectFieldOrNullAs
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.setObjectField
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.core.util.ClassUtil
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createBeforeHook

object UnlockClipboard : BaseHook() {
    override fun init() {
        // hook 点来自 淡い夏
        // 解锁原生剪切板编辑框
        // 新方法来自 WOMMO
        val clazzClipboardListener =
            ClassUtil.loadClass("com.android.systemui.clipboardoverlay.ClipboardListener")
        if (clazzClipboardListener.declaredFields.any { it.name == "sCtsTestPkgList" })
            clazzClipboardListener.methodFinder().filterByName("onPrimaryClipChanged")
                .filterNonAbstract().single().createBeforeHook { param ->
                    val mClipboardManager =
                        // Android 16 changed mClipboardManager to mClipboardManagerForUser
                        param.thisObject.getObjectFieldOrNullAs<ClipboardManager>("mClipboardManagerForUser")
                            ?: param.thisObject.getObjectFieldOrNullAs<ClipboardManager>("mClipboardManager")
                            ?: return@createBeforeHook
                    val primaryClipSource =
                        callMethod(mClipboardManager, "getPrimaryClipSource") as String?
                    val oldList =
                        param.thisObject.getObjectFieldOrNullAs<List<String>>("sCtsTestPkgList")!!
                    val newList = mutableListOf<String>().apply {
                        addAll(oldList)
                        if (!contains(primaryClipSource)) primaryClipSource?.let { add(it) }
                    }
                    param.thisObject.setObjectField("sCtsTestPkgList", newList)
                }
        else clazzClipboardListener.methodFinder().filterByName("start").filterNonAbstract()
            .single().createBeforeHook {
                val mClipboardManager =
                    it.thisObject.getObjectFieldOrNullAs<ClipboardManager>("mClipboardManager")!!
                mClipboardManager.addPrimaryClipChangedListener(it.thisObject as ClipboardManager.OnPrimaryClipChangedListener?)
                it.result = null
            }
    }
}
