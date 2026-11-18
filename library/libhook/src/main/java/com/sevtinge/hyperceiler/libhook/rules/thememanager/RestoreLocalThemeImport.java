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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.libxposed.api.XposedInterface.HookHandle;

/** Restores only the native footer; picker, import and authorization remain unchanged. */
public final class RestoreLocalThemeImport extends BaseHook {
    private static final String FRAGMENT =
        "com.android.thememanager.mine.local.view.fragment.LocalThemeFragment";
    private static final String FOOTER_FIELD = "v";
    private static final String ADAPTER_FIELD = "r";

    // Structural profile observed in 80.9.4.0, not a version/signature allowlist.
    // These IDs and obfuscated members are not claimed to work on every release.
    private static final int FOOTER_LAYOUT = 0x7f0e01b7;
    private static final int IMPORT_ID = 0x7f0b0486;

    private record Targets(Method create, Method show, Method hide, Method refresh,
                           Method activity, Method add, Method count) { }

    @Override
    public void init() {
        List<HookHandle> handles = new ArrayList<>();
        try {
            Targets targets = resolveTargets();
            handles.add(Objects.requireNonNull(hookFooter(targets)));
            handles.add(Objects.requireNonNull(hookVisibility(targets.show())));
            handles.add(Objects.requireNonNull(hookVisibility(targets.hide())));
            // The footer builder may be inlined into this known caller.
            if (!xposed().deoptimize(targets.refresh())) {
                throw new IllegalStateException("Cannot deoptimize local theme refresh");
            }
        } catch (Throwable error) {
            rollback(handles);
            throw new IllegalStateException("Local theme import restoration unavailable", error);
        }
    }

    private static void rollback(List<HookHandle> handles) {
        for (HookHandle handle : handles) {
            handle.unhook();
        }
    }

    private Targets resolveTargets() throws ReflectiveOperationException {
        Class<?> fragment = Class.forName(FRAGMENT, false, getClassLoader());
        if (!View.OnClickListener.class.isAssignableFrom(fragment)) {
            throw new IllegalStateException("Local theme fragment listener mismatch");
        }
        requireType(fragment.getDeclaredField(FOOTER_FIELD).getType(), View.class);
        Class<?> adapter = fragment.getSuperclass().getDeclaredField(ADAPTER_FIELD).getType();
        Method activity = fragment.getMethod("getActivity");
        if (!Context.class.isAssignableFrom(activity.getReturnType())) {
            throw new IllegalStateException("Local theme activity type mismatch");
        }
        return new Targets(
            checkedMethod(fragment, "erbd", void.class),
            checkedMethod(fragment, "t8r", void.class),
            checkedMethod(fragment, "y", void.class),
            checkedMethod(fragment, "ki", void.class, List.class),
            activity,
            checkedMethod(adapter, "c", void.class, View.class),
            checkedMethod(adapter, "e", int.class)
        );
    }

    private static Method checkedMethod(Class<?> owner, String name, Class<?> result,
                                        Class<?>... parameters) throws NoSuchMethodException {
        Method method = owner.getDeclaredMethod(name, parameters);
        requireType(method.getReturnType(), result);
        return method;
    }

    private static void requireType(Class<?> actual, Class<?> expected) {
        if (actual != expected) {
            throw new IllegalStateException("Local theme footer structure mismatch");
        }
    }

    private HookHandle hookFooter(Targets targets) {
        return chain(targets.create(), invocation -> {
            Object result = invocation.proceed();
            debugProtect("restore local theme footer",
                () -> restoreFooter(invocation.getThisObject(), targets));
            return result;
        });
    }

    private HookHandle hookVisibility(Method method) {
        return chain(method, invocation -> {
            Object result = invocation.proceed();
            debugProtect("local theme footer visibility",
                () -> showFooter(invocation.getThisObject()));
            return result;
        });
    }

    private static void showFooter(Object instance) {
        View view = (View) getObjectField(instance, FOOTER_FIELD);
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    private void restoreFooter(Object instance, Targets targets) throws ReflectiveOperationException {
        Object host = xposed().getInvoker(targets.activity()).invoke(instance);
        Object wrapper = getObjectField(instance, ADAPTER_FIELD);
        if (!(host instanceof Context context) || wrapper == null) return;
        if ((Integer) xposed().getInvoker(targets.count()).invoke(wrapper) != 0) return;
        if (!hasFooterResources(context)) return;
        View previous = (View) getObjectField(instance, FOOTER_FIELD);
        if (previous != null && previous.getParent() != null) return;
        View root = inflateFooter(context, instance);
        if (root != null) {
            registerFooter(instance, wrapper, root, previous, targets.add());
        }
    }

    private static boolean hasFooterResources(Context context) {
        return "layout".equals(context.getResources().getResourceTypeName(FOOTER_LAYOUT))
            && "id".equals(context.getResources().getResourceTypeName(IMPORT_ID));
    }

    private static View inflateFooter(Context context, Object instance) {
        View root = LayoutInflater.from(context).inflate(FOOTER_LAYOUT, (ViewGroup) null, false);
        View button = root.findViewById(IMPORT_ID);
        if (button == null) return null;
        // The listener belongs to the host, not this module's classloader.
        button.setOnClickListener((View.OnClickListener) instance);
        return root;
    }

    private void registerFooter(Object instance, Object wrapper, View root, View previous,
                                Method add) throws ReflectiveOperationException {
        setObjectField(instance, FOOTER_FIELD, root);
        boolean added = false;
        try {
            xposed().getInvoker(add).invoke(wrapper, root);
            added = true;
        } finally {
            if (!added) setObjectField(instance, FOOTER_FIELD, previous);
        }
    }
}
