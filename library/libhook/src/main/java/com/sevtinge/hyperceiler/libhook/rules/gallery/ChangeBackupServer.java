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
package com.sevtinge.hyperceiler.libhook.rules.gallery;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;


public class ChangeBackupServer extends BaseHook {
    private static final ThreadLocal<Boolean> sOverrideIsInternational = new ThreadLocal<>();

    @Override
    public void init() {
        int backupServer = PrefsBridge.getStringAsInt("gallery_backup_server", 0);
        boolean isXiaomi = backupServer == 1;
        boolean isGoogle = backupServer == 2;
        boolean isOneDrive = backupServer == 3;

        if (isOneDrive || isXiaomi) {
            findAndChainMethod("com.miui.gallery.util.BaseBuildUtil", "isInternational",
                chain -> {
                    Boolean override = sOverrideIsInternational.get();
                    if (override != null) {
                        return override;
                    }
                    return chain.proceed();
                }
            );
        }

        if (isOneDrive) {
            findAndChainMethod("com.miui.gallery.ui.GallerySettingsFragment",
                "initGlobalBackupPreference",
                chain -> {
                    sOverrideIsInternational.set(true);
                    try {
                        return chain.proceed();
                    } finally {
                        sOverrideIsInternational.remove();
                    }
                }
            );
            findAndHookMethod("com.miui.gallery.util.PhotoModelTypeUtil", "isSupportOneDrive", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    param.setResult(true);
                }
            });
        } else {
            if (isXiaomi) {
                findAndChainMethod("com.miui.gallery.ui.GallerySettingsFragment",
                    "initGlobalBackupPreference",
                    chain -> {
                        sOverrideIsInternational.set(false);
                        try {
                            return chain.proceed();
                        } finally {
                            sOverrideIsInternational.remove();
                        }
                    }
                );
            }
            try {
                findAndHookMethod("com.miui.gallery.transfer.GoogleSyncHelper", "isCloudServiceOffLine", new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        param.setResult(isGoogle);
                    }
                });
            } catch (Throwable t) {
                findAndHookMethod("com.miui.gallery.util.BuildUtil", "isGlobal", new IMethodHook() {
                    @Override
                    public void before(HookParam param) {
                        param.setResult(isGoogle);
                    }
                });
            }
        }
    }
}
