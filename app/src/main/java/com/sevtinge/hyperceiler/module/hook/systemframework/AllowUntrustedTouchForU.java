package com.sevtinge.hyperceiler.module.hook.systemframework;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class AllowUntrustedTouchForU  extends BaseHook {

    //Class<?> mInputManager;

    @Override
    public void init() {
        //mInputManager = findClassIfExists("android.hardware.input.InputManager");
        //XposedHelpers.setStaticLongField(mInputManager, "BLOCK_UNTRUSTED_TOUCHES", 0x96aec7eL);
        findAndHookMethod("com.android.server.wm.WindowState", "getTouchOcclusionMode", new MethodHook(){
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                param.setResult(2);
            }
        });
    }
}
