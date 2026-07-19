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
package com.sevtinge.hyperceiler.libhook.rules.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class StickyFloatingWindowsForHome extends BaseHook {
    private static final String STATE_RECENTS_CONTAINER =
        "StickyFloatingWindowsForHome.recentsContainer";
    private final Map<Object, BroadcastReceiver> mDismissReceivers =
        Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public void init() {
        Object restoredRecents = getHotReloadRuntimeState(
            STATE_RECENTS_CONTAINER, Object.class
        );
        if (restoredRecents != null) {
            try {
                Context context = (Context) callMethod(restoredRecents, "getContext");
                if (context != null) {
                    registerDismissReceiver(restoredRecents, context);
                }
            } catch (Throwable t) {
                XposedLog.w(TAG, getPackageName(), "Failed to restore recents receiver", t);
            }
        }

        findAndHookMethod("com.miui.home.recents.views.RecentsContainer", "onAttachedToWindow", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                Object recentsContainer = param.getThisObject();
                Context context = (Context) callMethod(recentsContainer, "getContext");
                registerDismissReceiver(recentsContainer, context);
            }
        });
    }

    private void registerDismissReceiver(Object recentsContainer, Context context) {
        if (context == null) return;
        synchronized (mDismissReceivers) {
            if (mDismissReceivers.containsKey(recentsContainer)) return;
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context receiverContext, Intent intent) {
                    try {
                        String pkgName = intent.getStringExtra("package");
                        if (pkgName != null) {
                            callMethod(
                                recentsContainer,
                                "dismissRecentsToLaunchTargetTaskOrHome",
                                pkgName,
                                true
                            );
                        }
                    } catch (Throwable t) {
                        XposedLog.w(TAG, getPackageName(), t);
                    }
                }
            };
            context.registerReceiver(
                receiver,
                new IntentFilter(ACTION_PREFIX + "dismissRecentsWhenFreeWindowOpen"),
                Context.RECEIVER_EXPORTED
            );
            mDismissReceivers.put(recentsContainer, receiver);
            registerReceiverHotReloadCleanup(context, receiver);
            putHotReloadRuntimeState(STATE_RECENTS_CONTAINER, recentsContainer);
        }
    }
}
