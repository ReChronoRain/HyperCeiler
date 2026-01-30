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
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.network;

import android.os.Message;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class NetworkSpeedSpacing extends BaseHook {

    private static final int MSG_UPDATE_NETWORK_SPEED = 200001;
    private static final int DEFAULT_INTERVAL = 40;
    private static final long ORIGINAL_INTERVAL = 4000L;

    @Override
    public void init() {
        if (tryHookPostUpdateMethod()) return;
        if (tryHookHandlerClass("com.android.systemui.statusbar.policy.NetworkSpeedController$4")) return;
        tryHookHandlerClass("com.android.systemui.statusbar.policy.NetworkSpeedController$2");
    }

    private boolean tryHookPostUpdateMethod() {
        try {
            findClass("com.android.systemui.statusbar.policy.NetworkSpeedController")
                .getDeclaredMethod("postUpdateNetworkSpeedDelay", long.class);

            findAndHookMethod("com.android.systemui.statusbar.policy.NetworkSpeedController",
                "postUpdateNetworkSpeedDelay", long.class, new PostUpdateHook());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private boolean tryHookHandlerClass(String className) {
        try {
            findAndHookMethod(className, "handleMessage", Message.class, new HandleMessageHook());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private class PostUpdateHook implements IMethodHook {
        @Override
        public void before(BeforeHookParam param) {
            long interval = (long) param.getArgs()[0];
            if (interval == ORIGINAL_INTERVAL) {
                param.getArgs()[0] = getCustomInterval();
            }
        }
    }

    private class HandleMessageHook implements IMethodHook {
        @Override
        public void after(AfterHookParam param) {
            Message message = (Message) param.getArgs()[0];
            if (message.what != MSG_UPDATE_NETWORK_SPEED) return;

            Object controller = getObjectField(param.getThisObject(), "this$0");
            Object bgHandler = getObjectField(controller, "mBgHandler");
            scheduleNextUpdate(bgHandler);
        }
    }

    private void scheduleNextUpdate(Object handler) {
        callMethod(handler, "removeMessages", MSG_UPDATE_NETWORK_SPEED);
        callMethod(handler, "sendEmptyMessageDelayed", MSG_UPDATE_NETWORK_SPEED, getCustomInterval());
    }

    private long getCustomInterval() {
        return mPrefsMap.getInt("system_ui_statusbar_network_speed_update_spacings", DEFAULT_INTERVAL) * 100L;
    }
}

