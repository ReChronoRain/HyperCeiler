package com.sevtinge.cemiuiler.module.hook.systemui;

import android.content.Context;
import android.os.Handler;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.PrefsUtils;
import com.sevtinge.cemiuiler.utils.log.XposedLogUtils;

public class MonetThemeOverlay extends BaseHook {

    Class<?> THEME_CLASS_AOSP;
    Context mContext;
    Handler mHandler;

    @Override
    public void init() {
        THEME_CLASS_AOSP = findClassIfExists("com.android.systemui.theme.ThemeOverlayController");

        hookAllConstructors(THEME_CLASS_AOSP, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                mContext = (Context) param.args[0];
                /*mHandler = (Handler) param.args[2];*/
            }
        });

        hookAllMethods(THEME_CLASS_AOSP, "getOverlay", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                mHandler = new Handler(mContext.getMainLooper());
                new PrefsUtils.SharedPrefsObserver(mContext, mHandler, "prefs_key_system_ui_monet_overlay_custom_color", -1) {
                    @Override
                    public void onChange(String name, int defValue) {
                        mPrefsMap.put(name, PrefsUtils.getSharedIntPrefs(mContext, name, defValue));
                        XposedLogUtils.INSTANCE.logI(TAG, name + "： " + PrefsUtils.getSharedIntPrefs(mContext, name, defValue));
                    }
                };

                param.args[0] = mPrefsMap.getInt("system_ui_monet_overlay_custom_color", -1);
            }
        });
    }
}
