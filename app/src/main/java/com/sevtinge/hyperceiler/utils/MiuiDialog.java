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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils;

import android.content.Context;
import android.content.DialogInterface;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.XposedHelpers;

/**
 * 调用 Miui 的 MiuiDialog，
 * 仅 Xposed 代码可以调用
 */
public class MiuiDialog {
    private static final String path = "miuix.appcompat.app.AlertDialog";
    private static final String pathJar = "/system/framework/miuix.jar";
    private Class<?> miuiClass = null;
    private Context context = null;
    private Object newInstance = null;

    /**
     * 基本不可用，请传 ClassLoader
     */
    public MiuiDialog(Context context) {
        this.context = context;
        miuiClass = findClass(new PathClassLoader(pathJar, ClassLoader.getSystemClassLoader()));
        newInstance = XposedHelpers.newInstance(miuiClass, context);
    }

    /**
     * 基本不可用，请传 ClassLoader
     */
    public MiuiDialog(Context context, int value) {
        this.context = context;
        miuiClass = findClass(new PathClassLoader(pathJar, ClassLoader.getSystemClassLoader()));
        newInstance = XposedHelpers.newInstance(miuiClass, context, value);
    }

    public MiuiDialog(ClassLoader classLoader, Context context) {
        this.context = context;
        miuiClass = findClass(classLoader);
        newInstance = XposedHelpers.newInstance(miuiClass, context);
    }

    public MiuiDialog(ClassLoader classLoader, Context context, int value) {
        this.context = context;
        miuiClass = findClass(classLoader);
        newInstance = XposedHelpers.newInstance(miuiClass, context, value);
    }

    private Class<?> findClass(ClassLoader classLoader) {
        Class<?> clz = XposedHelpers.findClassIfExists(path, classLoader);
        if (clz == null) throw new RuntimeException("Failed to get miuix class!");
        return clz;
    }

    public static class Builder {
        private Class<?> miuiClass = null;
        private Context context = null;
        private Object newInstance = null;
        private static final String pathBuilder = "miuix.appcompat.app.AlertDialog$Builder";

        /**
         * 基本不可用，请传 ClassLoader
         */
        public Builder(Context context) {
            this.context = context;
            miuiClass = findClass(new PathClassLoader(pathJar, ClassLoader.getSystemClassLoader()));
            newInstance = XposedHelpers.newInstance(miuiClass, context);
        }

        /**
         * 基本不可用，请传 ClassLoader
         */
        public Builder(Context context, int value) {
            this.context = context;
            miuiClass = findClass(new PathClassLoader(pathJar, ClassLoader.getSystemClassLoader()));
            newInstance = XposedHelpers.newInstance(miuiClass, context, value);
        }

        public Builder(ClassLoader classLoader, Context context) {
            this.context = context;
            miuiClass = findClass(classLoader);
            newInstance = XposedHelpers.newInstance(miuiClass, context);
        }

        public Builder(ClassLoader classLoader, Context context, int value) {
            this.context = context;
            miuiClass = findClass(classLoader);
            newInstance = XposedHelpers.newInstance(miuiClass, context, value);
        }

        public Builder setTitle(int id) {
            newInstance = XposedHelpers.callMethod(newInstance, "setTitle", id);
            return this;
        }

        public Builder setTitle(CharSequence charSequence) {
            newInstance = XposedHelpers.callMethod(newInstance, "setTitle", charSequence);
            return this;
        }

        public Builder setHapticFeedbackEnabled(boolean z) {
            newInstance = XposedHelpers.callMethod(newInstance, "setHapticFeedbackEnabled", z);
            return this;
        }

        public Builder setMessage(int id) {
            newInstance = XposedHelpers.callMethod(newInstance, "setMessage", id);
            return this;
        }

        public Builder setMessage(CharSequence charSequence) {
            newInstance = XposedHelpers.callMethod(newInstance, "setMessage", charSequence);
            return this;
        }

        public Builder setNegativeButton(int id, DialogInterface.OnClickListener onClickListener) {
            newInstance = XposedHelpers.callMethod(newInstance, "setNegativeButton", id, onClickListener);
            return this;
        }

        public Builder setNegativeButton(CharSequence charSequence, DialogInterface.OnClickListener onClickListener) {
            newInstance = XposedHelpers.callMethod(newInstance, "setNegativeButton", charSequence, onClickListener);
            return this;
        }

        public Builder setNeutralButton(int id, DialogInterface.OnClickListener onClickListener) {
            newInstance = XposedHelpers.callMethod(newInstance, "setNeutralButton", id, onClickListener);
            return this;
        }

        public Builder setNeutralButton(CharSequence charSequence, DialogInterface.OnClickListener onClickListener) {
            newInstance = XposedHelpers.callMethod(newInstance, "setNeutralButton", onClickListener);
            return this;
        }

        public Builder setPositiveButton(int id, DialogInterface.OnClickListener onClickListener) {
            newInstance = XposedHelpers.callMethod(newInstance, "setPositiveButton", id, onClickListener);
            return this;
        }

        public Builder setPositiveButton(CharSequence charSequence, DialogInterface.OnClickListener onClickListener) {
            newInstance = XposedHelpers.callMethod(newInstance, "setPositiveButton", onClickListener);
            return this;
        }

        public void show() {
            newInstance = XposedHelpers.callMethod(newInstance, "create");
            XposedHelpers.callMethod(newInstance, "show");
        }

        private Class<?> findClass(ClassLoader classLoader) {
            Class<?> clz = XposedHelpers.findClassIfExists(pathBuilder, classLoader);
            if (clz == null) throw new RuntimeException("Failed to get miuix$builder class!");
            return clz;
        }
    }
}
