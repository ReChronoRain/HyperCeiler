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

package com.sevtinge.hyperceiler.libhook.rules.securitycenter.other

import android.app.Activity
import android.os.Bundle

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.beforeHookMethod

object RemoveSIMLockSuccessDialog : BaseHook() {
    private const val SUCCESS_DIALOG = "com.miui.simlock.activity.SuccessDialogActivity"

    @Throws(NoSuchMethodException::class)
    override fun init() {
        // SIM lock only exists in the Chinese Security Center build; the whole
        // com.miui.simlock package is absent from the global one
        // (MIUISecurityCenterGlobal). Hooking it directly throws ClassNotFoundError and
        // leaves a meaningless "Hook Failed" in the log, so check the class first.
        if (findClassIfExists(SUCCESS_DIALOG) == null) {
            XposedLog.d(TAG, packageName, "$SUCCESS_DIALOG not present, skip (non-CN SecurityCenter)")
            return
        }

        SUCCESS_DIALOG.beforeHookMethod("onCreate", Bundle::class.java) { param ->
            (param.thisObject as Activity).finish()
        }
    }
}
