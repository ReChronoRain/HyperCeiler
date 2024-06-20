package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import static de.robv.android.xposed.XposedHelpers.setBooleanField;

import android.app.admin.DevicePolicyManager;
import android.graphics.drawable.Drawable;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DisableDeviceManaged extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod(DevicePolicyManager.class, "isDeviceManaged", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
        findAndHookMethod("com.android.systemui.statusbar.policy.SecurityControllerImpl", "isDeviceManaged", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
        findAndHookMethod("com.android.systemui.statusbar.policy.SecurityControllerImpl", "hasCACertInCurrentUser", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
        findAndHookMethod("com.android.systemui.statusbar.policy.SecurityControllerImpl", "hasCACertInWorkProfile", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(false);
            }
        });
        findAndHookConstructor("com.android.systemui.security.data.model.SecurityModel", boolean.class, boolean.class, boolean.class, boolean.class, String.class, String.class, boolean.class, boolean.class, String.class, String.class, boolean.class, boolean.class, boolean.class, Drawable.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.args[0] = false;
                param.args[10] = false;
                param.args[11] = false;
                setBooleanField(param.thisObject, "isDeviceManaged", false);
                setBooleanField(param.thisObject, "hasCACertInCurrentUser", false);
                setBooleanField(param.thisObject, "hasCACertInWorkProfile", false);
            }
        });
    }
}
