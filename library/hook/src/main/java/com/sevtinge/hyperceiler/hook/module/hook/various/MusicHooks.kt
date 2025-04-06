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
package com.sevtinge.hyperceiler.hook.module.hook.various

import cn.lyric.getter.api.data.*
import cn.lyric.getter.api.data.type.*
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder.`-Static`.constructorFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.hook.module.base.MusicBaseHook

object MusicHooks : MusicBaseHook() {
    override fun init() {
        if (lpparam.packageName == "com.salt.music") {
            val clazz = loadClassOrNull("cn.lyric.getter.api.API")
            clazz?.constructorFinder()?.first()?.createHook {
                before { hookParam ->
                    if ((hookParam.thisObject.objectHelper().getObjectOrNullAs<Int>("API_VERSION")
                            ?: 0) >= 6
                    ) {
                        clazz.methodFinder().first { name == "sendLyric" }.createHook {
                            before { hookParam ->
                                val extra = (hookParam.args[1]).objectHelper()
                                    .getObjectOrNullAs<HashMap<String, Any>>("extra")
                                extra?.put("packageName", "com.salt.music")
                            }
                        }
                    }
                }
            }
            logI(TAG, lpparam.packageName, "LyricGetter API6 Fixed")
        }
    }

    override fun onUpdate(lyricData: LyricData) {
        if (lyricData.type == OperateType.UPDATE){
            val pkgName = lyricData.extraData.packageName
            if (pkgName == context.packageName) {
                try {
                    sendNotification(lyricData.lyric, lyricData.extraData)
                } catch (e: Throwable) {
                    logE(TAG, lpparam.packageName, e)
                }
            }
        }
    }

    override fun onStop() {
        cancelNotification()
    }

}
