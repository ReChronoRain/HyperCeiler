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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.hook.mtb

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.setObjectField

object BypassAuthentication : BaseHook() {
    override fun init() {
        val mModemTestBoxClass = loadClass("com.xiaomi.mtb.activity.ModemTestBoxMainActivity")

        // 在HyperOS上
        runCatching {
            loadClass("com.xiaomi.mtb.XiaoMiServerPermissionCheck").methodFinder()
                .filterByName("getClassErrorString")
                .single().createHook {
                    after {
                        it.result = null
                    }
                }
        }

        runCatching {
            loadClass("com.xiaomi.mtb.XiaoMiServerPermissionCheck").methodFinder()
                .filterByName("updatePermissionClass")
                .single().createHook {
                    after {
                        it.result = 0L
                    }
                }
        }

        runCatching {
            loadClass("com.xiaomi.mtb.MtbApp").methodFinder()
                .filterByName("setMiServerPermissionClass")
                .single().createHook {
                    before {
                        it.args[0] = 0
                    }
                }
        }

        runCatching {
            mModemTestBoxClass.methodFinder()
                .filterByName("updateClass")
                .single().createHook {
                    before {
                        it.args[0] = 0
                        it.thisObject.setObjectField("mClassNet", 0)
                    }
                }
        }

        runCatching {
            mModemTestBoxClass.methodFinder()
                .filterByName("initClassProduct")
                .single().createHook {
                    after {
                        it.thisObject.setObjectField("mClassProduct", 0)
                    }
                }
        }
    }

}
