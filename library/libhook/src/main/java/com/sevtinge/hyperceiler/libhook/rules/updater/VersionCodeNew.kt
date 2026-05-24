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
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findConstructor
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setStaticObjectField
import org.json.JSONObject
import java.lang.reflect.Method

object VersionCodeNew : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        mBigMethod
        mOSCode
        mOSMethod
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
    private val mOSMethod by lazy<List<Method>> {
        requiredMemberList("VersionCodeNew2") {
            it.findMethod {
                matcher {
                    usingEqStrings("ro.mi.os.version.incremental")
                }
            }
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
        // 原始修改版本名
        findClassIfExists("com.android.updater.Application").findMethod { name("onCreate") }.createBeforeHook {
                if (!TextUtils.isEmpty(mOldVersionCode)) {
                    Build.VERSION::class.java.setStaticObjectField(
                        "INCREMENTAL",
                        "$mVersionCode"
                    )
                }
            }

        // 大版本名字修改
        mBigMethod.createBeforeHook {
            if (!TextUtils.isEmpty(mOldVersionCode)) {
                it.result = mOldVersionCode
            }
        }

        // OS 版本名修改
        mOSMethod.createHooks {
            before {
                if (!TextUtils.isEmpty(mVersionCode)) {
                    it.result = mVersionCode
                }
            }
        }

        // OS 版本修改
        mOSCode.createBeforeHook {
            if (!TextUtils.isEmpty(mVersionCode)) {
                it.result =
                    "${mVersionCode.split(".")[0]}.${mVersionCode.split(".")[1]}.${
                        mVersionCode.split(".")[2]
                    }"
            }
        }

        loadClass("android.os.SystemProperties").findMethod {
            name("get")
            parameterTypes(String::class.java, String::class.java)
        }.createBeforeHook {
            val key = it.args[0] as String?
            if ("persist.sys.xms.version" == key || "ro.mi.xms.version.incremental" == key) {
                if (mXmsVersion != null) it.result = mXmsVersion
            } else if ("ro.mi.os.version.incremental" == key) {
                if (mVersionCode != null) it.result = mVersionCode
            }
        }

        loadClass("com.android.updater.xms.bean.XmsVersionInfo").findConstructor {
            parameterTypes(JSONObject::class.java)
        }.createAfterHook {
            XposedLog.d(TAG, lpparam.packageName, "111 ")
            XposedLog.d(TAG, lpparam.packageName, "111 " + it.thisObject.getObjectField("curVerCode").toString())
        }

    }
}

