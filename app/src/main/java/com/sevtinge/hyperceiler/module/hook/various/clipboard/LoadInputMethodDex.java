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
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.various.clipboard;

import com.hchen.hooktool.BaseHC;
import com.hchen.hooktool.hook.IHook;

import java.util.Arrays;

/**
 * 获取常用语的 classloader。
 * from <a href="https://github.com/HChenX/ClipboardList">ClipboardList</a>
 *
 * @author 焕晨HChen
 */
public class LoadInputMethodDex extends BaseHC {
    private final OnInputMethodDexLoad[] mOnInputMethodDexLoad;
    private boolean isHooked;

    public LoadInputMethodDex(OnInputMethodDexLoad... dexLoads) {
        mOnInputMethodDexLoad = dexLoads;
    }

    @Override
    public void init() {
        hookMethod("android.inputmethodservice.InputMethodModuleManager",
                "loadDex", ClassLoader.class, String.class,
                new IHook() {
                    @Override
                    public void after() {
                        if (isHooked) return;
                        Arrays.stream(mOnInputMethodDexLoad).forEach(load -> load.load((ClassLoader) getArgs(0)));
                        isHooked = true;
                    }
                }
        );
    }

    public interface OnInputMethodDexLoad {
        void load(ClassLoader classLoader);
    }
}
