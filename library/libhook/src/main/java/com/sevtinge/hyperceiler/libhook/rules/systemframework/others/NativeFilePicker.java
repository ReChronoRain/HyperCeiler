package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.util.List;
import java.util.ArrayList;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class NativeFilePicker extends BaseHook {
    private static final String ACTION_PICK_IMAGES = "android.provider.action.PICK_IMAGES";
    private static final String HYPER_PHOTO_PICKER = "com.android.photopicker.hyper.HyperMainActivity";
    private static final String NATIVE_PHOTO_PICKER_PACKAGE = "com.android.providers.media.module";
    private static final String NATIVE_PHOTO_PICKER_ACTIVITY =
        "com.android.providers.media.photopicker.PhotoPickerActivity";

    @Override
    public void init() {
        // OS3 resolves PICK_IMAGES to the HyperOS activity before the normal
        // resolver can select the AOSP media-provider picker. Rewrite this
        // exact action at the system-server entry point so the result returns
        // directly to the requesting app.
        findAndHookMethod("com.android.server.wm.ActivityTaskManagerServiceImpl", "hookStartActivity", Intent.class, String.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                Intent intent = (Intent) param.getArgs()[0];
                if (intent != null
                    && ACTION_PICK_IMAGES.equals(intent.getAction())
                    && (intent.getComponent() == null
                        || HYPER_PHOTO_PICKER.equals(intent.getComponent().getClassName()))) {
                    Intent nativeIntent = new Intent(intent);
                    nativeIntent.setComponent(new ComponentName(
                        NATIVE_PHOTO_PICKER_PACKAGE,
                        NATIVE_PHOTO_PICKER_ACTIVITY
                    ));
                    param.setResult(nativeIntent);
                    return;
                }
                param.setResult(intent);
            }
        });

        findAndHookMethod("com.android.server.wm.ActivityTaskManagerServiceImpl", "mayReferToFileExplore", Intent.class, String.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(param.getArgs()[0]);
            }
        });

        hookQuery("com.android.server.pm.ComputerEngine", Intent.class, String.class, long.class, int.class);
        hookQuery("com.android.server.pm.ComputerEngine", Intent.class, String.class, long.class, int.class, int.class);
        hookQuery("com.android.server.pm.ComputerEngine", Intent.class, String.class, long.class, long.class, int.class, int.class, int.class, boolean.class, boolean.class);
    }

    private void hookQuery(String className, Object... parameterTypes) {
        Object[] args = new Object[parameterTypes.length + 1];
        System.arraycopy(parameterTypes, 0, args, 0, parameterTypes.length);
        args[parameterTypes.length] = new IMethodHook() {
            @Override
            @SuppressWarnings("unchecked")
            public void after(HookParam param) {
                Object result = param.getResult();
                if (!(result instanceof List<?> list)) return;
                ArrayList<Object> filtered = new ArrayList<>(list.size());
                for (Object item : list) {
                    if (!(item instanceof ResolveInfo resolveInfo)
                        || resolveInfo.activityInfo == null
                        || !"com.android.photopicker.hyper.HyperMainActivity".equals(resolveInfo.activityInfo.name)) {
                        filtered.add(item);
                    }
                }
                param.setResult(filtered);
            }
        };
        findAndHookMethod(className, "queryIntentActivitiesInternal", args);
    }
}
