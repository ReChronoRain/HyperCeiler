package com.sevtinge.cemiuiler.module.hook.personalassistant

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.log.XposedLogUtils

class WidgetCrack : BaseHook() {
    override fun init() {
        try {
            loadClass("com.miui.maml.widget.edit.MamlutilKt").methodFinder().filterByName("themeManagerSupportPaidWidget").first().createHook {
                returnConstant(false)
            }

            loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel").methodFinder().filterByName("isCanDirectAddMaMl").first()
                .createHook {
                    returnConstant(true)
                }

            loadClass("com.miui.personalassistant.picker.business.detail.utils.PickerDetailDownloadManager\$Companion").methodFinder()
                .filterByName("isCanDownload").first().createHook {
                    returnConstant(true)
                }

            loadClass("com.miui.personalassistant.picker.business.detail.utils.PickerDetailUtil").methodFinder().filterByName("isCanAutoDownloadMaMl").first()
                .createHook {
                    returnConstant(true)
                }

            loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponse").methodFinder().filterByName("isPay").first().createHook {
                returnConstant(false)
            }

            loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponse").methodFinder().filterByName("isBought").first()
                .createHook {
                    returnConstant(true)
                }

            loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponseWrapper").methodFinder().filterByName("isPay").first()
                .createHook {
                    returnConstant(false)
                }

            loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponseWrapper").methodFinder().filterByName("isBought").first()
                .createHook {
                    returnConstant(true)
                }

            loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel").methodFinder().filterByName("shouldCheckMamlBoughtState")
                .first().createHook {
                    returnConstant(false)
                }

            loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel").methodFinder()
                .filterByName("isTargetPositionMamlPayAndDownloading").first().createHook {
                    returnConstant(false)
                }

            loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel").methodFinder()
                .filterByName("checkIsIndependentProcessWidgetForPosition").first().createHook {
                    returnConstant(true)
                }
        } catch (t: Throwable) {
            XposedLogUtils.logE(TAG, t)
        }
    }
}
