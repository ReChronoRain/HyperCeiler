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