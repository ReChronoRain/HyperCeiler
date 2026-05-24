package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;

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
                param.setResult(param.getArgs()[0]);
            }
        });

        findAndHookMethod("com.android.server.pm.ComputerEngine", "queryIntentActivitiesInternal", Intent.class, String.class, long.class, long.class, int.class, int.class, int.class, boolean.class, boolean.class, new IMethodHook() {
                @Override
                public void after(HookParam param) {

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
}
