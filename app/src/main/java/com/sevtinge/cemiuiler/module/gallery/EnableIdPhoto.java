package com.sevtinge.cemiuiler.module.gallery;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XC_MethodHook;

public class EnableIdPhoto extends BaseHook {
        @Override
        public void init() {
            findAndHookMethod("com.miui.mediaeditor.api.MediaEditorApiHelper", "isIDPhotoAvailable", new BaseHook.MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
            findAndHookMethod("com.miui.gallery.domain.IDPhotoEntranceUtils", "isDeviceSupportIDPhoto", new BaseHook.MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
            findAndHookMethod("com.miui.gallery.domain.IDPhotoEntranceUtils", "getIdType", new BaseHook.MethodHook() {
                @Override
                protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                    param.setResult(2);
                }
            });
        }
    }



