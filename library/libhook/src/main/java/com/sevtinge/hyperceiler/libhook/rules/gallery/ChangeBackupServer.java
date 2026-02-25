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

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;
import io.github.libxposed.api.XposedInterface;


public class ChangeBackupServer extends BaseHook {
    @Override
    public void init() {
        int backupServer = PrefsBridge.getStringAsInt("gallery_backup_server", 0);
        boolean isXiaomi = backupServer == 1;
        boolean isGoogle = backupServer == 2;
        boolean isOneDrive = backupServer == 3;

        if (isOneDrive) {
            findAndHookMethod("com.miui.gallery.ui.GallerySettingsFragment", "initGlobalBackupPreference", new IMethodHook() {
                XposedInterface.MethodUnhooker isInternationalHook;

                @Override
                public void before(BeforeHookParam param) {
                    isInternationalHook = findAndHookMethod("com.miui.gallery.util.BaseBuildUtil", "isInternational", new IMethodHook() {
                        @Override
                        public void before(BeforeHookParam param) {
                            param.setResult(true);
                        }
                    });
                }

                @Override
                public void after(AfterHookParam param) {
                    if (isInternationalHook != null) {
                        isInternationalHook.unhook();
                        isInternationalHook = null;
                    }
                }
            });
            findAndHookMethod("com.miui.gallery.util.PhotoModelTypeUtil", "isSupportOneDrive", new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    param.setResult(true);
                }
            });
        } else {
            if (isXiaomi) {
                findAndHookMethod("com.miui.gallery.ui.GallerySettingsFragment", "initGlobalBackupPreference", new IMethodHook() {
                    XposedInterface.MethodUnhooker isInternationalHook;

                    @Override
                    public void before(BeforeHookParam param) {
                        isInternationalHook = findAndHookMethod("com.miui.gallery.util.BaseBuildUtil", "isInternational", new IMethodHook() {
                            @Override
                            public void before(BeforeHookParam param) {
                                param.setResult(false);
                            }
                        });
                    }

                    @Override
                    public void after(AfterHookParam param) {
                        if (isInternationalHook != null) {
                            isInternationalHook.unhook();
                            isInternationalHook = null;
                        }
                    }
                });
            }
            try {
                findAndHookMethod("com.miui.gallery.transfer.GoogleSyncHelper", "isCloudServiceOffLine", new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        param.setResult(isGoogle);
                    }
                });
            } catch (Throwable t) {
                findAndHookMethod("com.miui.gallery.util.BuildUtil", "isGlobal", new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        param.setResult(isGoogle);
                    }
                });
            }
        }
    }
}
