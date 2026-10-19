package com.sevtinge.hyperceiler.tests.notification

import android.os.Handler
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.AutoDismissExpandedPopupsHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.RegisteredHooks

class NotificationEntry(@JvmField val mKey: String, var pinned: Boolean = true) {
    fun isRowPinned() = pinned
}
class EntryAdapter(private val entry: NotificationEntry?, private val key: String) {
    fun getEntry() = entry
    fun getKey() = key
}
class Row
class Keyguard(@JvmField var mShowing: Boolean = false)
class HeadsUpEntry(@JvmField val mEntry: NotificationEntry) {
    @JvmField var mExpanded = true
    @JvmField var mRemoteInputActive = false
    @JvmField var extended = false
    var removed = 0
    @JvmField var mRemoveRunnable: Runnable? = Runnable { removed++ }
    fun updateEntry() = Unit
}
class LegacyHeadsUpEntry {
    @JvmField var remoteInputActive = false
    var removed = 0
    @JvmField val mRemoveAlertRunnable = Runnable { removed++ }
}
class HeadsUpManager {
    val entries = mutableMapOf<String, Any>()
    val queries = mutableListOf<String>()
    fun getHeadsUpEntry(key: String): Any? { queries += key; return entries[key] }
}
class Presenter {
    @JvmField val mKeyguardStateController = Keyguard()
    @JvmField val mHeadsUpManager = HeadsUpManager()
    fun onExpandClicked(entry: NotificationEntry, expanded: Boolean) = Unit
    fun onExpandClicked(row: Row, adapter: EntryAdapter, expanded: Boolean) = Unit
}

fun main() {
    AutoDismissExpandedPopupsHook.init()
    check(RegisteredHooks.callbacks.keys.count { it.name == "onExpandClicked" } == 2)
    var passed = 0
    var failed = 0
    fun test(name: String, body: () -> Unit) {
        Handler.pending.clear()
        try { body(); passed++; println("PASS $name") }
        catch (failure: Throwable) { failed++; println("FAIL $name: $failure") }
    }
    fun setup(): Triple<Presenter, NotificationEntry, HeadsUpEntry> {
        val entry = NotificationEntry("entry-key")
        val headsUp = HeadsUpEntry(entry)
        val presenter = Presenter().also { it.mHeadsUpManager.entries[entry.mKey] = headsUp }
        return Triple(presenter, entry, headsUp)
    }
    fun os4(p: Presenter, e: NotificationEntry?, expanded: Boolean = true, key: String = "entry-key") {
        RegisteredHooks.invoke("onExpandClicked", 3, p, Row(), EntryAdapter(e, key), expanded)
    }
    fun expectQueued(headsUp: HeadsUpEntry) {
        check(headsUp.removed == 0) { "removal must be delayed" }
        check(Handler.pending.single().first === headsUp.mRemoveRunnable)
        check(Handler.pending.single().second == 3700L) { "configured tenths of a second were not respected" }
        Handler.drain()
        check(headsUp.removed == 1)
    }
    test("legacy two-argument callback queues removal") {
        val (p, e, h) = setup()
        RegisteredHooks.invoke("onExpandClicked", 2, p, e, true)
        expectQueued(h)
    }
    test("OS4 row-adapter-boolean callback queues removal") {
        val (p, e, h) = setup()
        os4(p, e)
        expectQueued(h)
    }
    test("OS4 uses adapter key rather than entry field") {
        val (p, e, h) = setup()
        p.mHeadsUpManager.entries.clear()
        p.mHeadsUpManager.entries["adapter-key"] = h
        os4(p, e, key = "adapter-key")
        check(p.mHeadsUpManager.queries == listOf("adapter-key"))
        expectQueued(h)
    }
    test("collapsed notification remains unchanged") {
        val (p, e) = setup(); os4(p, e, false); check(Handler.pending.isEmpty())
    }
    test("lockscreen prevents delayed removal") {
        val (p, e) = setup(); p.mKeyguardStateController.mShowing = true
        os4(p, e); check(Handler.pending.isEmpty())
    }
    test("unpinned row is not removed") {
        val (p, e) = setup(); e.pinned = false; os4(p, e); check(Handler.pending.isEmpty())
    }
    test("active reply input is not dismissed on OS4") {
        val (p, e, h) = setup(); h.mRemoteInputActive = true
        os4(p, e); check(Handler.pending.isEmpty())
    }
    test("active reply input is not dismissed on legacy callback") {
        val (p, e, h) = setup(); h.mRemoteInputActive = true
        RegisteredHooks.invoke("onExpandClicked", 2, p, e, true)
        check(Handler.pending.isEmpty())
    }
    test("null adapter entry safely skips removal") {
        val (p) = setup(); os4(p, null); check(Handler.pending.isEmpty())
    }
    test("missing heads-up entry safely skips removal") {
        val (p, e) = setup(); p.mHeadsUpManager.entries.clear()
        os4(p, e); check(Handler.pending.isEmpty())
    }
    test("missing runnable safely skips removal") {
        val (p, e, h) = setup(); h.mRemoveRunnable = null
        os4(p, e); check(Handler.pending.isEmpty())
    }
    test("legacy runnable and remote-input field names remain supported") {
        val (p, e) = setup(); val h = LegacyHeadsUpEntry()
        p.mHeadsUpManager.entries[e.mKey] = h
        RegisteredHooks.invoke("onExpandClicked", 2, p, e, true)
        check(Handler.pending.single().first === h.mRemoveAlertRunnable)
        Handler.drain(); check(h.removed == 1)
        h.remoteInputActive = true
        RegisteredHooks.invoke("onExpandClicked", 2, p, e, true)
        check(Handler.pending.isEmpty())
    }
    test("unexpected callback argument counts are ignored") {
        val (p, e) = setup()
        for (args in listOf(emptyArray(), arrayOf(e), arrayOf(Row(), e, true, "extra"))) {
            RegisteredHooks.invoke("onExpandClicked", 3, p, *args)
        }
        check(Handler.pending.isEmpty())
    }
    test("unexpected expansion type is ignored") {
        val (p, e) = setup()
        RegisteredHooks.invoke("onExpandClicked", 3, p, Row(), EntryAdapter(e, e.mKey), "true")
        check(Handler.pending.isEmpty())
    }
    test("unexpected adapter type is ignored") {
        val (p) = setup()
        RegisteredHooks.invoke("onExpandClicked", 3, p, Row(), Any(), true)
        check(Handler.pending.isEmpty())
    }
    test("updateEntry still schedules configured delay") {
        val (_, _, h) = setup(); RegisteredHooks.invoke("updateEntry", 0, h)
        expectQueued(h)
    }
    test("extended heads-up entry retains ten-second delay") {
        val (_, _, h) = setup(); h.extended = true
        RegisteredHooks.invoke("updateEntry", 0, h)
        check(Handler.pending.single().second == 10000L)
    }
    println("Notification expansion callbacks: $passed passed, $failed failed")
    check(failed == 0) { "$failed regression checks failed" }
}
