package com.sevtinge.hyperceiler.module.hook.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DisableTransparent extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        // from https://www.coolapk.com/feed/52893204?shareKey=YTA3MTRkZGJmYTJmNjVlNmI4MTY~&shareUid=1499664&shareFrom=com.coolapk.app_5.3
        String methodName;
        if (isMoreHyperOSVersion(1f)) methodName = "isTransparent";
        else methodName = "isTransparentMode";
        findAndHookMethod("com.android.systemui.statusbar.notification.row.NotificationContentInflaterInjector", methodName, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
    }
}
