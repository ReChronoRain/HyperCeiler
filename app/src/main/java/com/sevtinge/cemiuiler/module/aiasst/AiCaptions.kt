package com.sevtinge.cemiuiler.module.aiasst;

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook


class AiCaptions : BaseHook() {

    override fun init() {
        val finder = loadClass("com.xiaomi.aiasst.vision.utils.SupportAiSubtitlesUtils").methodFinder()

        finder.filterByName("isSupportAiSubtitles").first()
            .createHook {
                returnConstant(true)
            }
        finder.filterByName("isSupportJapanKoreaTranslation").first()
            .createHook {
                returnConstant(true)
            }
        finder.filterByName("deviceWhetherSupportOfflineSubtitles").first()
            .createHook {
                returnConstant(true)
            }
        finder.filterByName("isSupportOfflineAiSubtitles").first()
            .createHook {
                returnConstant(true)
            }
    }

}
