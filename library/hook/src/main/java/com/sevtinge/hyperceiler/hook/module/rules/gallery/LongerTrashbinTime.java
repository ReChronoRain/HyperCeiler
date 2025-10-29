package com.sevtinge.hyperceiler.hook.module.rules.gallery;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

public class LongerTrashbinTime extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookConstructor("com.miui.gallery.trash.TrashUtils$UserInfo", String.class, String.class, String.class, long.class, long.class, long.class, long.class, new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.args[3] = 31536000000L;
                param.args[6] = 31536000000L;
            }
        });
    }
}
