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
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class HideStatusBarBeforeScreenshot extends BaseHook {

    private static final String COLLAPSED_STATUS_BAR_CLASS =
        "com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment";
    private static final String PHONE_STATUS_BAR_VIEW_CLASS =
        "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView";
    private static final String ACTION_TAKE_SCREENSHOT = "miui.intent.TAKE_SCREENSHOT";
    private static final String EXTRA_IS_FINISHED = "IsFinished";
    private static final String HOT_RELOAD_VIEW_KEY =
        "HideStatusBarBeforeScreenshot.statusBarView";
    private View mReceiverView;

    @Override
    public void init() {
        View restoredView = getHotReloadRuntimeState(HOT_RELOAD_VIEW_KEY, View.class);
        if (restoredView != null) {
            registerScreenshotReceiver(restoredView);
        }
        if (findClassIfExists(COLLAPSED_STATUS_BAR_CLASS) != null) {
            hookAllMethods(COLLAPSED_STATUS_BAR_CLASS, "onViewCreated", new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    View view = (View) param.getArgs()[0];
                    registerScreenshotReceiver(view);
                }
            });
            return;
        }

        // HyperOS 3 replaced the fragment based status bar with the HomeStatusBar view binder
        // pipeline, so MiuiCollapsedStatusBarFragment no longer exists and the hook above never
        // fired. The fragment root was always the inflated R.layout.status_bar, which is the same
        // MiuiPhoneStatusBarView instance obtained here, so hiding it keeps the old behaviour.
        hookAllMethods(PHONE_STATUS_BAR_VIEW_CLASS, "onFinishInflate", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                registerScreenshotReceiver((View) param.getThisObject());
            }
        });
    }

    private void registerScreenshotReceiver(View view) {
        if (view == null || mReceiverView == view) return;
        Context context = view.getContext();
        if (context == null) return;

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (!ACTION_TAKE_SCREENSHOT.equals(intent.getAction())) return;

                boolean finished = intent.getBooleanExtra(EXTRA_IS_FINISHED, true);
                view.setVisibility(finished ? View.VISIBLE : View.INVISIBLE);
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_TAKE_SCREENSHOT);
        ContextCompat.registerReceiver(context, receiver, filter,ContextCompat.RECEIVER_EXPORTED);
        mReceiverView = view;
        putHotReloadRuntimeState(HOT_RELOAD_VIEW_KEY, view);
        registerReceiverHotReloadCleanup(context, receiver);
        registerHotReloadCleanup(() -> mReceiverView = null);
    }
}
