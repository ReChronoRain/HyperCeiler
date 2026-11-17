/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.thememanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.github.libxposed.api.XposedInterface.HookHandle;

/** Restores only the native footer; picker, import and authorization remain unchanged. */
public final class RestoreLocalThemeImport extends BaseHook {
    private static final String FRAGMENT =
        "com.android.thememanager.mine.local.view.fragment.LocalThemeFragment";

    // Structural profile observed in 80.9.4.0, not a version/signature allowlist.
    // These IDs and obfuscated members are not claimed to work on every release.
    private static final int FOOTER_LAYOUT = 0x7f0e01b7;
    private static final int IMPORT_ID = 0x7f0b0486;

    @Override
    public void init() {
        List<HookHandle> handles = new ArrayList<>();
        try {
            Class<?> fragment = Class.forName(FRAGMENT, false, getClassLoader());
            if (!View.OnClickListener.class.isAssignableFrom(fragment)) {
                throw new IllegalStateException("Local theme fragment listener mismatch");
            }
            Field footer = memberField(fragment, "v");
            Field adapter = memberField(fragment.getSuperclass(), "r");
            Method create = memberMethod(fragment, "erbd");
            Method show = memberMethod(fragment, "t8r");
            Method hide = memberMethod(fragment, "y");
            Method refresh = memberMethod(fragment, "ki", List.class);
            Method activity = fragment.getMethod("getActivity");
            activity.setAccessible(true);
            Method add = memberMethod(adapter.getType(), "c", View.class);
            Method count = memberMethod(adapter.getType(), "e");
            if (footer.getType() != View.class || create.getReturnType() != void.class
                || show.getReturnType() != void.class || hide.getReturnType() != void.class
                || refresh.getReturnType() != void.class || count.getReturnType() != int.class
                || add.getReturnType() != void.class) {
                throw new IllegalStateException("Local theme footer structure mismatch");
            }

            handles.add(chain(create, invocation -> {
                Object result = invocation.proceed();
                debugProtect("restore local theme footer", () -> {
                    Object instance = invocation.getThisObject();
                    Object host = activity.invoke(instance);
                    Object wrapper = adapter.get(instance);
                    if (!(host instanceof Context context) || wrapper == null
                        || (Integer) count.invoke(wrapper) != 0) return;
                    if (!"layout".equals(context.getResources().getResourceTypeName(FOOTER_LAYOUT))
                        || !"id".equals(context.getResources().getResourceTypeName(IMPORT_ID))) return;
                    View previous = (View) footer.get(instance);
                    if (previous != null && previous.getParent() != null) return;
                    View root = LayoutInflater.from(context).inflate(FOOTER_LAYOUT, (ViewGroup) null, false);
                    View button = root.findViewById(IMPORT_ID);
                    if (button == null) return;
                    // The listener belongs to the host, not this module's classloader.
                    button.setOnClickListener((View.OnClickListener) instance);
                    footer.set(instance, root);
                    try {
                        add.invoke(wrapper, root);
                    } catch (Throwable error) {
                        footer.set(instance, previous);
                        throw error;
                    }
                });
                return result;
            }));
            for (Method method : new Method[]{show, hide}) {
                handles.add(chain(method, invocation -> {
                    Object result = invocation.proceed();
                    debugProtect("local theme footer visibility", () -> {
                        View view = (View) footer.get(invocation.getThisObject());
                        if (view != null) view.setVisibility(View.VISIBLE);
                    });
                    return result;
                }));
            }
            // The footer builder is small and may be inlined into this known caller.
            if (!xposed().deoptimize(refresh)) {
                throw new IllegalStateException("Cannot deoptimize local theme refresh");
            }
        } catch (Throwable error) {
            for (HookHandle handle : handles) {
                if (handle != null) handle.unhook();
            }
            throw new IllegalStateException("Local theme import restoration unavailable", error);
        }
    }

    private static Method memberMethod(Class<?> owner, String name, Class<?>... parameters)
        throws NoSuchMethodException {
        Method method = owner.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static Field memberField(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
