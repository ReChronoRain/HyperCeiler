package com.sevtinge.cemiuiler.module.hook.securitycenter.app;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.LogUtils;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedHelpers;

public class AppDefaultSort extends BaseHook {

    Class<?> mAppManagerCls;
    String fragCls = null;

    @Override
    public void init() {

        mAppManagerCls = findClassIfExists("com.miui.appmanager.AppManagerMainActivity");

        findAndHookMethod(mAppManagerCls, "onCreate", Bundle.class, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.args[0] = checkBundle((Context) param.thisObject, (Bundle) param.args[0]);
                Class<?> mFragXCls = findClassIfExists("androidx.fragment.app.Fragment");
                Field[] fields = param.thisObject.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (Fragment.class.isAssignableFrom(field.getType()) || (mFragXCls != null && mFragXCls.isAssignableFrom(field.getType()))) {
                        fragCls = field.getType().getCanonicalName();
                        break;
                    }
                }
                if (fragCls != null) {
                    hookAllMethods(fragCls, "onActivityCreated", new MethodHook() {
                        @Override
                        protected void before(final MethodHookParam param) throws Throwable {
                            try {
                                param.args[0] = checkBundle((Context) XposedHelpers.callMethod(param.thisObject, "getContext"), (Bundle) param.args[0]);
                            } catch (Throwable t) {
                                LogUtils.log("AppDefaultSortHook", t);
                            }
                        }
                    });
                }
            }
        });
    }

    public static Bundle checkBundle(Context context, Bundle bundle) {
        if (context == null) {
            LogUtils.log("AppDefaultSort" + "Context is null!");
            return null;
        }
        if (bundle == null) bundle = new Bundle();
        int order = Integer.parseInt(PrefsUtils.getSharedStringPrefs(context, "prefs_key_security_center_app_default_sort", "0"));
        bundle.putInt("current_sory_type", order - 1);
        bundle.putInt("current_sort_type", order - 1);
        return bundle;
    }
}
