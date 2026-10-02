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
package com.sevtinge.hyperceiler.libhook.provider;

import android.database.MatrixCursor;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.os.Binder;
import android.os.Process;
import android.view.SurfaceControl;
import android.view.View;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.Executable;
import java.util.Locale;
import java.util.TreeSet;

/** Read-only API inventory for Dock glass compatibility; never captures screen or app data. */
final class DockGlassDiagnostics {
    private DockGlassDiagnostics() {}

    static MatrixCursor query() {
        int uid = Binder.getCallingUid();
        if (uid != Process.myUid() && uid != 2000 && uid != 0) {
            throw new SecurityException("Dock diagnostics are restricted to this app and ADB");
        }
        MatrixCursor cursor = new MatrixCursor(new String[]{"api"});
        addCapabilities(cursor);
        for (Class<?> type : new Class<?>[]{SurfaceControl.Transaction.class, SurfaceControl.Builder.class,
                View.class, RenderEffect.class, RenderNode.class}) {
            addMethods(cursor, type);
        }
        try {
            addMethods(cursor, Class.forName("com.android.internal.graphics.drawable.BackgroundBlurDrawable"));
        } catch (ClassNotFoundException error) {
            cursor.addRow(new Object[]{"BackgroundBlurDrawable: unavailable"});
        }
        return cursor;
    }

    private static void addCapabilities(MatrixCursor cursor) {
        try {
            Class<?> root = Class.forName("android.view.ViewRootImpl");
            for (String capability : new String[]{"getSupportedBionicMaterial", "getSupportedMiBlur"}) {
                cursor.addRow(new Object[]{capability + "=" + HiddenApiBypass.invoke(root, null, capability)});
            }
        } catch (Throwable error) {
            cursor.addRow(new Object[]{"Capability query: " + error.getClass().getSimpleName()});
        }
    }

    private static boolean isMaterialMethod(Executable method) {
        String lower = method.getName().toLowerCase(Locale.ROOT);
        for (String word : new String[]{"glass", "blur", "material", "backdrop", "stroke",
                "refract", "blend", "shader", "effect"}) {
            if (lower.contains(word)) return true;
        }
        return false;
    }

    private static void addMethods(MatrixCursor cursor, Class<?> type) {
        try {
            TreeSet<String> matches = new TreeSet<>();
            for (Executable method : HiddenApiBypass.getDeclaredMethods(type)) {
                if (isMaterialMethod(method)) matches.add(method.toString());
            }
            if (matches.isEmpty()) matches.add(type.getName() + ": no matching API");
            for (String match : matches) cursor.addRow(new Object[]{match});
        } catch (Throwable error) {
            cursor.addRow(new Object[]{type.getName() + ": " + error.getClass().getSimpleName()});
        }
    }
}
