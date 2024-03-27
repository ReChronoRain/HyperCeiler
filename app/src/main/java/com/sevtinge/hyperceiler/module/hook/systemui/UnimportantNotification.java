package com.sevtinge.hyperceiler.module.hook.systemui;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;

public class UnimportantNotification extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.systemui.statusbar.notification.collection.coordinator.FoldCoordinator$shadeExpansionListener$1",
                "onPanelExpansionChanged", "com.android.systemui.shade.ShadeExpansionChangeEvent",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        Object FoldCoordinator = XposedHelpers.getObjectField(param.thisObject, "this$0");
                        XposedHelpers.setObjectField(FoldCoordinator, "mPendingNotifications", new ArrayList<>());
                    }
                }
        );

        findAndHookMethod("com.android.systemui.statusbar.notification.collection.coordinator.FoldCoordinator",
                "access$shouldIgnoreEntry",
                "com.android.systemui.statusbar.notification.collection.coordinator.FoldCoordinator",
                "com.android.systemui.statusbar.notification.collection.NotificationEntry",
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        // Object mSbn = XposedHelpers.getObjectField(param.args[1], "mSbn");
                        // String getPackageName = (String) XposedHelpers.callMethod(mSbn, "getPackageName");
                        // logE(TAG, "after: " + param.getResult() + " pkg: " + getPackageName);
                        param.setResult(true);
                    }
                }
        );
    }
}
