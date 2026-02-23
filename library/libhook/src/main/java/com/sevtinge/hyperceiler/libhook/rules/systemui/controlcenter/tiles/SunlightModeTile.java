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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
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
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.utils.shell.ShellUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Objects;

public class SunlightModeTile extends TileUtils {

    // ==================== 常量定义 ====================

    private static final String TILE_NAME = "custom_SUN";

    // 亮度文件路径
    private static final String BRIGHTNESS_FILE_PRIMARY = "/sys/class/mi_display/disp-DSI-0/brightness_clone";
    private static final String BRIGHTNESS_FILE_SECONDARY = "/sys/class/backlight/panel0-backlight/brightness";

    // 系统设置键
    private static final String SETTING_SCREEN_BRIGHTNESS = "screen_brightness";
    private static final String SETTING_SCREEN_BRIGHTNESS_ENABLE = "screen_brightness_enable";
    private static final String SETTING_SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";
    private static final String SETTING_SCREEN_BRIGHTNESS_CUSTOM_MODE = "screen_brightness_custom_mode";
    private static final String SETTING_SUNLIGHT_MODE = "sunlight_mode";

    // 附加字段键
    private static final String FIELD_BROADCAST_RECEIVER = "sunlight_broadcast_receiver";
    private static final String FIELD_CONTENT_OBSERVER = "sunlight_content_observer";
    private static final String FIELD_TILE_INSTANCE = "sunlight_tile_instance";

    // ==================== 运行模式枚举 ====================

    /**
     * 运行模式
     */
    private enum Mode {
        /** 系统阳光模式 */
        SYSTEM_SUNLIGHT,
        /** 强制最高亮度（直接写文件） */
        FORCE_MAX_BRIGHTNESS,
        /** 自定义亮度（Shell 命令） */
        CUSTOM_BRIGHTNESS_SHELL,
        /** 系统 API 最高亮度 */
        SYSTEM_API_MAX_BRIGHTNESS
    }

    // ==================== 状态字段 ====================

    private Mode mMode = Mode.SYSTEM_SUNLIGHT;
    private String mBrightnessPath = null;
    private boolean mUseSystemApi = false;
    private int mCustomBrightness = 2048;

    // 运行时状态（使用 volatile 保证多线程可见性）
    private volatile int mLastBrightness = 0;
    private volatile int mTargetBrightness = 0;
    private volatile boolean mIsListeningBroadcast = false;
    private volatile boolean mIsSunlightActive = false;

    // 磁贴实例引用（用于从广播接收器刷新状态）
    private WeakReference<Object> mTileInstanceRef;

    // ==================== 初始化 ====================

    @Override
    public void init() {
        initMode();
        initBrightnessPath();
        super.init();
    }

    /**
     * 初始化运行模式
     */
    private void initMode() {
        // 优先检查高级模式设置
        int highMode = PrefsBridge.getStringAsInt("system_control_center_sunshine_new_mode_high", 0);
        if (highMode > 0) {
            switch (highMode) {
                case 1 -> mMode = Mode.SYSTEM_SUNLIGHT;
                case 2 -> mMode = Mode.FORCE_MAX_BRIGHTNESS;
                case 3 -> mMode = Mode.CUSTOM_BRIGHTNESS_SHELL;
            }
            mCustomBrightness = PrefsBridge.getInt("system_control_center_sunshine_mode_brightness", 2048);
        } else {
            // 普通模式设置
            int normalMode = PrefsBridge.getStringAsInt("system_control_center_sunshine_new_mode", 0);
            switch (normalMode) {
                case 1 -> mMode = Mode.SYSTEM_SUNLIGHT;
                case 2 -> mMode = Mode.FORCE_MAX_BRIGHTNESS;
            }
        }

        XposedLog.d(TAG, "SunlightMode initialized with mode: " + mMode);
    }

