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

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHooks
import org.luckypray.dexkit.query.enums.StringMatchType
import java.lang.reflect.Method

object AutoUpdateDialog : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        find1
        find2
        return true
    }

    // 弹自动检测对话框的函数
    private val find1 by lazy<List<Method>> {
        requiredMemberList("AutoUpdateDialog1") {
            it.findMethod {
                matcher {
                    addCaller {
                        addUsingString("isShowAutoSetDialog", StringMatchType.Contains)
                    }
                    paramTypes("boolean", "boolean")
                }
            }
        }
    }

    // 弹移动网络下载提示对话框
    private val find2 by lazy<List<Method>> {
        requiredMemberList("AutoUpdateDialog2") {
            it.findMethod {
                matcher {
                    addCaller {
                        addUsingString("isShowMobileDownloadDialog", StringMatchType.Contains)
                    }
                    paramTypes("long", "int")
                }
            }
        }
    }

    override fun init() {
        (find1 + find2).createHooks {
            replace { null }
        }
    }
}
