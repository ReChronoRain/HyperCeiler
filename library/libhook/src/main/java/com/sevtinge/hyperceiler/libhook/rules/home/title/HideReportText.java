/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.rules.home.title;

import android.content.Context;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class HideReportText extends BaseHook {
    @Override
    public void init() {
        try {
            findAndHookMethod("com.miui.home.launcher.uninstall.BaseUninstallDialog", "init", Context.class, List.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    findAndHookMethod("com.miui.home.launcher.ShortcutInfo", "getInstallerPackageName", new IMethodHook() {
                        @Override
                        public void before(BeforeHookParam param) {
                            param.setResult("com.xiaomi.market");
                        }
                    });
                }
            });
        } catch (Throwable t) {
            findAndHookMethod("com.miui.home.launcher.uninstall.BaseUninstallDialog", "init", Context.class, List.class, String.class, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    findAndHookMethod("com.miui.home.launcher.ShortcutInfo", "getInstallerPackageName", new IMethodHook() {
                        @Override
                        public void before(BeforeHookParam param) {
                            param.setResult("com.xiaomi.market");
                        }
                    });
                }
            });
        }
    }
}
