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

package com.sevtinge.hyperceiler.libhook.appbase.input;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.XposedLog;

import java.util.List;

public final class InputMethodDexHelper {
    @FunctionalInterface
    public interface LoaderCallback {
        void load(@NonNull ClassLoader classLoader) throws Throwable;
    }

    public static final class Loader {
        private final String mName;
        private final LoaderCallback mCallback;

        private Loader(@NonNull String name, @NonNull LoaderCallback callback) {
            mName = name;
            mCallback = callback;
        }
    }

    private InputMethodDexHelper() {
    }

    @NonNull
    public static Loader loader(@NonNull String name, @NonNull LoaderCallback callback) {
        return new Loader(name, callback);
    }

    public static void dispatchLoaders(
        @NonNull String tag,
        @Nullable String packageName,
        @NonNull ClassLoader classLoader,
        @NonNull List<Loader> loaders
    ) {
        for (Loader loader : loaders) {
            try {
                loader.mCallback.load(classLoader);
                XposedLog.d(tag, packageName, loader.mName + " is loaded success.");
            } catch (Throwable t) {
                XposedLog.e(tag, packageName, loader.mName + " failed to load for IME classloader", t);
            }
        }
    }
}
