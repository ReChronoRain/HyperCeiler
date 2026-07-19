package com.sevtinge.hyperceiler.libhook.rules.systemui.other

import android.content.ClipboardManager
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook

object UnlockClipboard : BaseHook() {
    override fun init() {
        // hook 点来自 淡い夏
        // 解锁原生剪切板编辑框
        // 新方法来自 WOMMO
        val clazzClipboardListener =
            loadClass("com.android.systemui.clipboardoverlay.ClipboardListener")
        if (clazzClipboardListener.declaredFields.any { it.name == "sCtsTestPkgList" })
            clazzClipboardListener.findMethod { name("onPrimaryClipChanged"); notAbstract() }.createBeforeHook { param ->
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
        else clazzClipboardListener.findMethod { name("start"); notAbstract() }.createBeforeHook {
                val mClipboardManager =
                    it.thisObject.getObjectFieldOrNullAs<ClipboardManager>("mClipboardManager")!!
                mClipboardManager.addPrimaryClipChangedListener(it.thisObject as ClipboardManager.OnPrimaryClipChangedListener?)
                it.result = null
            }
    }
}
