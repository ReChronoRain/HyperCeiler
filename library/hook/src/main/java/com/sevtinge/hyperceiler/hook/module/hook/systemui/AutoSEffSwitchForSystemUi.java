package com.sevtinge.hyperceiler.hook.module.hook.systemui;

import static com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils.logW;
import static com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils.logE;
import static com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils.logI;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;

import com.sevtinge.hyperceiler.hook.IEffectInfo;

import com.hchen.hooktool.BaseHC;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.tool.additional.SystemPropTool;

import java.util.function.Supplier;

public class AutoSEffSwitchForSystemUi extends BaseHC {
    private static final String TAG = "AutoSEffSwitchForSystemUi";
    private Context mContext;
    private boolean isInit = false;
    private static IEffectInfo mIEffectInfo;

    @Override
    protected void init() {
        if (isSupportFW())
            onSupportFW();
        else
            onNotSupportFW();
    }

    public static boolean isSupportFW() {
        return SystemPropTool.getProp("ro.vendor.audio.fweffect", false);
    }

    @Override
    protected void onApplicationAfter(Context context) {
        mContext = context;

        Intent intent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) return;
        Bundle bundle = intent.getBundleExtra("effect_info");
        if (bundle == null) return;
        mIEffectInfo = IEffectInfo.Stub.asInterface(bundle.getBinder("effect_info"));
        logI(TAG, "onApplicationAfter: EffectInfoService: " + mIEffectInfo);
    }

    public static boolean getEarPhoneStateFinal() {
        if (mIEffectInfo != null) {
            try {
                return mIEffectInfo.isEarphoneConnection();
            } catch (RemoteException e) {
                logE(TAG, e);
                return false;
            }
        }
        logW(TAG, "getEarPhoneStateFinal: mIEffectInfo is null!!");
        return false;
    }

    private void onSupportFW() {
        hookMethod("android.media.audiofx.AudioEffectCenter",
                "setEffectActive",
                String.class, boolean.class,
                new IHook() {
                    @Override
                    public void before() {
                        if (getEarPhoneStateFinal()) {
                            logI(TAG, "earphone is connection, skip set effect: " + getArgs(0) + "!!");
                            returnNull();
                        }
                    }
                }
        );
    }

    private void onNotSupportFW() {
        hookMethod("com.android.systemui.shared.plugins.PluginInstance$PluginFactory",
                "createPluginContext",
                new IHook() {
                    @Override
                    public void after() {
                        if (isInit) return;
                        Supplier<?> mClassLoaderFactory = (Supplier<?>) getThisField("mClassLoaderFactory");
                        load((ClassLoader) mClassLoaderFactory.get());
                        isInit = true;
                    }
                }
        );
    }

    private void load(ClassLoader classLoader) {
        hookMethod("miui.systemui.quicksettings.soundeffect.DolbyAtomsSoundEffectTile",
                classLoader,
                "handleClick",
                new IHook() {
                    @Override
                    public void before() {
                        if (getEarPhoneStateFinal()) {
                            logI(TAG, "earphone is connection, skip set effect: " + getArgs(0) + "!!");
                            returnNull();
                        }
                    }
                }
        );
    }
}
