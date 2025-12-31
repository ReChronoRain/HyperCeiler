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
package com.sevtinge.hyperceiler.hook.module.base.pack.home

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool.getPackageVersionCode
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils
import com.sevtinge.hyperceiler.hook.utils.devicesdk.isPad
import com.sevtinge.hyperceiler.hook.utils.pkg.DebugModeUtils

abstract class HomeBaseHookNew : BaseHook() {

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FUNCTION)
    annotation class Version(val min: Int = Int.MIN_VALUE, val max: Int = Int.MAX_VALUE, val isPad: Boolean)

    /**
     * 如果需要跳过部分版本的匹配，可以在子类中设置该范围
     * 例如: isSkip = 701021135..702021135
     */
    var isSkip: IntRange? = null

    /**
     * 子类请实现原有的默认 init 逻辑，命名为 initBase()
     * 不要再直接 override init()，本类 final override init() 并负责分发到合适的方法。
     */
    protected abstract fun initBase()

    /**
     * 子类或 BaseHook 可重写以提供真实的版本号/是否平板信息
     */

    private val isDebug: Boolean by lazy {
        mPrefsMap.getBoolean("development_debug_mode")
    }
    private var _cachedAppVersion: Int? = null
    private var _cachedIsPad: Boolean? = null

    protected open fun appVersion(): Int {
        val v = if (isDebug) {
            _cachedAppVersion ?: DebugModeUtils.getChooseResult(lpparam.packageName).also { _cachedAppVersion = it }
        } else {
            _cachedAppVersion ?: getPackageVersionCode(lpparam).also { _cachedAppVersion = it }
        }
        return v
    }
    protected open fun isPadDevice(): Boolean = _cachedIsPad ?: isPad().also { _cachedIsPad = it }

    final override fun init() {
        val version = appVersion()
        val isPadCached = isPadDevice()

        // 针对修改版的检测不在于此处，避免调用过多
        // 针对目标版本进行筛选
        // RELEASE-6.01.02.1135-09051745 (601021135) 手机端桌面
        // RELEASE-4.50.0.592-0821-09051648 (450000592) 平板端桌面
        if ((version < 600000000 && !isPadDevice()) || (version < 450000000 && isPadDevice())) {
            initBase()
            logD(TAG, lpparam.packageName, "is load old hook")
            return
        } else if (version >= 900000000) {
            logD(TAG, lpparam.packageName, "version $version is too high, skip hook")
            return
        }

        // 优先匹配 isSkip：如果在跳过范围内，直接返回不处理
        val skip = isSkip
        if (skip != null && version in skip) {
            return
        }

        // 寻找带 @Version 注解且无参数的方法并逐一匹配
        val methods = this::class.java.declaredMethods
            .filter { it.isAnnotationPresent(Version::class.java) && it.parameterCount == 0 }

        for (m in methods) {
            val anno = m.getAnnotation(Version::class.java)
            if (anno != null) {
                // 如果注解显式指定 isPad，则按指定值匹配；未显式指定则忽略 isPad 条件
                val defaultIsPad = try {
                    Version::class.java.getMethod("isPad").defaultValue as? Boolean ?: false
                } catch (_: Exception) {
                    false
                }
                val isPadSpecified = anno.isPad != defaultIsPad

                if (version >= anno.min && version <= anno.max && (!isPadSpecified || anno.isPad == isPadCached)) {
                    try {
                        logD(TAG, lpparam.packageName, "Check method ${m.name} for version $version, select ${anno.min} to ${anno.max}, isPad = ${anno.isPad}")
                        m.isAccessible = true
                        m.invoke(this)
                        return
                    } catch (t: Throwable) {
                        logE(TAG, lpparam.packageName, "Invoke method ${m.name} failed", t)
                    }
                }
            }
        }

        // 都匹配不上则走原有实现
        initBase()
        logD(TAG, lpparam.packageName, "load nothing, so load old hook")
    }

    @JvmOverloads
    protected fun setDimensionPixelSizeFormPrefs(key: String, defaultValue: Int = 0): MethodHook {
        return object : MethodHook() {
            override fun before(param: MethodHookParam) {
                param.result = DisplayUtils.dp2px(
                    mPrefsMap.getInt(key, defaultValue).toFloat()
                )
            }
        }
    }

    companion object {
        const val DEVICE_CONFIG_OLD = "com.miui.home.launcher.DeviceConfig"
        const val DEVICE_CONFIG_NEW = "com.miui.home.common.device.DeviceConfigs"
        const val GRID_CONFIG_OLD = "com.miui.home.launcher.GridConfig"
        const val GRID_CONFIG_NEW = "com.miui.home.common.gridconfig.GridConfig"
    }
}
