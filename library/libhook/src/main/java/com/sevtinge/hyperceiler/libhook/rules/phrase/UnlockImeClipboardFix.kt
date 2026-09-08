package com.sevtinge.hyperceiler.libhook.rules.phrase

import com.sevtinge.hyperceiler.libhook.appbase.input.InputMethodConfig
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import java.lang.reflect.Modifier
import java.util.*

class UnlockImeClipboardFix : BaseHook() {
    override fun init() {
        val target = InputMethodConfig.getSelectedInputMethodPackages()

        val inputProviderClz = findClassIfExists("com.miui.provider.InputProvider") ?: return

        for (field in inputProviderClz.declaredFields) {
            val modifiers = field.modifiers

            if (!Modifier.isPublic(modifiers)) continue
            if (!Modifier.isStatic(modifiers)) continue
            if (!Modifier.isFinal(modifiers)) continue
            if (field.type != List::class.java) continue
            field.isAccessible = true

            val listObj = field.get(null) as? List<*> ?: continue
            if (listObj.isEmpty() || listObj[0] !is String) continue

            @Suppress("UNCHECKED_CAST")
            val whiteList = listObj as List<String>

            if (whiteList.contains("com.xiaomi.type")) {
                val toAdd = target - whiteList.toSet()

                if (toAdd.isNotEmpty()) {
                    val newList = whiteList.toMutableList().apply { addAll(toAdd) }
                    field.set(null, Collections.unmodifiableList(newList))
                }

                break
            }
        }
    }
}
