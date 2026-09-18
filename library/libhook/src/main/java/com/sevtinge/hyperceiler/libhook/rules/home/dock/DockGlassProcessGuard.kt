/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.home.dock

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import java.util.ArrayList

/** Protect only verified HyperCeiler render leases, never another application's UID. */
internal class DockGlassProcessGuard {
    private val policy = DockGlassProcessPolicy()
    private var serviceClass: Class<*>? = null

    /**
     * UID of the HyperCeiler package.
     *
     * <p>An upgrade keeps the UID and it outlives every renderer restart, so a successful
     * lookup is cached and re-validated with {@code getPackagesForUid} instead of asking
     * {@code PackageManager} for the application record on every recovery attempt. That call
     * is the fragile one: it is what throws while the package is mid-replacement.
     */
    @Volatile private var cachedUid = -1
    @Volatile private var closed = false

    fun install() {
        val clazz = loadClass("com.miui.server.greeze.GreezeManagerService")
        val uidMethod = clazz.getDeclaredMethod("freezeUids", IntArray::class.java,
            Long::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java,
            Boolean::class.javaPrimitiveType)
        val pidMethod = clazz.getDeclaredMethod("freezePids", IntArray::class.java,
            Long::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
        val pidUid = android.os.Process::class.java.getDeclaredMethod("getUidForPid", Int::class.javaPrimitiveType)
        for ((method, pids) in listOf(uidMethod to false, pidMethod to true)) {
            method.isAccessible = true
            method.createBeforeHook { param ->
                val original = param.args[0] as? IntArray
                if (!closed && original != null) {
                    val filtered = policy.filter(original, pids) { pid ->
                        // Check only candidate renderer PIDs, avoiding a PID-reuse exemption.
                        catchingRecoverableOr(-1) { pidUid.invoke(null, pid) as Int }
                    }
                    if (filtered !== original) {
                        if (filtered.isEmpty()) param.result = ArrayList<Int>()
                        else param.args[0] = filtered
                    }
                }
            }
        }
        val processConfig = loadClass("miui.process.ProcessConfig")
        val getPolicy = processConfig.getDeclaredMethod("getPolicy").apply { isAccessible = true }
        val getWhiteList = processConfig.getDeclaredMethod("getWhiteList").apply { isAccessible = true }
        val setWhiteList = processConfig.getDeclaredMethod("setWhiteList", List::class.java)
            .apply { isAccessible = true }
        val oneKeyClean = loadClass("com.android.server.am.ProcessSceneCleaner")
            .getDeclaredMethod("handleKillAll", processConfig).apply { isAccessible = true }
        oneKeyClean.createBeforeHook { param ->
            val config = param.args[0] ?: return@createBeforeHook
            if (closed || getPolicy.invoke(config) != 1 ||
                !policy.hasLiveOwner { pid ->
                    catchingRecoverableOr(-1) { pidUid.invoke(null, pid) as Int }
                }) return@createBeforeHook

            val existing = getWhiteList.invoke(config) as? List<*>
            if (existing?.contains(PACKAGE_NAME) == true) return@createBeforeHook
            val protected = ArrayList<String>(existing?.size?.plus(1) ?: 1)
            existing?.filterIsInstanceTo(protected)
            protected.add(PACKAGE_NAME)
            setWhiteList.invoke(config, protected)
            Log.i("HyperCeiler.DockGlass", "OneKeyClean protected active renderer lease")
        }
        serviceClass = clazz
    }

    /**
     * Resolve the UID whose renderer lease this ticket is allowed to protect.
     *
     * @return the UID, or {@code null} when the package is momentarily unresolvable — an
     *         in-place upgrade, an uninstall in flight, or a `PackageManager` that is busy.
     *         That is a transient dependency outage, not a defect of this ticket, so it is
     *         reported as such instead of being thrown at the caller.
     */
    private fun resolveUid(context: Context): Int? {
        val pm = context.packageManager
        val cached = cachedUid
        if (cached >= 0) {
            // Unknown UID (uninstall, or a PM outage) returns null and falls through to the
            // authoritative lookup; anything else must still be owned by our package alone.
            val owners = pm.getPackagesForUid(cached)
            if (owners != null) {
                if (owners.size == 1 && owners[0] == PACKAGE_NAME) return cached
                cachedUid = -1 // Reinstalled under a new UID.
            }
        }
        val info = try {
            pm.getApplicationInfo(PACKAGE_NAME, 0)
        } catch (error: PackageManager.NameNotFoundException) {
            Log.i("HyperCeiler.DockGlass",
                "Renderer package temporarily unavailable: ${error.message?.take(160)}")
            return null
        }
        val owners = pm.getPackagesForUid(info.uid)
        check(owners?.size == 1 && owners[0] == PACKAGE_NAME) { "Renderer UID ownership is ambiguous" }
        cachedUid = info.uid
        return info.uid
    }

    /**
     * Acquire the freezer exemption for [token].
     *
     * <p>Called only on the IPC worker: no `PackageManager` or thaw call happens under WM locks.
     *
     * @return false when the HyperCeiler package is temporarily unresolvable. The caller must
     *         then schedule a bounded retry; no lease was taken, so there is nothing to undo.
     *         Every other failure still throws, and no VM-fatal error is ever swallowed.
     */
    fun acquire(context: Context, token: Any): Boolean {
        check(!closed) { "Renderer guard closed" }
        val clazz = checkNotNull(serviceClass) { "OS4 renderer lifecycle API unavailable" }
        val uid = resolveUid(context) ?: return false
        policy.acquire(token, uid)
        var ready = false
        try {
            val service = clazz.getDeclaredMethod("getService").invoke(null)
                ?: error("OS4 freezer service unavailable")
            val frozen = clazz.getDeclaredMethod("isUidFrozen", Int::class.javaPrimitiveType)
                .invoke(service, uid) == true
            if (frozen) {
                val modules = loadClass("com.miui.server.greeze.GreezeServiceUtils")
                val allModules = modules.getField("GREEZER_MODULE_ALL").getInt(null)
                check(clazz.getDeclaredMethod("thawUid", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, String::class.java)
                    .invoke(service, uid, allModules, "HyperCeiler Dock renderer Start") == true) {
                    "Renderer could not be thawed"
                }
            }
            ready = true
        } finally {
            // No failed/partial acquisition may leave an unowned freezer exemption.
            if (!ready) policy.release(token)
        }
        return true
    }

    fun setPid(token: Any, pid: Int) { policy.setPid(token, pid) }
    fun release(token: Any) { policy.release(token) }
    fun close() { closed = true; policy.clear() }

    private companion object {
        const val PACKAGE_NAME = "com.sevtinge.hyperceiler"
    }
}
