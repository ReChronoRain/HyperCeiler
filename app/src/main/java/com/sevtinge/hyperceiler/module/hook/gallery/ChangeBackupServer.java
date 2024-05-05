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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.hook.gallery;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;

public class ChangeBackupServer extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        boolean isXiaomi = mPrefsMap.getStringAsInt("gallery_backup_server", 0) == 1;
        boolean isGoogle = mPrefsMap.getStringAsInt("gallery_backup_server", 0) == 2;
        boolean isOneDrive = mPrefsMap.getStringAsInt("gallery_backup_server", 0) == 3;
        if (isOneDrive) {
            findAndHookMethod("com.miui.gallery.ui.GallerySettingsFragment", "initGlobalBackupPreference", new MethodHook() {
                XC_MethodHook.Unhook isInternationalHook;

                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    isInternationalHook = findAndHookMethodUseUnhook("com.miui.gallery.util.BaseBuildUtil", lpparam.classLoader, "isInternational", XC_MethodReplacement.returnConstant(true));
                }

                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    if (isInternationalHook != null) isInternationalHook.unhook();
                    isInternationalHook = null;
                }
            });
            findAndHookMethod("com.miui.gallery.util.PhotoModelTypeUtil", "isSupportOneDrive", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } else {
            if (isXiaomi) {
                findAndHookMethod("com.miui.gallery.ui.GallerySettingsFragment", "initGlobalBackupPreference", new MethodHook() {
                    XC_MethodHook.Unhook isInternationalHook;

                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        isInternationalHook = findAndHookMethodUseUnhook("com.miui.gallery.util.BaseBuildUtil", lpparam.classLoader, "isInternational", XC_MethodReplacement.returnConstant(false));
                    }

                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        if (isInternationalHook != null) isInternationalHook.unhook();
                        isInternationalHook = null;
                    }
                });
            }
            try {
                findAndHookMethod("com.miui.gallery.transfer.GoogleSyncHelper", "isCloudServiceOffLine", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(isGoogle);
                    }
                });
            } catch (Throwable t) {
                findAndHookMethod("com.miui.gallery.util.BuildUtil", "isGlobal", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.setResult(isGoogle);
                    }
                });
            }
        }
    }
}
