package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.tiles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileConfig;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileContext;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileState;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

public class GmsTile extends TileUtils {

    private static final String CHECK_GMS = "com.google.android.gms";
    private static final String[] GMS_APPS = {
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.vending",
        "com.google.android.syncadapters.contacts",
        "com.google.android.backuptransport",
        "com.google.android.onetimeinitializer",
        "com.google.android.partnersetup",
        "com.google.android.configupdater",
        "com.google.android.ext.shared",
        "com.google.android.printservice.recommendation"
    };

    @NonNull
    @Override
    protected TileConfig onCreateTileConfig() {
        return new TileConfig.Builder()
            .setTileClass(findClassIfExists("com.android.systemui.qs.tiles.ScreenLockTile"))
            .setTileName("custom_GMS")
            .setTileProvider("screenLockTileProvider")
            .setLabelResId(R.string.tiles_gms)
            .setIcons(
                R.drawable.ic_control_center_gms_toggle_on,
                R.drawable.ic_control_center_gms_toggle_off
            )
            .build();
    }

    @Override
    protected boolean onCheckAvailable(TileContext ctx) {
        try {
            ctx.getContext().getPackageManager()
                .getPackageInfo(CHECK_GMS, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            XposedLog.e(TAG, getPackageName(), "GMS not found: " + e.getMessage());
            return false;
        }
    }

    @Override
    protected void onTileClick(TileContext ctx) {
        Context context = ctx.getContext();
        PackageManager pm = context.getPackageManager();

        boolean isCurrentlyEnabled = isGmsEnabled(context);

        int newState = isCurrentlyEnabled
            ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
            : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

        int successCount = 0;
        int failCount = 0;

        for (String gmsApp : GMS_APPS) {
            try {
                pm.getPackageInfo(gmsApp, PackageManager.GET_ACTIVITIES);
                pm.setApplicationEnabledSetting(gmsApp, newState, 0);
                successCount++;XposedLog.d(TAG, getPackageName(),
                    (isCurrentlyEnabled ? "Disabled" : "Enabled") + " GMS app: " + gmsApp);
            } catch (PackageManager.NameNotFoundException e) {
                XposedLog.d(TAG, getPackageName(), "GMS app not installed: " + gmsApp);
            } catch (SecurityException e) {
                failCount++;
                XposedLog.e(TAG, getPackageName(), "Permission denied for: " + gmsApp + " - " + e.getMessage());
            } catch (Exception e) {
                failCount++;
                XposedLog.e(TAG, getPackageName(), "Failed to toggle: " + gmsApp + " - " + e.getMessage());
            }
        }

        XposedLog.d(TAG, getPackageName(),
            "Toggle complete: success=" + successCount + ", failed=" + failCount);

        ctx.refreshState();
    }

    @Nullable
    @Override
    protected Intent onGetLongClickIntent(TileContext ctx) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setComponent(new ComponentName(
            "com.miui.securitycenter",
            "com.miui.googlebase.ui.GmsCoreSettings"
        ));
        return intent;
    }

    @Nullable
    @Override
    protected TileState onUpdateState(TileContext ctx) {
        boolean isEnabled = isGmsEnabled(ctx.getContext());

        XposedLog.d(TAG, getPackageName(), "onUpdateState: isEnabled=" + isEnabled);

        return new TileState(
            isEnabled,
            isEnabled
                ? R.drawable.ic_control_center_gms_toggle_on
                : R.drawable.ic_control_center_gms_toggle_off
        );
    }

    /**
     * 检查 GMS 是否启用
     */
    private boolean isGmsEnabled(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            int state = pm.getApplicationEnabledSetting(CHECK_GMS);

            boolean enabled = (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                || state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);

            XposedLog.d(TAG, getPackageName(),
                "isGmsEnabled: state=" + state + ", enabled=" + enabled);

            return enabled;
        } catch (Exception e) {
            XposedLog.e(TAG, getPackageName(), "Failed to check GMS state: " + e.getMessage());
            return false;
        }
    }
}
