package com.sevtinge.cemiuiler.module.personalassistant

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

class WidgetCrack : BaseHook() {
    override fun init() {
        try {
            loadClass("com.miui.maml.widget.edit.MamlutilKt").methodFinder().first {
                name == "themeManagerSupportPaidWidget"
            }.createHook {
                returnConstant(false)
            }

            loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel").methodFinder()
                .first {
                    name == "isCanDirectAddMaMl"
                }.createHook {
                returnConstant(true)
            }

            loadClass("com.miui.personalassistant.picker.business.detail.utils.PickerDetailDownloadManager\$Companion").methodFinder()
                .first {
                    name == "isCanDownload"
                }.createHook {
                    returnConstant(true)
                }

            loadClass("com.miui.personalassistant.picker.business.detail.utils.PickerDetailUtil").methodFinder()
                .first {
                    name == "isCanAutoDownloadMaMl"
                }.createHook {
                returnConstant(true)
            }

            loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponse").methodFinder()
                .first {
                    name == "isPay"
                }.createHook {
                returnConstant(false)
            }

            loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponse").methodFinder()
                .first {
                    name == "isBought"
                }.createHook {
                returnConstant(true)
            }

            loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponseWrapper").methodFinder()
                .first {
                    name == "isPay"
                }.createHook {
                    returnConstant(false)
                }

            loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponseWrapper").methodFinder()
                .first {
                    name == "isBought"
                }.createHook {
                    returnConstant(true)
                }

            loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel").methodFinder()
                .first {
                    name == "shouldCheckMamlBoughtState"
                }.createHook {
                returnConstant(false)
            }

            loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel").methodFinder()
                .first {
                    name == "isTargetPositionMamlPayAndDownloading"
                }.createHook {
                returnConstant(false)
            }

            loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel").methodFinder()
                .first {
                    name == "checkIsIndependentProcessWidgetForPosition"
                }.createHook {
                returnConstant(true)
            }
        } catch (t: Throwable) {
            Log.ex(t)
        }
    }
}
