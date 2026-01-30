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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileConfig;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileContext;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileState;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

public class TaplusTile extends TileUtils {

    private static final String SETTING_KEY_TAPLUS = "key_enable_taplus";
    private static final String FIELD_CONTENT_OBSERVER = "taplusListener";

    @NonNull
    @Override
    protected TileConfig onCreateTileConfig() {
        return new TileConfig.Builder()
            .setTileClass(findClassIfExists("com.android.systemui.qs.tiles.NightModeTile"))
            .setTileName("taplus_tile")
            .setTileProvider("nightModeTileProvider")
            .setLabelResId(R.string.tiles_taplus)
            .setIcons(
                R.drawable.ic_control_center_taplustile_on,
                R.drawable.ic_control_center_taplustile_off
            )
            .build();
    }

    @Override
    protected boolean onCheckAvailable(TileContext ctx) {
        return true;
    }

    @Override
    protected void onTileClick(TileContext ctx) {
        Context context = ctx.getContext();
        boolean enabled = isTaplusEnabled(context);
        setTaplusEnabled(context, !enabled);

        ctx.refreshState();
    }

    @Nullable
    @Override
    protected Intent onGetLongClickIntent(TileContext ctx) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setComponent(new ComponentName("com.miui.contentextension", "com.miui.contentextension.setting.activity.MainSettingsActivity"));
        return intent;
    }

    @Override
    protected void onListeningChanged(TileContext ctx, boolean listening) {
        Context context = ctx.getContext();

        if (listening) {
            if (ctx.getAdditionalField(FIELD_CONTENT_OBSERVER) == null) {
                ContentObserver contentObserver = new ContentObserver(new Handler(context.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        ctx.refreshState();
                    }
                };

                context.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(SETTING_KEY_TAPLUS),
                    false,
                    contentObserver
                );

                ctx.setAdditionalField(FIELD_CONTENT_OBSERVER, contentObserver);
            }
        } else {
            ContentObserver contentObserver = ctx.getAdditionalField(FIELD_CONTENT_OBSERVER);
            if (contentObserver != null) {
                context.getContentResolver().unregisterContentObserver(contentObserver);
                ctx.removeAdditionalField(FIELD_CONTENT_OBSERVER);
            }
        }
    }

    @Nullable
    @Override
    protected TileState onUpdateState(TileContext ctx) {
        return TileState.of(isTaplusEnabled(ctx.getContext()));
    }

    private boolean isTaplusEnabled(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), SETTING_KEY_TAPLUS) == 1;
        } catch (Throwable t) {
            XposedLog.e(TAG, "isTaplusEnabled error", t);
            return false;
        }
    }

    private void setTaplusEnabled(Context context, boolean enabled) {
        try {
            Settings.System.putInt(context.getContentResolver(), SETTING_KEY_TAPLUS, enabled ? 1 : 0);
        } catch (Throwable t) {
            XposedLog.e(TAG, "setTaplusEnabled error", t);
        }
    }
}
