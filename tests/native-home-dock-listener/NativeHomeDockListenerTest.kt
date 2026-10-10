package com.sevtinge.hyperceiler.tests.nativehome

import android.content.SharedPreferences
import android.util.Log
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.rules.home.other.NativeHomeHooksOS4
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

private const val STORED_KEY = "prefs_key_home_dock_bg_custom_enable"
private const val READ_KEY = "home_dock_bg_custom_enable"

private class Preferences : SharedPreferences {
    var registrations = 0
    var rejectRegistration = false
    var onRegister: (() -> Unit)? = null
    // Android SharedPreferences permits weak listener retention. This is a robustness test;
    // current LSPosed remote preferences may themselves keep a strong listener set.
    var listener = WeakReference<SharedPreferences.OnSharedPreferenceChangeListener>(null)
    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        registrations++
        if (rejectRegistration) throw IllegalStateException("registration unavailable")
        this.listener = WeakReference(listener)
        onRegister?.invoke()
    }
    fun send(key: String?) {
        checkNotNull(listener.get()) { "listener was not registered or was collected" }
            .onSharedPreferenceChanged(this, key)
    }
}

private val watcher = NativeHomeHooksOS4::class.java.getDeclaredMethod("watchDockEnabled").apply {
    isAccessible = true
}
private val registrationFlag = NativeHomeHooksOS4::class.java.getDeclaredField("dockWatchStarted")
    .apply { isAccessible = true }.get(null) as AtomicBoolean

private fun register() { watcher.invoke(NativeHomeHooksOS4) }
private fun reset(preferences: Preferences? = Preferences()): Preferences? {
    registrationFlag.set(false)
    PrefsBridge.preferences = preferences
    PrefsBridge.enabled = false
    PrefsBridge.reads.clear()
    Log.lines.clear()
    return preferences
}
private fun expectRefresh(enabled: Boolean) {
    check(PrefsBridge.reads.any { it == READ_KEY to enabled }) {
        "production callback did not read the current Dock preference"
    }
    check(Log.lines.any { it.startsWith("stage=dock switch refreshed") && it.endsWith("enabled=$enabled") }) {
        "production callback did not finish its native synchronization attempt"
    }
}

fun main() {
    var passed = 0
    var failed = 0
    fun test(name: String, body: () -> Unit) {
        try { body(); passed++; println("PASS $name") }
        catch (failure: Throwable) { failed++; println("FAIL $name: $failure") }
    }
    test("stored key delivers live enable and disable values") {
        val prefs = reset()!!
        register()
        for (enabled in listOf(true, false)) {
            PrefsBridge.enabled = enabled
            PrefsBridge.reads.clear(); Log.lines.clear()
            prefs.send(STORED_KEY)
            expectRefresh(enabled)
        }
    }
    test("unrelated preference callback does not synchronize Dock") {
        val prefs = reset()!!
        register()
        PrefsBridge.reads.clear(); Log.lines.clear()
        prefs.send("prefs_key_home_other_high_models")
        check(PrefsBridge.reads.isEmpty() && Log.lines.isEmpty())
    }
    test("null clear callback delivers the reset Dock value") {
        val prefs = reset()!!
        register()
        PrefsBridge.enabled = true
        prefs.send(STORED_KEY)
        PrefsBridge.enabled = false
        PrefsBridge.reads.clear(); Log.lines.clear()
        prefs.send(null)
        expectRefresh(false)
    }
    test("successful registration is idempotent") {
        val prefs = reset()!!
        register(); register()
        check(registrationFlag.get() && prefs.registrations == 1)
    }
    test("registration resynchronizes a switch changed after the initial push") {
        val prefs = reset()!!
        prefs.onRegister = { PrefsBridge.enabled = true }
        register()
        check(PrefsBridge.reads.any { it == READ_KEY to true })
    }
    test("temporarily unavailable preferences do not latch registration") {
        reset(null)
        register()
        check(!registrationFlag.get())
        val prefs = Preferences()
        PrefsBridge.preferences = prefs
        register()
        check(registrationFlag.get() && prefs.registrations == 1)
        prefs.send(STORED_KEY)
        expectRefresh(false)
    }
    test("registration exception permits a later successful attempt") {
        val prefs = reset()!!
        prefs.rejectRegistration = true
        register()
        check(!registrationFlag.get())
        prefs.rejectRegistration = false
        register()
        check(registrationFlag.get() && prefs.registrations == 2)
        prefs.send(STORED_KEY)
        expectRefresh(false)
    }
    test("listener remains owned by the module with a weak preference registry") {
        val prefs = reset()!!
        register()
        repeat(4) { System.gc(); Thread.sleep(20) }
        checkNotNull(prefs.listener.get())
        prefs.send(STORED_KEY)
        expectRefresh(false)
    }
    println("NativeHome Dock listener: $passed passed, $failed failed")
    check(failed == 0) { "$failed regression checks failed" }
}
