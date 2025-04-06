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
package com.sevtinge.hyperceiler.hook.module.hook.misound

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.utils.setObjectField

//from SetoHook by SetoSkins
object IncreaseSamplingRate : BaseHook() {
    override fun init() {
        try {
            loadClass("com.miui.misound.EqualizerView").methodFinder()
                .first().createHook {
                    before {
                        it.thisObject.setObjectField("l", "0x2ee00")
                    }
                }
        } catch (e: Exception) {
           logW(TAG, this.lpparam.packageName, "hook com.miui.misound.EqualizerView.l failed by ", e)
        }
        try {
        loadClass("com.miui.misound.mihearingassist.h").methodFinder()
            .first().createHook {
                before {
                    it.thisObject.setObjectField("a", "0x2ee00")
                }
            }
        } catch (e: Exception) {
            logW(TAG, this.lpparam.packageName, "hook com.miui.misound.mihearingassist.h.a failed by ", e)
        }
        try {
        loadClass("com.miui.misound.mihearingassist.h").methodFinder()
            .filterByName("b")
            .first().createHook {
                before {
                    it.args[1] = "0x2ee00"
                }
            }
        } catch (e: Exception) {
            logW(TAG, this.lpparam.packageName, "hook com.miui.misound.mihearingassist.h.b failed by ", e)
        }
        try {
        loadClass("com.miui.misound.soundid.controller.AudioTrackController").methodFinder()
            .filterByParamCount(2)
            .first().createHook {
                before {
                    it.args[0] = "0x2ee00"
                }
            }
        } catch (e: Exception) {
            logW(TAG, this.lpparam.packageName, "hook com.miui.misound.soundid.controller.AudioTrackController with paramCount == 2 failed by ", e)
        }
        try {
        loadClass("com.miui.misound.soundid.controller.AudioTrackController").methodFinder()
            .filterByParamCount(3)
            .first().createHook {
                before {
                    it.args[0] = "0x2ee00"
                }
            }
        } catch (e: Exception) {
            logW(TAG, this.lpparam.packageName, "hook com.miui.misound.soundid.controller.AudioTrackController with paramCount == 3 failed by ", e)
        }
        try {
        loadClass("miuix.media.Mp3Encoder").methodFinder()
            .first().createHook {
                before {
                    it.thisObject.setObjectField("DEFAULT_SAMPLE_RATE", "0x2ee00")
                }
            }
        } catch (e: Exception) {
            logW(TAG, this.lpparam.packageName, "hook miuix.media.Mp3Encoder.DEFAULT_SAMPLE_RATE failed by ", e)
        }
        try {
        loadClass("com.miui.misound.mihearingassist.h").methodFinder()
            .filterByName("b")
            .first().createHook {
                before {
                    it.args[6] = "0x2ee00"
                }
            }
        } catch (e: Exception) {
            logW(TAG, this.lpparam.packageName, "hook com.miui.misound.mihearingassist.h.b failed by ", e)
        }
    }
}
