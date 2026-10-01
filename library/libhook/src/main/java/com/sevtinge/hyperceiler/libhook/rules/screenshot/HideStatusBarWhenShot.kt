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
package com.sevtinge.hyperceiler.libhook.rules.screenshot

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook

object HideStatusBarWhenShot : BaseHook() {

    private const val ACTION_TAKE_SCREENSHOT = "miui.intent.TAKE_SCREENSHOT"
    private const val EXTRA_IS_FINISHED = "IsFinished"
    private const val STATUS_BAR_SETTLE_DELAY_MS = 80L

    override fun init() {
        loadClass($$"android.provider.Settings$System").findMethod { name("getInt"); parameterTypes(ContentResolver::class.java, String::class.java, Integer.TYPE) }
            .createBeforeHook {
                val touchEnable = it.args[1] as String
                if (touchEnable == "touch_assistant_enabled") {
                    it.result = 1
                    return@createBeforeHook
                }
            }

        if (Build.VERSION.SDK_INT >= 37) {
            val captureDisplay = loadClass("com.miui.screenshot.core.util.DisplayCapture")
                .findMethod {
                    name("captureDisplay")
                    parameterTypes(
                        Context::class.java,
                        Integer.TYPE,
                        Rect::class.java,
                        Array<String>::class.java
                    )
                }

            captureDisplay.createBeforeHook {
                val context = it.args[0] as Context
                sendScreenshotState(context, false)
                SystemClock.sleep(STATUS_BAR_SETTLE_DELAY_MS)
                XposedLog.d(
                    TAG,
                    packageName,
                    "HOOK_STATE=CAPTURE_BARRIER phase=before delayMs=$STATUS_BAR_SETTLE_DELAY_MS"
                )
            }
            captureDisplay.createAfterHook {
                val context = it.args[0] as Context
                sendScreenshotState(context, true)
                XposedLog.d(TAG, packageName, "HOOK_STATE=CAPTURE_BARRIER phase=after")
            }
        }
    }

    private fun sendScreenshotState(context: Context, finished: Boolean) {
        context.sendBroadcast(
            Intent(ACTION_TAKE_SCREENSHOT).apply {
                putExtra(EXTRA_IS_FINISHED, finished)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }
        )
    }
}
