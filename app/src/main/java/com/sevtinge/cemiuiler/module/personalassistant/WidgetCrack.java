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
        mPickerDetailResponseWrapper = findClassIfExists("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponseWrapper");
        mPickerDetailResponse = findClassIfExists("com.miui.personalassistant.picker.business.detail.bean.PickerDetailResponse");
        mPickerDetailViewModel = findClassIfExists("com.miui.personalassistant.picker.business.detail.PickerDetailViewModel");
        mPickerDetailUtil = findClassIfExists("com.miui.personalassistant.picker.business.detail.utils.PickerDetailUtil");
        mPickerDetailActionController = findClassIfExists("com.miui.personalassistant.picker.business.detail.utils.PickerDetailActionController");
        mPickerDetailDownloadManager = findClassIfExists("com.miui.personalassistant.picker.business.detail.utils.PickerDetailDownloadManager$Companion");


        findAndHookMethod(mPickerDetailViewModel, "isCanDirectAddMaMl", int.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });

        findAndHookMethod(mPickerDetailViewModel, "mamlDownloading", String.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {

            }
        });

        hookAllMethods(mPickerDetailDownloadManager, "isCanDownload", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });

        hookAllMethods(mPickerDetailUtil, "isCanAutoDownloadMaMl", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });

        hookAllMethods(mPickerDetailResponse, "isPay", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });

        hookAllMethods(mPickerDetailResponse, "isBought", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });


        hookAllMethods(mPickerDetailResponseWrapper, "isPay", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });

        hookAllMethods(mPickerDetailResponseWrapper, "isBought", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });

        findAndHookMethod(mPickerDetailViewModel, "shouldCheckMamlBoughtState", int.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });

        findAndHookMethod(mPickerDetailViewModel, "isTargetPositionMamlPayAndDownloading", int.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });

        findAndHookMethod(mPickerDetailViewModel, "checkIsIndependentProcessWidgetForPosition", int.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });

        findAndHookMethod("com.miui.personalassistant.picker.bean.cards.SuitEntity", "isShowPayLogo", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });

        hookAllMethods("com.miui.maml.widget.edit.MamlutilKt", "themeManagerSupportPaidWidget", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }
}
