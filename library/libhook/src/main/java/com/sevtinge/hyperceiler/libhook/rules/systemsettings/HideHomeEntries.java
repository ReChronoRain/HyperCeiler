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
package com.sevtinge.hyperceiler.libhook.rules.systemsettings;

import android.app.Activity;
import android.content.res.Resources;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.util.Iterator;
import java.util.List;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class HideHomeEntries extends BaseHook {
    @Override
    public void init() {
        Class<?> settings = findClassIfExists("com.android.settings.MiuiSettings");
        if (settings == null) return;

        boolean hideUpdater = PrefsBridge.getBoolean("system_settings_hide_system_apps_updater");
        boolean hideZoomRing = PrefsBridge.getBoolean("system_settings_hide_camera_mr");
        findAndHookMethod(settings, "updateHeaderList", List.class, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                hideEntries(param, hideUpdater, hideZoomRing);
            }
        });
    }

    private void hideEntries(HookParam param, boolean hideUpdater, boolean hideZoomRing) {
        if (!(param.getArgs()[0] instanceof List<?> headers)
            || !(param.getThisObject() instanceof Activity activity)) return;

        Resources resources = activity.getResources();
        int updaterId = hideUpdater ? resources.getIdentifier(
            "system_apps_updater", "id", "com.android.settings") : 0;
        int zoomRingId = hideZoomRing ? resources.getIdentifier(
            "camera_mr_settings", "id", "com.android.settings") : 0;
        if (updaterId == 0 && zoomRingId == 0) return;
        removeMatchingHeaders(headers, updaterId, zoomRingId);
    }

    private void removeMatchingHeaders(List<?> headers, int updaterId, int zoomRingId) {
        // Match stable header IDs, not translated labels or whole packages.
        for (Iterator<?> iterator = headers.iterator(); iterator.hasNext();) {
            Object header = iterator.next();
            if (header == null) continue;
            long id = getLongField(header, "id");
            if ((updaterId != 0 && id == updaterId)
                || (zoomRingId != 0 && id == zoomRingId)) {
                iterator.remove();
            }
        }
    }
}
