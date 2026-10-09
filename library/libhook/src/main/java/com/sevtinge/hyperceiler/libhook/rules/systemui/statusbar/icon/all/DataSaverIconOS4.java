/*
 * This file is part of HyperCeiler.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.all;

import android.content.res.Resources;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

/** OS4's legacy icon pipeline, including repeated Home block-list emissions.
 * Preferences are selected at hook initialization; restart SystemUI after changes.
 */
public final class DataSaverIconOS4 extends BaseHook {
    private static final String PREFIX = "com.android.systemui.statusbar.";
    private volatile boolean installed;

    @Override
    public void init() {
        final int mode = PrefsBridge.getStringAsInt("system_ui_status_bar_icon_data_saver", 0);
        if (mode != 1 && mode != 2 && mode != 11 && mode != 12) return;

        List<XposedInterface.HookHandle> handles = new ArrayList<>();
        try {
            Class<?> policy = required(PREFIX + "phone.PhoneStatusBarPolicy");
            Class<?> controller = required(PREFIX + "phone.ui.StatusBarIconControllerImpl");
            Class<?> controllerInterface = required(PREFIX + "phone.ui.StatusBarIconController");
            Class<?> manager = required(PREFIX + "phone.ui.IconManager");
            Class<?> location = required(PREFIX + "phone.StatusBarLocation");
            Class<?> iconList = required(PREFIX + "phone.ui.StatusBarIconList");
            Class<?> holder = required(PREFIX + "phone.StatusBarIconHolder");
            Class<?> icon = required("com.android.internal.statusbar.StatusBarIcon");
            if (!location.isEnum() || !controllerInterface.isAssignableFrom(controller)) {
                throw new IllegalStateException("Unrecognized OS4 data-saver controller shape");
            }
            Field policySlot = field(policy, "mSlotDataSaver", String.class);
            Field policyController = field(policy, "mIconController", controllerInterface);
            Field policyResources = field(policy, "mResources", Resources.class);
            Field controllerIcons = field(controller, "mStatusBarIconList", iconList);
            Field holderType = field(holder, "type", int.class);
            Field holderIcon = field(holder, "icon", icon);
            Field managerLocation = field(manager, "mLocation", location);
            Field managerBlocked = field(manager, "mBlockList", ArrayList.class);
            Object home = enumValue(location, "HOME");
            Object keyguard = enumValue(location, "KEYGUARD");
            Object fake = enumValue(location, "QS_FAKE");
            Object qs = enumValue(location, "QS");
            Method callback = method(policy, "onDataSaverChanged", void.class, boolean.class);
            Method blockList = method(manager, "setBlockList", void.class, List.class);
            Method addGroup = method(controller, "addIconGroup", void.class, manager);
            Method getHolder = method(iconList, "getIconHolder", holder, int.class, String.class);
            Method setIcon = method(controller, "setIcon", void.class, CharSequence.class, String.class, int.class);

            Resources system = Resources.getSystem();
            int slotId = system.getIdentifier("status_bar_data_saver", "string", "android");
            final String slot = slotId == 0 ? null : system.getString(slotId);
            if (!"data_saver".equals(slot)) {
                throw new IllegalStateException("Unrecognized OS4 data-saver slot resource");
            }

            handles.add(requireHandle(hookMethod(blockList, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    if (!installed || param.isSkipped()) return;
                    try {
                        int region = region(managerLocation.get(param.getThisObject()), home, keyguard, fake, qs);
                        if (region != 0 && param.getArgs()[0] instanceof List<?> input) {
                            // The ROM copies this into its own mutable list and refreshes
                            // the group. Never edit a shared, immutable or Flow-owned input.
                            param.getArgs()[0] = adjusted(input, slot, mode, region);
                        }
                    } catch (ReflectiveOperationException | RuntimeException error) {
                        debugCallbackError("data saver block-list", error);
                    }
                }
            })));
            handles.add(requireHandle(hookMethod(addGroup, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    if (!installed || param.isSkipped()) return;
                    try {
                        Object group = param.getArgs()[0];
                        if (!manager.isInstance(group)) return;
                        int region = region(managerLocation.get(group), home, keyguard, fake, qs);
                        if (region == 0) return;
                        Object value = managerBlocked.get(group);
                        if (!(value instanceof ArrayList<?> input)) return;
                        List<?> replacement = adjusted(input, slot, mode, region);
                        // MiuiQSHeaderView adds a group without setBlockList. The
                        // original addIconGroup consumes this list immediately.
                        // Avoid an extra view refresh while the group is being added.
                        @SuppressWarnings("unchecked")
                        ArrayList<Object> owned = (ArrayList<Object>) input;
                        owned.clear();
                        owned.addAll(replacement);
                    } catch (ReflectiveOperationException | RuntimeException error) {
                        debugCallbackError("data saver initial icon group", error);
                    }
                }
            })));
            handles.add(requireHandle(hookMethod(callback, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    if (!installed || param.isSkipped()) return;
                    try {
                        Object target = param.getThisObject();
                        if (!slot.equals(policySlot.get(target))) return;
                        boolean visible = mode == 1 || (mode != 2 && (boolean) param.getArgs()[0]);
                        if (visible) {
                            Object owner = policyController.get(target);
                            if (!controller.isInstance(owner)) return;
                            Object icons = controllerIcons.get(owner);
                            Object current = getHolder.invoke(icons, 0, slot);
                            if (current == null) {
                                Resources resources = (Resources) policyResources.get(target);
                                if (resources == null) return;
                                int drawable = resources.getIdentifier("stat_sys_data_saver", "drawable", "com.android.systemui");
                                int description = resources.getIdentifier("data_saver", "string", "com.android.systemui");
                                if (drawable == 0 || description == 0) return;
                                // This ROM still dispatches onDataSaverChanged, but
                                // start() no longer creates its legacy icon holder.
                                setIcon.invoke(owner, resources.getString(description), slot, drawable);
                                current = getHolder.invoke(icons, 0, slot);
                            }
                            // Do not reinterpret or replace a bindable/mobile/foreign holder.
                            if (current == null || current.getClass() != holder
                                || holderType.getInt(current) != 0 || holderIcon.get(current) == null) return;
                        }
                        param.getArgs()[0] = visible;
                    } catch (ReflectiveOperationException | RuntimeException error) {
                        debugCallbackError("data saver visibility", error);
                    }
                }
            })));
            installed = true;
        } catch (ReflectiveOperationException | RuntimeException error) {
            installed = false;
            for (XposedInterface.HookHandle handle : handles) {
                try { handle.unhook(); } catch (RuntimeException rollback) { error.addSuppressed(rollback); }
            }
            throw new IllegalStateException("OS4 data-saver hook shape or registration unavailable", error);
        }
    }

    private static int region(Object value, Object home, Object keyguard, Object fake, Object qs) {
        if (value == qs) return 2;
        if (value == home || value == keyguard || value == fake) return 1;
        return 0;
    }

    private static List<?> adjusted(List<?> input, String slot, int mode, int region) {
        ArrayList<Object> result = new ArrayList<>(input);
        result.removeIf(slot::equals);
        if (mode == 2 || (mode == 11 && region == 1) || (mode == 12 && region == 2)) result.add(slot);
        return result;
    }

    private static XposedInterface.HookHandle requireHandle(XposedInterface.HookHandle handle) {
        if (handle == null) throw new IllegalStateException("OS4 data-saver hook was not registered");
        return handle;
    }

    private static Class<?> required(String name) {
        Class<?> type = findClassIfExists(name);
        if (type == null) throw new IllegalStateException("Missing OS4 data-saver class: " + name);
        return type;
    }

    private static Field field(Class<?> owner, String name, Class<?> type) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        if (field.getType() != type || Modifier.isStatic(field.getModifiers())) {
            throw new IllegalStateException("Unrecognized OS4 field: " + owner.getName() + "." + name);
        }
        field.setAccessible(true);
        return field;
    }

    private static Object enumValue(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        if (field.getType() != owner || !Modifier.isStatic(field.getModifiers()) || !field.isEnumConstant()) {
            throw new IllegalStateException("Unrecognized OS4 icon location: " + name);
        }
        return field.get(null);
    }

    private static Method method(Class<?> owner, String name, Class<?> result, Class<?>... args)
        throws ReflectiveOperationException {
        Method method = owner.getDeclaredMethod(name, args);
        if (method.getReturnType() != result || Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException("Unrecognized OS4 method: " + owner.getName() + "." + name);
        }
        method.setAccessible(true);
        return method;
    }
}
