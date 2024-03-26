package com.sevtinge.hyperceiler.module.hook.systemui;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class UnlockCustomActions extends BaseHook {

    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.systemui.media.controls.pipeline.MediaDataManager$createActionsFromState$customActions$1",
                "invoke", Object.class
                , new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        Object INSTANCE = XposedHelpers.getStaticObjectField(
                                findClassIfExists("com.android.systemui.statusbar.notification.NotificationSettingsManager$Holder"),
                                "INSTANCE");
                        XposedHelpers.setObjectField(INSTANCE, "mHiddenCustomActionsList", new ArrayList<>());
                    }

                }
        );
    }
}