    /**
     * 初始化亮度文件路径
     */
    private void initBrightnessPath() {
        // 根据设置选择路径优先级
        int writeMode = PrefsBridge.getStringAsInt("system_control_center_sunshine_new_mode_write", 1);

        if (writeMode == 1) {
            // 优先使用 mi_display 路径
            if (new File(BRIGHTNESS_FILE_PRIMARY).exists()) {
                mBrightnessPath = BRIGHTNESS_FILE_PRIMARY;
            } else if (new File(BRIGHTNESS_FILE_SECONDARY).exists()) {
                mBrightnessPath = BRIGHTNESS_FILE_SECONDARY;
            }
        } else {
            // 优先使用 backlight 路径
            if (new File(BRIGHTNESS_FILE_SECONDARY).exists()) {
                mBrightnessPath = BRIGHTNESS_FILE_SECONDARY;
            }
        }

        if (mBrightnessPath == null) {
            mUseSystemApi = true;
            XposedLog.w(TAG, "Brightness file not found, falling back to system API");
        } else {
            // 设置文件权限
            ShellUtils.rootExecCmd("chmod 777 " + mBrightnessPath);
            XposedLog.d(TAG, "Using brightness path: " + mBrightnessPath);
        }
    }

    // ==================== TileUtils 实现 ====================

    @NonNull
    @Override
    protected TileConfig onCreateTileConfig() {
        return new TileConfig.Builder()
            .setTileClass(findClassIfExists("com.android.systemui.qs.tiles.PowerSaverTile"))
            .setTileName(TILE_NAME)
            .setTileProvider("powerSaverTileProvider")
            .setLabelResId(R.string.tiles_sunshine_mode)
            .setIcons(R.drawable.baseline_wb_sunny_24, R.drawable.baseline_wb_sunny_24)
            .build();
    }

    @Override
    protected boolean onCheckAvailable(TileContext ctx) {
        return true;
    }

    @Override
    protected void onTileClick(TileContext ctx) {
        Context context = ctx.getContext();

        // 保存磁贴实例引用
        saveTileInstance(ctx);

        try {
            switch (mMode) {
                case SYSTEM_SUNLIGHT -> toggleSystemSunlightMode(context);
                case FORCE_MAX_BRIGHTNESS -> toggleForceBrightness(ctx, context, Integer.MAX_VALUE);
                case CUSTOM_BRIGHTNESS_SHELL -> toggleForceBrightness(ctx, context, mCustomBrightness);
                case SYSTEM_API_MAX_BRIGHTNESS -> toggleSystemApiBrightness(ctx, context);
            }
        } catch (Exception e) {
            XposedLog.e(TAG, "Error handling tile click", e);
        }

        ctx.refreshState();
    }

