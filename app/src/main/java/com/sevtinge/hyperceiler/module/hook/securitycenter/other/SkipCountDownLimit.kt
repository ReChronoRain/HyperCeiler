/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.securitycenter.other

import android.os.*
import android.view.*
import com.github.kyuubiran.ezxhelper.ClassLoaderProvider.safeClassLoader
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toMethod
import de.robv.android.xposed.*

/**
 * @author Xzakota & Sevtinge
 */
object SkipCountDownLimit : BaseHook() {

    private val skipCountDownLimitMethod by lazy {
        DexKit.getDexKitBridge("SkipCountDownLimit") {
            it.findMethod {
                matcher {
                    declaredClass("com.miui.permcenter.privacymanager.InterceptBaseFragment")
                    usingNumbers(-1, 0)
                }
            }.single().getMethodInstance(safeClassLoader)
        }.toMethod()
    }

    override fun init() {
        if (!mPrefsMap.getBoolean("security_center_skip_count_down_limit_direct")) {
            loadClass("com.miui.permcenter.privacymanager.model.InterceptBaseActivity").methodFinder()
                .filterByName("onCreate")
                .filterByParamTypes(Bundle::class.java)
                .first().createHook {
                    before { param ->
                        val feat: Bundle.() -> Unit = {
                            // putInt("KEY_STEP_COUNT", 0)
                            putInt("KET_STEP_COUNT", 0)
                            putBoolean("KEY_ALLOW_ENABLE", true)
                        }
                        param.args.first().let {
                            val bundle = it as? Bundle?
                            if (bundle == null) {
                                param.args[0] = Bundle().apply(feat)
                            } else {
                                bundle.feat()
                            }
                        }
                    }
                }
        } else {
            val hookClassName = "com.miui.permcenter.privacymanager.InterceptBaseFragment"
            findAndHookMethod(
                hookClassName,
                "onInflateView",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Bundle::class.java,
                object : MethodHook() {
                    override fun after(param: MethodHookParam?) {
                        XposedHelpers.callMethod(
                            param?.thisObject,
                            skipCountDownLimitMethod.name,
                            true
                        )
                    }
                }
            )

        }
    }
}