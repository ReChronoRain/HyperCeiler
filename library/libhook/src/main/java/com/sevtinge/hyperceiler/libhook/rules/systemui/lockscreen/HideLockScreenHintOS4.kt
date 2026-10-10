/*
 * This file is part of HyperCeiler, licensed under AGPL-3.0-or-later.
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Looper
import android.view.View
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.libxposed.api.XposedInterface
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The confirmed stock bottom hint is type 13 (soft-sleep dismissible keyguard).
 * Other/default-theme text producers are not inferred from their wording.
 */
object HideLockScreenHintOS4 : BaseHook() {
    // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
    @Suppress("TooGenericExceptionCaught")
    override fun init() {
        if (!PrefsBridge.getBoolean("system_ui_lock_screen_unlock_tip")) return
        val owner = required("com.android.systemui.keyguard.KeyguardIndicationRotateTextViewController")
        val value = required("com.android.systemui.keyguard.KeyguardIndication")
        val viewType = required("com.android.systemui.statusbar.phone.KeyguardIndicationTextView")
        val base = required("com.android.systemui.util.ViewController")
        check(owner.superclass == base && Modifier.isFinal(owner.modifiers))
        check(View::class.java.isAssignableFrom(viewType) && Modifier.isFinal(value.modifiers))
        val view = field(base, "mView", View::class.java)
        check(Modifier.isFinal(view.modifiers))
        val message = field(value, "mMessage", CharSequence::class.java)
        val color = field(value, "mTextColor", ColorStateList::class.java)
        val action = field(value, "mOnClickListener", View.OnClickListener::class.java)
        val background = field(value, "mBackground", Drawable::class.java)
        val minimum = field(value, "mMinVisibilityMillis", java.lang.Long::class.java)
        val assertive = field(value, "mForceAccessibilityLiveRegionAssertive", Boolean::class.javaPrimitiveType!!)
        val theme = required("com.miui.systemui.util.CommonUtil").getDeclaredMethod("isDefaultLockScreenTheme").also {
            check(Modifier.isStatic(it.modifiers) && it.returnType == Boolean::class.javaPrimitiveType)
            it.isAccessible = true
        }
        val method = owner.getDeclaredMethod("updateIndication", Int::class.javaPrimitiveType, value, Boolean::class.javaPrimitiveType).also {
            check(!Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers) && it.returnType == Void.TYPE)
            it.isAccessible = true
        }
        val active = AtomicBoolean(false)
        var handle: XposedInterface.HookHandle? = null
        try {
            handle = checkNotNull(hookMethod(method, object : IMethodHook {
                // ROM calls and rollback can throw arbitrary runtime failures; preserve the host and cleanup boundary.
                @Suppress("TooGenericExceptionCaught")
                override fun before(param: HookParam) {
                    if (!active.get() || param.isSkipped || Looper.myLooper() !== Looper.getMainLooper()) return
                    try {
                        if (param.thisObject.javaClass != owner || param.args.size != 3) return
                        if (param.args[0] != 13 || param.args[2] != true) return
                        val indication = param.args[1] ?: return
                        if (indication.javaClass != value || theme.invoke(null) != true) return
                        val bottom = view.get(param.thisObject) as? View ?: return
                        if (bottom.javaClass != viewType) return
                        val resources = bottom.resources
                        val bottomId = resources.getIdentifier("keyguard_indication_text_bottom", "id", "com.android.systemui")
                        if (bottomId == 0 || bottom.id != bottomId || resources.getResourceName(bottomId) != "com.android.systemui:id/keyguard_indication_text_bottom") return
                        val textId = resources.getIdentifier("dismissible_keyguard_swipe", "string", "com.android.systemui")
                        if (textId == 0 || resources.getResourceName(textId) != "com.android.systemui:string/dismissible_keyguard_swipe") return
                        // The actual producer uses Resources.getString, not arbitrary styled text.
                        val text = message.get(indication)
                        if (text !is String || text.isEmpty() || text != resources.getString(textId)) return
                        if (color.get(indication) == null || action.get(indication) != null) return
                        if (background.get(indication) != null || minimum.get(indication) != null || assertive.getBoolean(indication)) return
                        if (!active.get() || param.args[1] !== indication) return
                        // A before-argument substitution, not a replacement hook: the OEM
                        // removes its own map/queue entry and performs its pending transition.
                        param.args[1] = null
                    } catch (error: Exception) {
                        debugCallbackError("OS4 stock unlock hint", error)
                    }
                }
            })) { "OS4 stock unlock hint registration failed" }
            registerHotReloadCleanup { active.set(false) }
            active.set(true)
        } catch (error: Throwable) {
            active.set(false)
            try { handle?.unhook() } catch (rollback: Throwable) { error.addSuppressed(rollback) }
            throw error
        }
    }

    private fun required(name: String): Class<*> = checkNotNull(findClassIfExists(name)) { "OS4 unlock hint class unavailable: $name" }
    private fun field(owner: Class<*>, name: String, type: Class<*>): Field = owner.getDeclaredField(name).also {
        check(it.type == type && !Modifier.isStatic(it.modifiers))
        it.isAccessible = true
    }
}
