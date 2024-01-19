package com.sevtinge.hyperceiler.module.hook.home.folder;

import android.graphics.Rect;
import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class UnlockBlurSupported extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.home.launcher.common.BlurUtilities",
            "isBlurSupported",
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    boolean isDefaultIcon = (boolean) XposedHelpers.callStaticMethod(
                        findClassIfExists("com.miui.home.launcher.DeviceConfig"),
                        "isDefaultIcon");
                    if (!isDefaultIcon)
                        param.setResult(true);
                }
            }
        );

        findAndHookMethod("com.miui.home.launcher.folder.LauncherFolder2x2IconContainer",
            "resolveTopPadding", Rect.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    // Rect rect = (Rect) param.args[0];
                    View view = (View) param.thisObject;
                    XposedHelpers.callMethod(view,
                        "setPadding", 0,
                        XposedHelpers.callMethod(param.thisObject,
                            "getMContainerPaddingTop"), 0, 0);
                    param.setResult(null);
                }
            }
        );
    }
}
