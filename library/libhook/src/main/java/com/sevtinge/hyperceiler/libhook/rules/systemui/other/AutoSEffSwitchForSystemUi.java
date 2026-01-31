/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.other;

import static com.sevtinge.hyperceiler.libhook.utils.api.PropUtils.getProp;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.BINDER_KEY_EFFECT_INFO;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.PREFS_KEY_LOCK_SELECTION;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;

import com.sevtinge.hyperceiler.libhook.IEffectInfo;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

/**
 * 自动切换音效 - SystemUI 端
 * 拦截 SystemUI 中的音效切换操作
 *
 * @author 焕晨HChen
 */
public class AutoSEffSwitchForSystemUi extends BaseHook {

    private static final String TAG = "AutoSEffSwitchForSystemUi";
    private static final String PROP_FW_EFFECT = "ro.vendor.audio.fweffect";

    private static final AtomicReference<IEffectInfo> sEffectInfoRef = new AtomicReference<>();
    private static final AtomicBoolean sLockSelectionEnabled = new AtomicBoolean(true);

    @Override
    public void init() {
        loadConfig();
        if (isSupportFW()) {
            hookFWEffectCenter();
        }
        initBinderConnection();
    }

    /**
     * 加载配置
     */
    private void loadConfig() {
        sLockSelectionEnabled.set(mPrefsMap.getBoolean(PREFS_KEY_LOCK_SELECTION));
        XposedLog.d(TAG, "Config loaded: lockSelection=" + sLockSelectionEnabled.get());
    }

    /**
     * 检查是否支持 FW 模式
     */
    public static boolean isSupportFW() {
        return getProp(PROP_FW_EFFECT, false);
    }

    /**
     * 获取耳机连接状态
     */
    public static boolean getEarPhoneStateFinal() {
        IEffectInfo effectInfo = sEffectInfoRef.get();
        if (effectInfo != null) {
            try {
                return effectInfo.isEarphoneConnection();
            } catch (RemoteException e) {
                XposedLog.e(TAG, "Failed to get earphone state", e);
            }
        } else {
            XposedLog.w(TAG, "IEffectInfo is null");
        }
        return false;
    }

    /**
     * 是否启用锁定选择
     */
    public static boolean isLockSelectionEnabled() {
        return sLockSelectionEnabled.get();
    }

    /**
     * 判断是否应该阻止音效切换
     * 只有在启用锁定且耳机连接时才阻止
     */
    public static boolean shouldBlockEffectSwitch() {
        return isLockSelectionEnabled() && getEarPhoneStateFinal();
    }

    /**
     * 初始化 Binder 连接
     */
    private void initBinderConnection() {
        runOnApplicationAttach(this::connectToEffectInfoService);
    }

    /**
     * 连接到 EffectInfoService
     */
    private void connectToEffectInfoService(Context context) {
        try {
            Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (intent == null) {
                XposedLog.w(TAG, "Battery intent is null");
                return;
            }

            Bundle bundle = intent.getBundleExtra(BINDER_KEY_EFFECT_INFO);
            if (bundle == null) {
                XposedLog.w(TAG, "Effect info bundle is null");
                return;
            }

            IEffectInfo effectInfo = IEffectInfo.Stub.asInterface(
                bundle.getBinder(BINDER_KEY_EFFECT_INFO)
            );
            sEffectInfoRef.set(effectInfo);

            XposedLog.d(TAG, "Connected to EffectInfoService: " + effectInfo);
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to connect to EffectInfoService", e);
        }
    }

    /**
     * Hook FW 模式下的 AudioEffectCenter
     */
    private void hookFWEffectCenter() {
        findAndHookMethod("android.media.audiofx.AudioEffectCenter",
            "setEffectActive",
            String.class, boolean.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (shouldBlockEffectSwitch()) {
                        String effect = (String) param.getArgs()[0];
                        XposedLog.d(TAG, "Lock enabled and earphone connected, skip setting effect: " + effect);
                        param.setResult(null);
                    }
                }
            }
        );
    }

    /**
     * Hook 非 FW 模式下的音效切换（供外部调用）
     */
    public static void hookNonFWEffectSwitch(ClassLoader classLoader) {
        EzxHelpUtils.findAndHookMethod(
            "miui.systemui.quicksettings.soundeffect.DolbyAtomsSoundEffectTile",
            classLoader,
            "handleClick",
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (shouldBlockEffectSwitch()) {
                        XposedLog.d(TAG, "Lock enabled and earphone connected, skip Dolby tile click");
                        param.setResult(null);
                    }
                }
            }
        );
    }
}
