package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

public class NativeFilePicker extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.server.wm.ActivityTaskManagerServiceImpl", "mayReferToFileExplore", Intent.class, String.class, new IMethodHook() {
            @Override
            public void before(HookParam param) throws Throwable {
                Intent intent = (Intent) param.getArgs()[0];
                if (shouldUseNativePicker(intent)) {
                    param.setResult(intent);
                }
            }
        });

        findAndHookMethod("com.android.server.pm.ComputerEngine", "queryIntentActivitiesInternal", Intent.class, String.class, long.class, long.class, int.class, int.class, int.class, boolean.class, boolean.class, new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    Intent intent = (Intent) param.getArgs()[0];
                    if (!shouldUseNativePicker(intent)) return;

                    List<ResolveInfo> list = (List<ResolveInfo>) param.getResult();
                    if (list == null) return;

                    list.removeIf(r -> {
                        ActivityInfo ai = r.activityInfo;
                        return ai != null && "com.android.photopicker.hyper.HyperMainActivity".equals(ai.name);
                    });
                }
            }
        );
    }

    /**
     * 判断是否应该使用原生选择器
     * 根据用户的两个独立开关设置和 Intent 类型来决定
     */
    private boolean shouldUseNativePicker(Intent intent) {
        if (intent == null) return false;

        String action = intent.getAction();
        String type = intent.getType();

        // 判断是否为照片/视频选择
        boolean isPhotoPicker = isPhotoPickerIntent(action, type);
        // 判断是否为文件选择
        boolean isFilePicker = isFilePickerIntent(action, type);

        // 照片选择：检查「使用原生照片管理器」开关
        if (isPhotoPicker) {
            return PrefsBridge.getBoolean("system_framework_native_photo_picker");
        }
        // 文件选择：检查「使用原生文件管理器」开关
        if (isFilePicker) {
            return PrefsBridge.getBoolean("system_framework_native_file_picker_only");
        }

        return false;
    }

    /**
     * 判断 Intent 是否用于照片/视频选择
     */
    private boolean isPhotoPickerIntent(String action, String type) {
        // 照片选择的典型 Action
        boolean isPhotoAction = Intent.ACTION_GET_CONTENT.equals(action)
            || "android.provider.action.PICK_IMAGES".equals(action)  // MediaStore.ACTION_PICK_IMAGES
            || "android.intent.action.PICK".equals(action);

        // 照片/视频类型
        boolean isPhotoType = type != null && (
            type.startsWith("image/")
                || type.startsWith("video/")
                || type.equals("image/*")
                || type.equals("video/*")
        );

        // 如果是照片相关 Action 且类型为图片/视频，或没有指定类型但 Action 是照片选择
        return isPhotoAction && (isPhotoType || type == null || type.equals("*/*"));
    }

    /**
     * 判断 Intent 是否用于文件选择
     */
    private boolean isFilePickerIntent(String action, String type) {
        // 文件选择的典型 Action
        boolean isFileAction = Intent.ACTION_OPEN_DOCUMENT.equals(action)
            || Intent.ACTION_CREATE_DOCUMENT.equals(action)
            || Intent.ACTION_OPEN_DOCUMENT_TREE.equals(action);

        // 如果 type 是通用类型或不限制类型，判定为文件选择
        boolean isGenericType = type == null
            || type.equals("*/*")
            || (!type.startsWith("image/") && !type.startsWith("video/"));

        return isFileAction && isGenericType;
    }
}
