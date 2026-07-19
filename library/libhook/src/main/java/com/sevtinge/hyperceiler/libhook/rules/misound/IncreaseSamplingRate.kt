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
package com.sevtinge.hyperceiler.libhook.rules.misound

import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

//from SetoHook by SetoSkins
object IncreaseSamplingRate : BaseHook() {
    override fun init() {
        try {
            loadClass("com.miui.misound.EqualizerView").methods.first().createHook {
                    before {
                        it.thisObject.setObjectField("l", "0x2ee00")
                    }
                }
        } catch (e: Exception) {
            XposedLog.e(
                TAG,
                this.lpparam.packageName,
                "hook com.miui.misound.EqualizerView.l failed by ",
                e
            )
        }
        try {
            loadClass("com.miui.misound.mihearingassist.h").methods.first().createHook {
                    before {
                        it.thisObject.setObjectField("a", "0x2ee00")
                    }
                }
        } catch (e: Exception) {
            XposedLog.e(
                TAG,
                this.lpparam.packageName,
                "hook com.miui.misound.mihearingassist.h.a failed by ",
                e
            )
        }
        try {
            loadClass("com.miui.misound.mihearingassist.h").findMethod { name("b") }.createHook {
                    before {
                        it.args[1] = "0x2ee00"
                    }
                }
        } catch (e: Exception) {
            XposedLog.e(
                TAG,
                this.lpparam.packageName,
                "hook com.miui.misound.mihearingassist.h.b failed by ",
                e
            )
        }
        try {
            loadClass("com.miui.misound.soundid.controller.AudioTrackController").findMethod { paramCount(2) }.createHook {
                    before {
                        it.args[0] = "0x2ee00"
                    }
                }
        } catch (e: Exception) {
            XposedLog.e(
                TAG,
                this.lpparam.packageName,
                "hook com.miui.misound.soundid.controller.AudioTrackController with paramCount == 2 failed by ",
                e
            )
        }
        try {
            loadClass("com.miui.misound.soundid.controller.AudioTrackController").findMethod { paramCount(3) }.createHook {
                    before {
                        it.args[0] = "0x2ee00"
                    }
                }
        } catch (e: Exception) {
            XposedLog.e(
                TAG,
                this.lpparam.packageName,
                "hook com.miui.misound.soundid.controller.AudioTrackController with paramCount == 3 failed by ",
                e
            )
        }
        try {
            loadClass("miuix.media.Mp3Encoder").methods.first().createHook {
                    before {
                        it.thisObject.setObjectField("DEFAULT_SAMPLE_RATE", "0x2ee00")
                    }
                }
        } catch (e: Exception) {
            XposedLog.e(
                TAG,
                this.lpparam.packageName,
                "hook miuix.media.Mp3Encoder.DEFAULT_SAMPLE_RATE failed by ",
                e
            )
        }
        try {
            loadClass("com.miui.misound.mihearingassist.h").findMethod { name("b") }.createHook {
                    before {
                        it.args[6] = "0x2ee00"
                    }
                }
        } catch (e: Exception) {
            XposedLog.e(
                TAG,
                this.lpparam.packageName,
                "hook com.miui.misound.mihearingassist.h.b failed by ",
                e
            )
        }
    }
}
