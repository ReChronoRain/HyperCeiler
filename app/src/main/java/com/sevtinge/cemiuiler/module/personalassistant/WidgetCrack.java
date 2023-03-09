package com.sevtinge.cemiuiler.module.personalassistant;

import android.content.Context;
import com.sevtinge.cemiuiler.module.base.BaseHook;

public class WidgetCrack extends BaseHook {

    Class<?> mPickerDetailResponseWrapper;
    Class<?> mPickerDetailResponse;
    Class<?> mPickerDetailViewModel;
    Class<?> mPickerDetailUtil;
    Class<?> mPickerDetailActionController;
    Class<?> mPickerDetailDownloadManager;


    @Override
    public void init() {
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
