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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.hook.various

import cn.lyric.getter.api.data.*
import cn.lyric.getter.api.data.type.*
import com.sevtinge.hyperceiler.module.base.*

object MusicHooks : MusicBaseHook() {
    override fun init() {
    }

    override fun onUpdate(lyricData: LyricData) {
        if (lyricData.type == OperateType.UPDATE){
            val pkgName = lyricData.extraData.packageName
            if (pkgName == context.packageName) {
                try {
                    sendNotification(lyricData.lyric)
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