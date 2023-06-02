package com.sevtinge.cemiuiler.module.personalassistant

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook

class WidgetCrack : BaseHook() {
    override fun init() {
        loadClass("com.miui.maml.widget.edit.MamlutilKt").methodFinder().first {
            name == "themeManagerSupportPaidWidget"
        }.createHook {
            after {
                it.result = false
            }
        }

        loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel").methodFinder().first{
            name == "isCanDirectAddMaMl"
        }.createHook {
            after {
                it.result = true
            }
        }

        loadClass("com.miui.personalassistant.picker.business.detail.utils.PickerDetailDownloadManager\$Companion").methodFinder().first {
            name == "isCanDownload"
        }.createHook {
            before {
                it.result = true
            }
        }

        loadClass("com.miui.personalassistant.picker.business.detail.utils.PickerDetailUtil").methodFinder().first {
            name == "isCanAutoDownloadMaMl"
        }.createHook {
            before {
                it.result = true
            }
        }

        loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponse").methodFinder().first {
            name == "isPay"
        }.createHook {
            before {
                it.result = false
            }
        }

        loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponse").methodFinder().first {
            name == "isBought"
        }.createHook {
            before {
                it.result = true
            }
        }

        loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponseWrapper").methodFinder().first {
            name == "isPay"
        }.createHook {
            before {
                it.result = false
            }
        }

        loadClass("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponseWrapper").methodFinder().first {
            name == "isBought"
        }.createHook {
            before {
                it.result = true
            }
        }

        loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel").methodFinder().first {
            name == "shouldCheckMamlBoughtState"
        }.createHook {
            after {
                it.result = false
            }
        }

        loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel").methodFinder().first {
            name == "isTargetPositionMamlPayAndDownloading"
        }.createHook {
            after {
                it.result = false
            }
        }

        loadClass("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel").methodFinder().first {
            name == "checkIsIndependentProcessWidgetForPosition"
        }.createHook {
            after {
                it.result = true
            }
        }
    }
}
