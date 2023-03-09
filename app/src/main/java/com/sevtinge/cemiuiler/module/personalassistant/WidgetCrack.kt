package com.sevtinge.cemiuiler.module.personalassistant

import android.content.Context
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.*
import com.sevtinge.cemiuiler.module.base.BaseHook

class WidgetCrack : BaseHook() {

    override fun init() {
        EzXHelperInit.setEzClassLoader(lpparam.classLoader)
        findMethod("com.miui.maml.widget.edit.MamlutilKt") {
            name == "themeManagerSupportPaidWidget"
        }.hookAfter {
            it.result = false
        }
        findMethod("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel") {
            name == "isCanDirectAddMaMl"
        }.hookAfter {
            it.result = true
        }
        findMethod("com.miui.personalassistant.picker.business.detail.utils.PickerDetailDownloadManager\$Companion") {
            name == "isCanDownload"
        }.hookBefore {
            it.result = true
        }
        findMethod("com.miui.personalassistant.picker.business.detail.utils.PickerDetailUtil") {
            name == "isCanAutoDownloadMaMl"
        }.hookBefore {
            it.result = true
        }
                findMethod("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponse") {
                    name == "isPay"
                }.hookBefore {
                    it.result = false
                }
                findMethod("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponse") {
                    name == "isBought"
                }.hookBefore {
                    it.result = true
                }
                findMethod("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponseWrapper") {
                    name == "isPay"
                }.hookBefore {
                    it.result = false
                }
                findMethod("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponseWrapper") {
                    name == "isBought"
                }.hookBefore {
                    it.result = true
                }
                findMethod("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel") {
                    name == "shouldCheckMamlBoughtState"
                }.hookAfter {
                    it.result = false
                }
                findMethod("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel") {
                    name == "isTargetPositionMamlPayAndDownloading"
                }.hookAfter {
                    it.result = false
                }
                findMethod("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel") {
                    name == "checkIsIndependentProcessWidgetForPosition"
                }.hookAfter {
                    it.result = true
       }
    }
}
