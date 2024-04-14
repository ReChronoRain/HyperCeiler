package com.sevtinge.hyperceiler.module.hook.demo;

import static com.sevtinge.hyperceiler.module.base.tool.OtherTool.getModuleRes;

import android.content.Context;
import android.content.res.Resources;
import android.widget.Toast;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class ToastTest extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.hchen.demo.MainActivity", "makeToast",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        try {
                            Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "context");
                            Resources mAPP = getModuleRes(context);
                            Toast.makeText(context, R.string.settings,
                                    Toast.LENGTH_LONG).show();
                            param.setResult(null);
                            mResHook.setObjectReplacement("com.hchen.demo",
                                    "string", "test_toast", "Hook");
                        } catch (Throwable e) {
                            logE(TAG, "toast: " + e);
                        }
                        // param.setResult(null);
                    }
                }
        );
    }
}
