/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * HyperCeiler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all;

import android.service.notification.StatusBarNotification;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class HidePersistentNotificationIcons extends BaseHook {
    private static final String STATUS_KEYS = "HidePersistentNotificationIcons.lsposedKeys";

    @Override
    @SuppressWarnings("unchecked")
    public void init() {
        Class<?> iconMapper = findClassIfExists("com.android.systemui.statusbar.notification.icon.ui.viewmodel."
            + "NotificationIconContainerStatusBarViewModel$icons$1");
        Class<?> entryClass = findClassIfExists("com.android.systemui.statusbar.notification.collection.NotificationEntry");
        if (iconMapper == null || entryClass == null) return;

        boolean hideGarmin = PrefsBridge.getBoolean("system_ui_status_bar_hide_garmin_icon");
        boolean hideLsposed = PrefsBridge.getBoolean("system_ui_status_bar_hide_lsposed_icon");
        Set<String> restored = getHotReloadRuntimeState(STATUS_KEYS, Set.class);
        Set<String> lsposedKeys = restored != null ? restored : ConcurrentHashMap.newKeySet();
        putHotReloadRuntimeState(STATUS_KEYS, lsposedKeys);

        if (hideLsposed) {
            // Track only LSPosed's channel. Its notification is posted as Android,
            // so filtering the whole package would also hide unrelated system alerts.
            findAndHookMethod(entryClass, "setSbn", StatusBarNotification.class, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    if (!(param.getArgs()[0] instanceof StatusBarNotification sbn)) return;
                    String pkg = sbn.getPackageName();
                    if (("android".equals(pkg) || "org.lsposed.manager".equals(pkg))
                        && "lsposed_status".equals(sbn.getNotification().getChannelId())) {
                        lsposedKeys.add(sbn.getKey());
                    } else {
                        lsposedKeys.remove(sbn.getKey());
                    }
                }
            });
        }

        findAndHookMethod(iconMapper, "invoke", Object.class, Object.class, Object.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                if (!(param.getArgs()[0] instanceof Set<?> icons)) return;
                Set<Object> filtered = null;
                for (Object icon : icons) {
                    if (icon == null) continue;
                    String pkg = (String) getObjectField(icon, "packageName");
                    String key = (String) getObjectField(icon, "notifKey");
                    if ((hideGarmin && "com.garmin.android.apps.connectmobile".equals(pkg))
                        || (hideLsposed && lsposedKeys.contains(key))) {
                        if (filtered == null) filtered = new LinkedHashSet<>(icons);
                        filtered.remove(icon);
                    }
                }
                // Filter before package deduplication and the icon limit. Keep the
                // shared notification set, shade, shelf and foreground service intact.
                if (filtered != null) param.getArgs()[0] = filtered;
            }
        });
    }
}