    @Nullable
    @Override
    protected Intent onGetLongClickIntent(TileContext ctx) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$DisplaySettingsActivity"));
        return intent;
    }

    @Override
    protected void onListeningChanged(TileContext ctx, boolean listening) {
        Context context = ctx.getContext();

        saveTileInstance(ctx);

        if (listening) {
            registerContentObserver(ctx, context);
        } else {
            unregisterContentObserver(ctx, context);
        }
    }

    @Nullable
    @Override
    protected TileState onUpdateState(TileContext ctx) {
        saveTileInstance(ctx);

        boolean isEnabled = checkIsEnabled(ctx.getContext());


        if (!isEnabled && mIsSunlightActive) {
            cleanupOnExternalDisable(ctx);
        }

        mIsSunlightActive = isEnabled;

        return new TileState(isEnabled);
    }

    // ==================== 磁贴实例管理 ====================

    /**
     * 保存磁贴实例引用
     */
    private void saveTileInstance(TileContext ctx) {
        mTileInstanceRef = new WeakReference<>(ctx.getTileInstance());
    }

    /**
     * 刷新磁贴状态（从非 TileContext 环境调用）
     */
    private void refreshTileState() {
        if (mTileInstanceRef != null) {
            Object tile = mTileInstanceRef.get();
            if (tile != null) {
                try {
                    tile.getClass().getMethod("refreshState").invoke(tile);
                } catch (Exception e) {
                    XposedLog.e(TAG, "Failed to refresh tile state", e);
                }
            }
        }
    }

    // ==================== 模式切换逻辑 ====================

    /**
     * 切换系统阳光模式
     */
    private void toggleSystemSunlightMode(Context context) {
        try {
            int currentMode = Settings.System.getInt(context.getContentResolver(), SETTING_SUNLIGHT_MODE);
            int newMode = (currentMode == 1) ? 0 : 1;
            Settings.System.putInt(context.getContentResolver(), SETTING_SUNLIGHT_MODE, newMode);
            mIsSunlightActive = (newMode == 1);
        } catch (Settings.SettingNotFoundException e) {
            XposedLog.e(TAG, "sunlight_mode setting not found", e);
        }
    }

    /**
     * 切换强制亮度模式
     */
    private void toggleForceBrightness(TileContext ctx, Context context, int targetBrightness) {
        if (mUseSystemApi) {
            toggleSystemApiBrightness(ctx, context);
            return;
        }

        int currentBrightness = readBrightness();

        if (!mIsSunlightActive) {
            // 开启高亮模式
            enableSunlightMode(ctx, context, currentBrightness, targetBrightness);
        } else {
            // 关闭高亮模式
            disableSunlightMode(ctx, context);
        }
    }

    /**
     * 启用阳光模式
     */
    private void enableSunlightMode(TileContext ctx, Context context, int currentBrightness, int targetBrightness) {
        handleAutoBrightnessOnEnable(context);
        registerBroadcastReceiver(ctx, context);

        mLastBrightness = currentBrightness;
        writeBrightness(targetBrightness);
        mTargetBrightness = readBrightness();mIsSunlightActive = true;

        XposedLog.d(TAG, "Sunlight mode enabled, last=" + mLastBrightness + ", target=" + mTargetBrightness);
    }

    /**
     * 禁用阳光模式
     */
    private void disableSunlightMode(TileContext ctx, Context context) {
        handleAutoBrightnessOnDisable(context);
        unregisterBroadcastReceiver(ctx, context);

        if (mLastBrightness > 0) {
            writeBrightness(mLastBrightness);
        }

        resetState();

        XposedLog.d(TAG, "Sunlight mode disabled");
    }

    /**
     * 切换系统 API 最高亮度模式
     */
    private void toggleSystemApiBrightness(TileContext ctx, Context context) {
        try {
            if (!mIsSunlightActive) {
                // 开启
                handleAutoBrightnessOnEnable(context);
                registerBroadcastReceiver(ctx, context);

                mLastBrightness = Settings.System.getInt(context.getContentResolver(), SETTING_SCREEN_BRIGHTNESS);
                Settings.System.putInt(context.getContentResolver(), SETTING_SCREEN_BRIGHTNESS, Integer.MAX_VALUE);
                Settings.System.putInt(context.getContentResolver(), SETTING_SCREEN_BRIGHTNESS_ENABLE, 1);
                mIsSunlightActive = true;
            } else {
                // 关闭
                handleAutoBrightnessOnDisable(context);
                unregisterBroadcastReceiver(ctx, context);

                Settings.System.putInt(context.getContentResolver(), SETTING_SCREEN_BRIGHTNESS, mLastBrightness);
                Settings.System.putInt(context.getContentResolver(), SETTING_SCREEN_BRIGHTNESS_ENABLE, 0);

                resetState();
            }
        } catch (Settings.SettingNotFoundException e) {
            XposedLog.e(TAG, "Brightness setting not found", e);
        }
    }

    /**
     * 重置内部状态
     */
    private void resetState() {
        mLastBrightness = 0;
        mTargetBrightness = 0;
        mIsSunlightActive = false;
    }

    /**
     * 外部关闭时清理状态
     */
    private void cleanupOnExternalDisable(TileContext ctx) {
        XposedLog.d(TAG, "Sunlight mode externally disabled, cleaning up");

        if (mIsListeningBroadcast) {
            unregisterBroadcastReceiver(ctx, ctx.getContext());
        }

        resetState();
    }

    // ==================== 状态检查 ====================

    /**
     * 检查当前是否启用
     */
    private boolean checkIsEnabled(Context context) {
        try {
            switch (mMode) {
                case SYSTEM_SUNLIGHT -> {
                    return Settings.System.getInt(context.getContentResolver(), SETTING_SUNLIGHT_MODE) == 1;
                }
                case FORCE_MAX_BRIGHTNESS, CUSTOM_BRIGHTNESS_SHELL -> {
                    if (mUseSystemApi) {
                        return Settings.System.getInt(context.getContentResolver(), SETTING_SCREEN_BRIGHTNESS_ENABLE) == 1;
                    }
                    if (mTargetBrightness == 0) {
                        return false;
                    }
                    int currentBrightness = readBrightness();
                    return currentBrightness == mTargetBrightness;
                }
                case SYSTEM_API_MAX_BRIGHTNESS -> {
                    return Settings.System.getInt(context.getContentResolver(), SETTING_SCREEN_BRIGHTNESS_ENABLE) == 1;
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            XposedLog.e(TAG, "Setting not found while checking state", e);
        }
        return false;
    }

    // ==================== 亮度文件操作 ====================

    /**
     * 读取当前亮度
     */
    private int readBrightness() {
        if (mMode == Mode.CUSTOM_BRIGHTNESS_SHELL) {
            String result = ShellUtils.rootExecCmd("cat " + BRIGHTNESS_FILE_SECONDARY);
            try {
                return Integer.parseInt(result.trim());
            } catch (NumberFormatException e) {
                XposedLog.e(TAG, "Failed to parse brightness: " + result, e);
                return 0;
            }
        }

        if (mBrightnessPath == null) return 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(mBrightnessPath))) {
            String line = reader.readLine();
            return line != null ? Integer.parseInt(line.trim()) : 0;
        } catch (IOException | NumberFormatException e) {
            XposedLog.e(TAG, "Failed to read brightness from: " + mBrightnessPath, e);
            return 0;
        }
    }

    /**
     * 写入亮度值
     */
    private void writeBrightness(int brightness) {
        if (mMode == Mode.CUSTOM_BRIGHTNESS_SHELL) {
            ShellUtils.rootExecCmd("echo " + brightness + " > " + BRIGHTNESS_FILE_SECONDARY);
            return;
        }

        if (mBrightnessPath == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(mBrightnessPath, false))) {
            writer.write(String.valueOf(brightness));
        } catch (IOException e) {
            XposedLog.e(TAG, "Failed to write brightness to: " + mBrightnessPath, e);
        }
    }

    // ==================== 自动亮度处理 ====================

    /**
     * 启用高亮时处理自动亮度
     */
    private void handleAutoBrightnessOnEnable(Context context) {
        if (getBrightnessMode(context) == 1) {
            setCustomBrightnessMode(context, 1);
        }
    }

    /**
     * 禁用高亮时恢复自动亮度
     */
    private void handleAutoBrightnessOnDisable(Context context) {
        if (getCustomBrightnessMode(context) == 1) {
            setCustomBrightnessMode(context, 0);
        }
    }

    private int getBrightnessMode(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), SETTING_SCREEN_BRIGHTNESS_MODE);
        } catch (Settings.SettingNotFoundException e) {
            return -1;
        }
    }

    private void setBrightnessMode(Context context, int value) {
        Settings.System.putInt(context.getContentResolver(), SETTING_SCREEN_BRIGHTNESS_MODE, value);
    }

    private int getCustomBrightnessMode(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), SETTING_SCREEN_BRIGHTNESS_CUSTOM_MODE);
        } catch (Settings.SettingNotFoundException e) {
            setCustomBrightnessMode(context, 0);
            return 0;
        }
    }

    private void setCustomBrightnessMode(Context context, int value) {
        Settings.System.putInt(context.getContentResolver(), SETTING_SCREEN_BRIGHTNESS_CUSTOM_MODE, value);
    }

    // ==================== 广播接收器 ====================

    /**
     * 注册息屏广播接收器
     */
    private void registerBroadcastReceiver(TileContext ctx, Context context) {
        if (mIsListeningBroadcast) return;

        ScreenOffReceiver receiver = new ScreenOffReceiver(context, this);
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);

        ctx.setAdditionalField(FIELD_BROADCAST_RECEIVER, receiver);
        mIsListeningBroadcast = true;

        XposedLog.d(TAG, "Registered screen off receiver");
    }

    /**
     * 注销息屏广播接收器
     */
    private void unregisterBroadcastReceiver(TileContext ctx, Context context) {
        if (!mIsListeningBroadcast) return;

        BroadcastReceiver receiver = ctx.getAdditionalField(FIELD_BROADCAST_RECEIVER);
        if (receiver != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
                // 已经注销
            }
            ctx.removeAdditionalField(FIELD_BROADCAST_RECEIVER);
        }
        mIsListeningBroadcast = false;

        XposedLog.d(TAG, "Unregistered screen off receiver");
    }

    /**
     * 息屏时恢复亮度并刷新磁贴状态
     */
    private void onScreenOff(Context context) {
        if (mLastBrightness == 0) return;

        XposedLog.d(TAG, "Screen off, restoring brightness and refreshing tile");

        // 恢复亮度
        if (!mUseSystemApi && mBrightnessPath != null) {
            writeBrightness(mLastBrightness);
        } else {
            Settings.System.putInt(context.getContentResolver(), SETTING_SCREEN_BRIGHTNESS, mLastBrightness);
            Settings.System.putInt(context.getContentResolver(), SETTING_SCREEN_BRIGHTNESS_ENABLE, 0);
        }

        // 恢复自动亮度
        if (getCustomBrightnessMode(context) == 1) {
            setBrightnessMode(context, 1);
            setCustomBrightnessMode(context, 0);
        }

        // 重置状态
        resetState();

        // 刷新磁贴状态
        refreshTileState();
    }

    /**
     * 息屏广播接收器（静态内部类避免内存泄漏）
     */
    private static class ScreenOffReceiver extends BroadcastReceiver {
        private final WeakReference<Context> contextRef;
        private final WeakReference<SunlightModeTile> tileRef;

        ScreenOffReceiver(Context context, SunlightModeTile tile) {
            this.contextRef = new WeakReference<>(context);
            this.tileRef = new WeakReference<>(tile);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_OFF)) return;

            SunlightModeTile tile = tileRef.get();
            Context ctx = contextRef.get();

            if (tile != null && ctx != null) {
                tile.onScreenOff(ctx);
            }
        }
    }

    // ==================== 内容观察者 ====================

    /**
     * 注册内容观察者
     */
    private void registerContentObserver(TileContext ctx, Context context) {
        // 确保自定义亮度模式设置存在
        getCustomBrightnessMode(context);

        ContentObserver observer = new ContentObserver(new Handler(context.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                super.onChange(selfChange, uri);

                if (uri != null) {
                    String uriString = uri.toString();
                    String customModeUri = Settings.System.getUriFor(SETTING_SCREEN_BRIGHTNESS_CUSTOM_MODE).toString();

                    if (uriString.equals(customModeUri)) {
                        // 同步自动亮度模式
                        syncAutoBrightnessMode(context);
                    }
                }

                // 刷新磁贴状态
                ctx.refreshState();
            }
        };

        context.getContentResolver().registerContentObserver(
            Settings.System.getUriFor(SETTING_SCREEN_BRIGHTNESS), false, observer);context.getContentResolver().registerContentObserver(
            Settings.System.getUriFor(SETTING_SCREEN_BRIGHTNESS_CUSTOM_MODE), false, observer);

        // 对于系统阳光模式，也监听 sunlight_mode
        if (mMode == Mode.SYSTEM_SUNLIGHT) {
            context.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(SETTING_SUNLIGHT_MODE), false, observer);
        }

        ctx.setAdditionalField(FIELD_CONTENT_OBSERVER, observer);
    }

    /**
     * 注销内容观察者
     */
    private void unregisterContentObserver(TileContext ctx, Context context) {
        ContentObserver observer = ctx.getAdditionalField(FIELD_CONTENT_OBSERVER);
        if (observer != null) {
            context.getContentResolver().unregisterContentObserver(observer);ctx.removeAdditionalField(FIELD_CONTENT_OBSERVER);
        }
    }

    /**
     * 同步自动亮度模式
     */
    private void syncAutoBrightnessMode(Context context) {
        int brightnessMode = getBrightnessMode(context);
        int customMode = getCustomBrightnessMode(context);

        if (brightnessMode == 0 && customMode == 0) {
            setBrightnessMode(context, 1);
        } else if (brightnessMode == 1 && customMode == 1) {
            setBrightnessMode(context, 0);
        }
    }
}
