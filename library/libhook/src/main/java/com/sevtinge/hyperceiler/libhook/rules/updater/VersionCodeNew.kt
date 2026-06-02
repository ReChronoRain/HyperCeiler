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
package com.sevtinge.hyperceiler.libhook.rules.updater

import android.os.Build
import android.text.TextUtils
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setStaticObjectField
import java.lang.reflect.Method

object VersionCodeNew : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        mBigMethod
        mOSCode
        return true
    }

    private val mBigMethod by lazy<Method> {
        requiredMember("VersionCodeNew1") {
            it.findMethod {
                matcher {
                    usingEqStrings("ro.miui.ui.version.name")
                }
            }.single()
        }
    }

    private val mOSCode by lazy<Method> {
        requiredMember("VersionCodeNew3") {
            it.findMethod {
                matcher {
                    usingEqStrings("ro.mi.os.version.name", "OS")
                }
            }.single()
        }
    }

    private val mOldVersionCode =
        PrefsBridge.getString("various_updater_big_version", "OS2")

    private val mVersionCode =
        PrefsBridge.getString("various_updater_miui_version", "OS2.0.200.0.VOCCNXM")

    private val mXmsVersion =
        PrefsBridge.getString("various_updater_xms_version", "")


    override fun init() {
        // 覆盖 Build.VERSION.INCREMENTAL
        loadClass("com.android.updater.Application").findMethod { name("onCreate") }
            .createBeforeHook {
                if (!TextUtils.isEmpty(mVersionCode)) {
                    Build.VERSION::class.java.setStaticObjectField("INCREMENTAL", mVersionCode)
                }
            }

        // 大版本号
        mBigMethod.createBeforeHook {
            if (!TextUtils.isEmpty(mOldVersionCode) && mOldVersionCode.startsWith("V")) {
                it.result = mOldVersionCode
            }
        }

        // OS 版本修改
        mOSCode.createBeforeHook {
            if (TextUtils.isEmpty(mVersionCode)) return@createBeforeHook
            val sets = mVersionCode.split(".")
            if (sets.size >= 3) {
                it.result = "${sets[0]}.${sets[1]}.${sets[2]}"
            }
        }

        // SOTA 版本修改
        loadClass("android.os.SystemProperties").findMethod {
            name("get")
            parameterTypes(String::class.java, String::class.java)
        }.createBeforeHook {
            val key = it.args[0] as? String ?: return@createBeforeHook
            when (key) {
                "persist.sys.xms.version",
                "ro.mi.xms.version.incremental" -> {
                    if (!TextUtils.isEmpty(mXmsVersion)) it.result = mXmsVersion
                }
                "ro.mi.os.version.incremental" -> {
                    if (!TextUtils.isEmpty(mVersionCode)) it.result = mVersionCode
                }
            }
        }
    }
}
