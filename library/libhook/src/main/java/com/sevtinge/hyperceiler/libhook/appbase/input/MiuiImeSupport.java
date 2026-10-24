/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.appbase.input;

import com.sevtinge.hyperceiler.common.log.ModuleLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

/** Keep the selected third-party IME supported across service/plugin recreation. */
public final class MiuiImeSupport {
    private static final String TAG = "HyperCeiler.ImeBottom";
    private static final String FIELD = "sIsImeSupport";

    private MiuiImeSupport() {}

    public static void install(Class<?> owner) {
        boolean hasField = false;
        try {
            Object original = BaseHook.getStaticObjectField(owner, FIELD);
            BaseHook.setStaticObjectField(owner, FIELD, 1);
            BaseHook.registerHotReloadCleanup(() -> BaseHook.setStaticObjectField(owner, FIELD, original));
            hasField = true;
        } catch (Throwable failure) {
            ModuleLog.w(TAG, owner.getName() + ": support cache unavailable", failure);
        }

        int getters = 0, entries = 0;
        try {
            // onDestroy() resets the plugin's cache to -1. A one-time field write
            // cannot survive that reset, even when loadDex reuses the same loader.
            getters = BaseHook.hookAllMethods(owner, "isImeSupport", new IMethodHook() {
                @Override public void before(HookParam param) { param.setResult(true); }
            }).size();
        } catch (Throwable failure) {
            ModuleLog.w(TAG, owner.getName() + ": support predicate hook unavailable", failure);
        }
        if (hasField) {
            try {
                entries = BaseHook.hookAllMethods(owner, "addMiuiBottomView", new IMethodHook() {
                    private boolean loggedRecovery;
                    private boolean loggedFailure;

                    @Override public void before(HookParam param) {
                        try {
                            // Restore before the entry runs, including builds that
                            // inline the support predicate into this method.
                            if (!Integer.valueOf(1).equals(BaseHook.getStaticObjectField(owner, FIELD))) {
                                BaseHook.setStaticObjectField(owner, FIELD, 1);
                                if (!loggedRecovery) {
                                    loggedRecovery = true;
                                    ModuleLog.i(TAG, owner.getName() + ": restored support after service recreation");
                                }
                            }
                        } catch (Throwable failure) {
                            if (!loggedFailure) {
                                loggedFailure = true;
                                ModuleLog.w(TAG, owner.getName() + ": support cache restore failed", failure);
                            }
                        }
                    }
                }).size();
            } catch (Throwable failure) {
                ModuleLog.w(TAG, owner.getName() + ": bottom-view lifecycle hook unavailable", failure);
            }
        }
        String state = owner.getName() + ": persistent support installed getters=" + getters
            + " entries=" + entries + " cache=" + hasField;
        if (getters == 0 && entries == 0) ModuleLog.w(TAG, state);
        else ModuleLog.i(TAG, state);
    }
}
