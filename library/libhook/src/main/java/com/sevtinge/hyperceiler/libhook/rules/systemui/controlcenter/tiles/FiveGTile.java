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
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.tiles;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileConfig;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileContext;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileState;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileUtils;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils;
import com.sevtinge.hyperceiler.libhook.utils.api.TelephonyManager;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class FiveGTile extends TileUtils {

    private static final String NFC_TILE_CLASS = "com.android.systemui.qs.tiles.MiuiNfcTile";
    private static final String SETTING_5G_USER_ENABLE = "fiveg_user_enable";
    private static final String SETTING_DUAL_NR_ENABLED = "dual_nr_enabled";
    private static final String FIELD_CONTENT_OBSERVER = "fiveGContentObserver";

    private final List<WeakReference<Object>> customToggles = new ArrayList<>();
    private final Map<View, DetailHeightAdjustment> detailHeights = new WeakHashMap<>();
    private final AtomicBoolean detailInsertionErrorLogged = new AtomicBoolean();

    private final int mode = PrefsBridge.getStringAsInt("system_control_center_5g_new_tile", 0);
    private ContentObserver contentObserver;
    private boolean isDetailHooked = false;

    @NonNull
    @Override
    protected TileConfig onCreateTileConfig() {
        if (mode == 3) {
            return new TileConfig.Builder()
                .setTileClass(findClassIfExists(NFC_TILE_CLASS))
                .build();
        }

        boolean isStyle1 = mode == 1;
        return new TileConfig.Builder()
            .setTileClass(findClassIfExists(NFC_TILE_CLASS))
            .setTileName("custom_5G")
            .setTileProvider("nfcTileProvider")
            .setLabelResId(R.string.tiles_5g)
            .setIcons(
                isStyle1 ? R.drawable.ic_control_center_5g_toggle_on : R.drawable.ic_control_center_5g_toggle_v2_on,
                isStyle1 ? R.drawable.ic_control_center_5g_toggle_off : R.drawable.ic_control_center_5g_toggle_v2_off
            )
            .build();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void init() {
        if (mode == 3) {
            XposedLog.d(TAG, "Init 5G Tile Mode 3");
            hookDetailPage();
            return;
        }
        super.init();
    }

    @Override
    protected boolean onCheckAvailable(TileContext ctx) {
        return TelephonyManager.getDefault().isFiveGCapable();
    }

    @Override
    protected void onTileClick(TileContext ctx) {
        toggleFiveG();
        ctx.refreshState();
    }

    @Nullable
    @Override
    protected Intent onGetLongClickIntent(TileContext ctx) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.settings.MiuiFiveGNetworkSetting"));
        return intent;
    }

    @Override
    protected void onListeningChanged(TileContext ctx, boolean listening) {
        Context context = ctx.getContext();

        if (listening) {
            if (contentObserver == null) {
                contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        ctx.refreshState();
                    }
                };

                context.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(SETTING_5G_USER_ENABLE), false, contentObserver);
                context.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(SETTING_DUAL_NR_ENABLED), false, contentObserver);
                BaseHook.registerContentObserverHotReloadCleanup(
                    context.getContentResolver(), contentObserver
                );

                ctx.setAdditionalField(FIELD_CONTENT_OBSERVER, contentObserver);
            }
        } else {
            if (contentObserver != null) {
                try {
                    context.getContentResolver().unregisterContentObserver(contentObserver);
                } catch (IllegalArgumentException ignored) {
                    // 已在热重载清理阶段注销。
                }
                contentObserver = null;
                ctx.removeAdditionalField(FIELD_CONTENT_OBSERVER);
            }
        }
    }

    @Nullable
    @Override
    protected TileState onUpdateState(TileContext ctx) {
        return TileState.of(TelephonyManager.getDefault().isUserFiveGEnabled());
    }

    // ==================== Mode 3 Logic ====================

    private void hookDetailPage() {
        if (isDetailHooked) return;

        try {
            Class<?> detailClass = findClass("com.android.systemui.qs.QSDetailContent");
            String detailName = detailClass.getName();
            Class<?> itemClass = findClass(detailName + "$Item");
            Class<?> selectableClass = findClass(detailName + "$SelectableItem");
            Class<?> toggleClass = findClass(detailName + "$ToggleItem");
            Constructor<?> dividerConstructor = findClass(detailName + "$TextDividerItem")
                .getConstructor(CharSequence.class);
            ToggleItemFactory toggleFactory = new ToggleItemFactory(itemClass, toggleClass);
            Field suffix = detailClass.getDeclaredField("suffix");
            suffix.setAccessible(true);

            Class<?> adapterClass = findClass(
                "com.android.systemui.qs.tiles.MiuiCellularTile$CellularDetailAdapter");
            Method createDetailView = adapterClass.getDeclaredMethod(
                "createDetailView", Context.class, View.class, ViewGroup.class);
            Method onDetailItemClick = adapterClass.getDeclaredMethod("onDetailItemClick", itemClass);
            Method setItems = detailClass.getDeclaredMethod("setItems", Array.newInstance(itemClass, 0).getClass());
            Method updateItems = toggleFactory.checked == null ? null : adapterClass.getDeclaredMethod("updateItems");
            if (updateItems != null) updateItems.setAccessible(true);

            hookMethod(onDetailItemClick, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    Object item = param.getArgs()[0];
                    if (item == null || !isCustomToggle(item)) return;

                    // A ToggleItem reaching the original callback would operate the intelligent dual-SIM switch.
                    param.setResult(null);
                    try {
                        if (updateItems == null) {
                            toggleFiveG();
                        } else {
                            boolean checked = toggleFactory.checked.getBoolean(item);
                            TelephonyManager.getDefault().setUserFiveGEnabled(checked);
                            XposedLog.d(TAG, "Requested detail 5G state: " + checked);
                            updateItems.invoke(param.getThisObject());
                        }
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        XposedLog.e(TAG, "Failed to handle detail 5G click", e);
                    }
                }
            });

            hookMethod(createDetailView, new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    if (param.getResult() instanceof View view) {
                        trackDetailHeight(view);
                    }
                }
            });

            // Install insertion after the click handler so every inserted row can be handled safely.
            hookMethod(setItems, new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    try {
                        ViewGroup content = (ViewGroup) param.getThisObject();
                        if (!"Cellular".equals(suffix.get(content))) return;
                        Object[] items = (Object[]) param.getArgs()[0];
                        if (items == null || items.length == 0) return;

                        int insertIndex = findInsertIndex(items, selectableClass);
                        if (insertIndex < 0) return;

                        TelephonyManager manager = TelephonyManager.getDefault();
                        if (!manager.isFiveGCapable()) return;
                        Resources modRes = AppsTool.getModuleRes(content.getContext());
                        if (modRes == null) return;

                        Object divider = dividerConstructor.newInstance(
                            modRes.getString(R.string.system_framework_phone_network_title));
                        Object toggle = toggleFactory.create(modRes.getString(R.string.dashboard_5g),
                            manager.isUserFiveGEnabled());
                        Object[] newItems = (Object[]) Array.newInstance(itemClass, items.length + 2);
                        System.arraycopy(items, 0, newItems, 0, insertIndex);
                        newItems[insertIndex] = divider;
                        newItems[insertIndex + 1] = toggle;
                        System.arraycopy(items, insertIndex, newItems, insertIndex + 2, items.length - insertIndex);
                        rememberCustomToggle(toggle);
                        param.getArgs()[0] = newItems;
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                        if (detailInsertionErrorLogged.compareAndSet(false, true)) {
                            XposedLog.e(TAG, "Failed to insert detail 5G toggle", e);
                        }
                    }
                }
            });

            registerHotReloadCleanup(() -> {
                detailHeights.forEach((view, adjustment) -> {
                    view.removeOnAttachStateChangeListener(adjustment);
                    adjustment.restoreHeight();
                });
                detailHeights.clear();
                synchronized (customToggles) {
                    customToggles.clear();
                }
            });
            isDetailHooked = true;
            XposedLog.d(TAG, "Detail page hooks applied successfully");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            XposedLog.e(TAG, "Failed to initialize detail 5G toggle", e);
        }
    }

    private synchronized void trackDetailHeight(View view) {
        if (detailHeights.containsKey(view)) return;
        DetailHeightAdjustment adjustment = new DetailHeightAdjustment();
        detailHeights.put(view, adjustment);
        view.addOnAttachStateChangeListener(adjustment);
        if (view.isAttachedToWindow()) adjustment.onViewAttachedToWindow(view);
    }

    private int findInsertIndex(Object[] items, Class<?> selectableClass) {
        int insertIndex = -1;
        for (int i = items.length - 1; i >= 0; i--) {
            Object item = items[i];
            if (item != null && isCustomToggle(item)) return -1;
            if (insertIndex < 0 && selectableClass.isInstance(item)) {
                insertIndex = i + 1;
            }
        }
        return insertIndex;
    }

    private void rememberCustomToggle(Object item) {
        synchronized (customToggles) {
            customToggles.removeIf(reference -> reference.get() == null);
            customToggles.add(new WeakReference<>(item));
        }
    }

    private boolean isCustomToggle(Object item) {
        synchronized (customToggles) {
            for (WeakReference<Object> reference : customToggles) {
                if (reference.get() == item) return true;
            }
            return false;
        }
    }

    private static final class DetailHeightAdjustment implements View.OnAttachStateChangeListener {
        private WeakReference<View> container;
        private int originalHeight;
        private int adjustedHeight;

        @Override
        public void onViewAttachedToWindow(@NonNull View view) {
            if (view.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return;
            }
            for (View child = view; child.getParent() instanceof View parent; child = parent) {
                try {
                    if (parent.getId() == View.NO_ID
                        || !"detail_container".equals(parent.getResources().getResourceEntryName(parent.getId()))) {
                        continue;
                    }
                    int height = (int) callMethod(parent, "getMaxHeight");
                    int extraHeight = DisplayUtils.dp2px(45f);
                    if (height > 0 && height <= Integer.MAX_VALUE - extraHeight) {
                        callMethod(parent, "setMaxHeight", height + extraHeight);
                        container = new WeakReference<>(parent);
                        originalHeight = height;
                        adjustedHeight = height + extraHeight;
                        parent.requestLayout();
                    }
                } catch (Resources.NotFoundException e) {
                    // A generated view ID has no resource name.
                    continue;
                } catch (RuntimeException | LinkageError e) {
                    // The height adjustment is optional for hosts without the matching container methods.
                }
                return;
            }
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull View view) {
            restoreHeight();
        }

        void restoreHeight() {
            View view = container == null ? null : container.get();
            container = null;
            if (view == null) return;
            try {
                if ((int) callMethod(view, "getMaxHeight") == adjustedHeight) {
                    callMethod(view, "setMaxHeight", originalHeight);
                    view.requestLayout();
                }
            } catch (RuntimeException | LinkageError ignored) {
                // The host owns the container and may have disposed it already.
            }
        }
    }

    private static final class ToggleItemFactory {
        private final Class<?> type;
        private final Constructor<?> constructor;
        private final Object unsafe;
        private final Method allocateInstance;
        private final Field title;
        private final Field summary;
        private final Field checked;

        ToggleItemFactory(Class<?> itemClass, Class<?> type) throws ReflectiveOperationException {
            if (!itemClass.isAssignableFrom(type)) {
                throw new NoSuchMethodException("Unexpected ToggleItem type: " + type.getName());
            }

            this.type = type;
            Constructor<?>[] constructors = type.getDeclaredConstructors();
            if (constructors.length == 0 && type.getSuperclass() == Object.class
                && Modifier.isFinal(type.getModifiers()) && type.getFields().length == 3) {
                // OS4's DEX allocates the item and initializes these fields directly.
                title = requireField(type, "title", CharSequence.class);
                summary = requireField(type, "summary", CharSequence.class);
                checked = requireField(type, "isChecked", boolean.class);
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                Field singleton = unsafeClass.getDeclaredField("theUnsafe");
                singleton.setAccessible(true);
                unsafe = singleton.get(null);
                allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
                constructor = null;
            } else {
                constructor = findLegacyConstructor(type);
                unsafe = null;
                allocateInstance = null;
                title = null;
                summary = null;
                checked = null;
            }
        }

        private static Constructor<?> findLegacyConstructor(Class<?> type) throws NoSuchMethodException {
            try {
                return type.getConstructor(CharSequence.class, CharSequence.class, boolean.class);
            } catch (NoSuchMethodException e) {
                return type.getConstructor(CharSequence.class, CharSequence.class, boolean.class, Object.class);
            }
        }

        private static Field requireField(Class<?> owner, String name, Class<?> type) throws NoSuchFieldException {
            Field field = owner.getDeclaredField(name);
            int modifiers = field.getModifiers();
            if (field.getType() != type || Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                throw new NoSuchFieldException("Unexpected ToggleItem field: " + field);
            }
            field.setAccessible(true);
            return field;
        }

        Object create(CharSequence label, boolean enabled) throws ReflectiveOperationException {
            if (constructor != null) {
                if (constructor.getParameterCount() == 3) {
                    return constructor.newInstance(label, null, enabled);
                }
                return constructor.newInstance(label, null, enabled, null);
            }
            Object item = allocateInstance.invoke(unsafe, type);
            title.set(item, label);
            summary.set(item, null);
            checked.setBoolean(item, enabled);
            return item;
        }
    }

    private void toggleFiveG() {
        TelephonyManager manager = TelephonyManager.getDefault();
        boolean newState = !manager.isUserFiveGEnabled();
        manager.setUserFiveGEnabled(newState);
        XposedLog.d(TAG, "5G switched to: " + newState);
    }
}
