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
package com.sevtinge.hyperceiler.hook.module.hook.mms

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import io.github.kyuubiran.ezxhelper.core.finder.MethodFinder.`-Static`.methodFinder
import io.github.kyuubiran.ezxhelper.xposed.dsl.HookFactory.`-Static`.createAfterHook

// https://github.com/YunZiA/HyperStar/blob/master/app/src/main/java/com/yunzia/hyperstar/hook/app/mms/AutoCopyVerificationCode.kt
object AutoCopyVerificationCode : BaseHook() {
    override fun init() {

        PendingIntent::class.java.methodFinder()
            .filterByName("getActivity")
            .filterByParamTypes(Context::class.java,
                Int::class.java,
                Intent::class.java,
                Int::class.java
            ).first().createAfterHook {
                val intent = it.args[2] as Intent
                val extraText = intent.getStringExtra("extra_text")
                if (extraText != null) {
                    val context = it.args[0] as Context
                    context.copyVerificationCodeToClipboard(extraText)
                    logD(TAG, "New verification code: $extraText")

                }
            }
    }

    @SuppressLint("ServiceCast")
    private fun Context.copyVerificationCodeToClipboard(text: CharSequence) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        val extras = PersistableBundle().apply {
            putBoolean("mms_is_ververification_code", true)
        }
        val clip = ClipData.newPlainText(null, text).apply {
            description.extras = extras
        }
        clipboard.setPrimaryClip(clip)
    }
}
