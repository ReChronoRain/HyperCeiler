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

import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileConfig;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileContext;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileState;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileUtils;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils;
import com.sevtinge.hyperceiler.libhook.utils.api.TelephonyManager;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class FiveGTile extends TileUtils {

    private static final String NFC_TILE_CLASS = "com.android.systemui.qs.tiles.MiuiNfcTile";
    private static final String SETTING_5G_USER_ENABLE = "fiveg_user_enable";
    private static final String SETTING_DUAL_NR_ENABLED = "dual_nr_enabled";
    private static final String FIELD_CONTENT_OBSERVER = "fiveGContentObserver";

    private Class<?> mDetailContentClass;
    private Class<?> mItemClass;
    private Constructor<?> mDividerConstructor;
    private Constructor<?> mToggleConstructor3;
    private Constructor<?> mToggleConstructor4;

    private final int mode = mPrefsMap.getStringAsInt("system_control_center_5g_new_tile", 0);;
    private ContentObserver contentObserver;
    private boolean isDetailHooked = false;
    private String fiveGLabel = null;

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

                ctx.setAdditionalField(FIELD_CONTENT_OBSERVER, contentObserver);
            }
        } else {
            if (contentObserver != null) {
                context.getContentResolver().unregisterContentObserver(contentObserver);
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
            mDetailContentClass = findClassIfExists("com.android.systemui.qs.QSDetailContent");
            if (mDetailContentClass == null) {
                XposedLog.e(TAG, "QSDetailContent class not found");
                return;
            }

            String itemClzName = mDetailContentClass.getName() + "$Item";
            mItemClass = findClassIfExists(itemClzName);
            if (mItemClass == null) {
                XposedLog.e(TAG, "QSDetailContent$Item class not found");
                return;
            }

            prepareReflections();

            findAndHookMethod(
                "com.android.systemui.qs.tiles.MiuiCellularTile$CellularDetailAdapter",
                "createDetailView",
                Context.class, View.class, ViewGroup.class,
                new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        Context context = (Context) param.getArgs()[0];
                        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            return;
                        }
                        View content = (View) param.getResult();
                        adjustDetailViewHeight(content);

                        if (fiveGLabel == null) {
                            try {
                                Resources modRes = AppsTool.getModuleRes(context);
                                if (modRes != null) {
                                    fiveGLabel = modRes.getString(R.string.dashboard_5g);
                                }
                            } catch (Throwable t) {
                                XposedLog.e(TAG, "Failed to get 5G label", t);
                            }
                        }
                    }
                }
            );

            findAndHookMethod(
                "com.android.systemui.qs.tiles.MiuiCellularTile$CellularDetailAdapter",
                "onDetailItemClick",
                mItemClass,
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        Object item = param.getArgs()[0];
                        String className = item.getClass().getSimpleName();

                        if (!className.contains("ToggleItem")) {
                            XposedLog.d(TAG, "Not a ToggleItem, skip");
                            return;
                        }

                        try {
                            CharSequence title = getItemTitle(item);

                            if (title != null && fiveGLabel != null &&
                                title.toString().equals(fiveGLabel)) {
                                XposedLog.d(TAG, "5G toggle clicked!");
                                toggleFiveG();
                                param.setResult(null);
                            }
                        } catch (Throwable t) {
                            XposedLog.e(TAG, "Error handling click", t);
                        }
                    }
                }
            );

            Class<?> itemArrayClass = Array.newInstance(mItemClass, 0).getClass();
            findAndHookMethod(
                mDetailContentClass,
                "setItems",
                itemArrayClass,
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) throws Throwable {
                        handleSetItems(param);
                    }
                }
            );

            isDetailHooked = true;
            XposedLog.d(TAG, "Detail page hooks applied successfully");

        } catch (Throwable t) {
            XposedLog.e(TAG, "Error in hookDetailPage", t);
        }
    }

    private void adjustDetailViewHeight(View content) {
        content.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                View view = v;
                while (view.getParent() instanceof View) {
                    view = (View) view.getParent();
                    if (view.getId() != View.NO_ID) {
                        String idName = view.getResources().getResourceName(view.getId());
                        if (idName.endsWith("detail_container")) {
                            int maxHeight = (int) EzxHelpUtils.callMethod(view, "getMaxHeight");
                            EzxHelpUtils.callMethod(view, "setMaxHeight", maxHeight + DisplayUtils.dp2px(45f));
                            break;
                        }
                    }
                }
                v.removeOnAttachStateChangeListener(this);
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {}
        });
    }

    private void handleSetItems(BeforeHookParam param) throws Throwable {
        ViewGroup content = (ViewGroup) param.getThisObject();
        Object suffix = EzxHelpUtils.getObjectField(content, "suffix");

        if (!"Cellular".equals(suffix)) return;

        Object[] rawItems = (Object[]) param.getArgs()[0];
        if (rawItems == null || rawItems.length == 0) return;

        // 如果不支持 5G，直接返回，不添加选项
        if (!TelephonyManager.getDefault().isFiveGCapable()) {
            return;
        }

        int insertIndex = findInsertIndex(rawItems);
        if (insertIndex == -1) {
            XposedLog.d(TAG, "Insert index not found");
            return;
        }

        if (mDividerConstructor == null || (mToggleConstructor3 == null && mToggleConstructor4 == null)) {
            prepareReflections();
            if (mDividerConstructor == null) {
                XposedLog.e(TAG, "Constructors not ready");
                return;
            }
        }

        Resources modRes = AppsTool.getModuleRes(content.getContext());
        if (modRes == null) {
            XposedLog.e(TAG, "Module resources is null");
            return;
        }

        String fiveGLabel = modRes.getString(R.string.dashboard_5g);
        String networkTitle = modRes.getString(R.string.system_framework_phone_network_title);

        Object[] newItems = (Object[]) Array.newInstance(mItemClass, rawItems.length + 2);

        System.arraycopy(rawItems, 0, newItems, 0, insertIndex);

        newItems[insertIndex] = mDividerConstructor.newInstance(networkTitle);

        Object toggleItem = createToggleItem(fiveGLabel);
        if (toggleItem != null) {
            // 因为会被 GC 回收导致第一次调用会 null, 设置强引用或者字符串判断为佳
            // EzxHelpUtils.setAdditionalInstanceField(toggleItem, FIELD_IS_CUSTOM_5G, true);
            newItems[insertIndex + 1] = toggleItem;
        } else {
            XposedLog.e(TAG, "Failed to create toggle item");
            return;
        }

        if (insertIndex < rawItems.length) {
            System.arraycopy(rawItems, insertIndex, newItems, insertIndex + 2, rawItems.length - insertIndex);
        }

        param.getArgs()[0] = newItems;
        XposedLog.d(TAG, "5G item injected");
    }

    private CharSequence getItemTitle(Object item) {
        String[] possibleFields = {"title", "mTitle", "label", "mLabel", "name", "mName", "text"};

        for (String fieldName : possibleFields) {
            try {
                Object value = EzxHelpUtils.getObjectField(item, fieldName);
                if (value instanceof CharSequence) {
                    return (CharSequence) value;
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private int findInsertIndex(Object[] items) {
        for (int i = items.length - 1; i >= 0; i--) {
            if (items[i].getClass().getName().endsWith("SelectableItem")) {
                return i + 1;
            }
        }
        return -1;
    }

    private void prepareReflections() {
        if (mDividerConstructor != null) return;

        try {
            Class<?> dividerClass = findClassIfExists(mDetailContentClass.getName() + "$TextDividerItem");
            if (dividerClass != null) {
                mDividerConstructor = dividerClass.getConstructor(CharSequence.class);
            } else {
                XposedLog.e(TAG, "TextDividerItem class not found");
            }

            Class<?> toggleClass = findClassIfExists(mDetailContentClass.getName() + "$ToggleItem");
            if (toggleClass != null) {
                try {
                    mToggleConstructor3 = toggleClass.getConstructor(CharSequence.class, CharSequence.class, boolean.class);
                } catch (NoSuchMethodException e) {
                    mToggleConstructor4 = toggleClass.getConstructor(CharSequence.class, CharSequence.class, boolean.class, Object.class);
                }
            } else {
                XposedLog.e(TAG, "ToggleItem class not found");
            }
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to prepare reflections", e);
        }
    }

    private Object createToggleItem(String label) {
        boolean enabled = TelephonyManager.getDefault().isUserFiveGEnabled();
        try {
            if (mToggleConstructor3 != null) {
                return mToggleConstructor3.newInstance(label, null, enabled);
            } else if (mToggleConstructor4 != null) {
                return mToggleConstructor4.newInstance(label, null, enabled, null);
            }
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to create toggle item instance", e);
        }
        return null;
    }

    private void toggleFiveG() {
        TelephonyManager manager = TelephonyManager.getDefault();
        boolean newState = !manager.isUserFiveGEnabled();
        manager.setUserFiveGEnabled(newState);
        XposedLog.d(TAG, "5G switched to: " + newState);
    }
}
