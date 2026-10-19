package fixture
import android.content.SharedPreferences
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.rules.home.other.NativeHomeHooksOS4 as Home
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object NativeProbe {
    external fun snapshot(): IntArray
    external fun failConfigureOnce()
}
private class Preferences : SharedPreferences {
    var registrations = 0
    var reject = false
    var onRegister: (() -> Unit)? = null
    var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) {
        registrations++
        check(!reject) { "listener registration unavailable" }
        listener = l
        onRegister?.invoke()
    }
}
private fun flag(name: String) = (Home::class.java.getDeclaredField(name).apply { isAccessible=true }.get(null) as AtomicBoolean).get()
private fun configured() = NativeProbe.snapshot()[1]
private fun preferences(): Preferences {
    PrefsBridge.values.putAll(mapOf("home_other_high_models" to true, "home_other_disable_prestart" to true,
        "home_dock_bg_custom_enable" to true, "home_dock_add_blur" to 1))
    return Preferences().also { PrefsBridge.preferences=it }
}
private fun cold() {
    Home.ensureLoadedFromModuleEntry("com.miui.home")
    check(NativeProbe.snapshot().take(7)==listOf(1,1,0,0,0,1,0)) { NativeProbe.snapshot().contentToString() }
    check(flag("preferencesRecoveryPending"))
}
private fun recovered(p: Preferences, configurations: Int = 2) {
    val s=NativeProbe.snapshot()
    check(s[0]==1 && s[1]==configurations && s[2]==1 && s[3]==1 && s[4]==1 && s[6]==1) { s.contentToString() }
    check(!flag("preferencesRecoveryPending") && flag("dockWatchStarted") && p.listener!=null)
}
fun main(args: Array<String>) {
    when(val name=args[0]) {
        "restore_and_idempotence" -> {
            cold(); val p=preferences(); Home.onPreferencesReadyAfterRetry(); recovered(p)
            repeat(3) { Home.onPreferencesReadyAfterRetry(); Home.init() }
            check(configured()==2 && p.registrations==1)
            PrefsBridge.values["home_dock_bg_custom_enable"]=false
            p.listener!!.onSharedPreferenceChanged(p,"prefs_key_home_dock_bg_custom_enable")
            val before=NativeProbe.snapshot().toList()
            Home.onPreferencesReadyAfterRetry();Home.init()
            check(NativeProbe.snapshot().toList()==before && before[6]==0)
        }
        "module_entry_cannot_recover" -> {
            cold(); val p=preferences();Home.ensureLoadedFromModuleEntry("com.miui.home")
            check(configured()==1 && p.registrations==0 && flag("preferencesRecoveryPending"))
            Home.onPreferencesReadyAfterRetry();recovered(p)
        }
        "listener_retry_and_reentry" -> {
            cold();val p=preferences();p.reject=true;Home.onPreferencesReadyAfterRetry()
            check(flag("preferencesRecoveryPending") && !flag("dockWatchStarted"))
            p.reject=false;p.onRegister={Home.onPreferencesReadyAfterRetry()};Home.init();recovered(p,3)
            check(p.registrations==2)
        }
        "configuration_failure_retry" -> {
            cold();val p=preferences();NativeProbe.failConfigureOnce();Home.onPreferencesReadyAfterRetry()
            check(flag("preferencesRecoveryPending") && p.registrations==0)
            Home.onPreferencesReadyAfterRetry();recovered(p,3)
        }
        "read_failure_retry" -> {
            cold();val p=preferences();PrefsBridge.rejectRead=true;Home.onPreferencesReadyAfterRetry()
            check(flag("preferencesRecoveryPending") && configured()==1 && p.registrations==0)
            PrefsBridge.rejectRead=false;Home.init();recovered(p)
        }
        "failed_library_never_configures" -> {
            Home.ensureLoadedFromModuleEntry("com.miui.home");preferences();Home.onPreferencesReadyAfterRetry();Home.init()
            check(!Home::class.java.getDeclaredField("libraryLoaded").apply{isAccessible=true}.getBoolean(null))
            check(!flag("dockWatchStarted"))
        }
        "healthy_bootstrap_unchanged" -> {
            val p=preferences();Home.ensureLoadedFromModuleEntry("com.miui.home");recovered(p,1)
            repeat(3){Home.onPreferencesReadyAfterRetry();Home.init()}
            check(configured()==1 && p.registrations==1)
        }
        "no_preferences_yet" -> {
            cold();Home.onPreferencesReadyAfterRetry();Home.init()
            check(configured()==1 && flag("preferencesRecoveryPending") && !flag("dockWatchStarted"))
        }
        "non_null_handle_failed_bootstrap" -> {
            val p=preferences();PrefsBridge.rejectRead=true
            Home.ensureLoadedFromModuleEntry("com.miui.home",false)
            check(flag("preferencesRecoveryPending") && configured()==1)
            check(NativeProbe.snapshot().slice(2..4)==listOf(0,0,0))
            PrefsBridge.rejectRead=false;Home.onPreferencesReadyAfterRetry();recovered(p)
            check(p.registrations==1)
        }
        "changed_preference_handle" -> {
            val early=preferences();PrefsBridge.rejectRead=true
            Home.ensureLoadedFromModuleEntry("com.miui.home",false)
            check(early.registrations==0 && flag("preferencesRecoveryPending"))
            val ready=preferences();PrefsBridge.rejectRead=false
            Home.onPreferencesReadyAfterRetry();recovered(ready)
            check(early.listener==null && ready.registrations==1)
            PrefsBridge.values["home_dock_bg_custom_enable"]=false
            ready.listener!!.onSharedPreferenceChanged(ready,"prefs_key_home_dock_bg_custom_enable")
            check(NativeProbe.snapshot()[6]==0)
            Home.onPreferencesReadyAfterRetry();check(ready.registrations==1)
        }
        "late_concurrent_recovery" -> {
            cold();val p=preferences();val blocked=CountDownLatch(1);val release=CountDownLatch(1)
            PrefsBridge.onGetPreferences={if(Thread.currentThread().name=="late-recovery"){
                blocked.countDown();check(release.await(5,TimeUnit.SECONDS))}}
            var failure: Throwable?=null
            val late=Thread({try{Home.onPreferencesReadyAfterRetry()}catch(t:Throwable){failure=t}},"late-recovery")
            late.start();check(blocked.await(5,TimeUnit.SECONDS))
            Home.onPreferencesReadyAfterRetry();recovered(p);release.countDown();late.join(5000)
            check(!late.isAlive && failure==null && configured()==2 && p.registrations==1)
        }
        else -> error(name)
    }
    println("PASS native_${args[0]}")
}
