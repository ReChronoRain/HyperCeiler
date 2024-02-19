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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArrayMap;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.utils.MathUtils;
import com.sevtinge.hyperceiler.utils.TileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class FlashLight extends TileUtils {
    public final String mtk = "/sys/class/flashlight_core/flashlight/torchbrightness";
    public final String torch = "/sys/class/leds/led:torch_0/brightness";
    public final String other = "/sys/class/leds/flashlight/brightness";
    public final String flashSwitch = "/sys/class/leds/led:switch_0/brightness";
    public final String maxBrightness = "/sys/class/leds/led:torch_0/max_brightness";
    public boolean isListening = false;
    public boolean isHook = false;

    @Override
    public void init() {
        super.init();
        setPermission(mtk);
        setPermission(torch);
        setPermission(other);
        initListen();
        hookBrightness();
    }

    @Override
    public Class<?> customClass() {
        return findClassIfExists("com.android.systemui.qs.tiles.MiuiFlashlightTile");
    }

    @Override
    public ArrayMap<String, Integer> tileUpdateState(XC_MethodHook.MethodHookParam param, Class<?> mResourceIcon, String tileName) {
        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
        if (param.args[1] != null) {
            boolean enabled = (boolean) param.args[1];
            Object flash = XposedHelpers.getObjectField(param.thisObject, "flashlightController");
            boolean isEnabled = (boolean) XposedHelpers.callMethod(flash, "isEnabled");
            if (enabled) {
                if (!isFlashLightEnabled(mContext)) {
                    setFlashLightEnabled(mContext, 1);
                }
            } else if (isEnabled) {
                if (!isFlashLightEnabled(mContext)) {
                    setFlashLightEnabled(mContext, 1);
                }
            } else setFlashLightEnabled(mContext, 0);
        }
        return null;
    }

    private void initListen() {
        hookAllConstructors("com.android.systemui.qs.tiles.MiuiFlashlightTile",
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    listening(mContext);
                }
            }
        );
    }

    public void listening(Context mContext) {
        if (!isListening) {
            ContentObserver contentObserver = new ContentObserver(new Handler(mContext.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    super.onChange(selfChange, uri);
                    isHook = isFlashLightEnabled(mContext);
                }
            };
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("flash_light_enabled"),
                false, contentObserver);
            this.isListening = true;
        }
    }

    private void hookBrightness() {
        if (!isMoreAndroidVersion(34)) {
            findAndHookMethod("com.android.systemui.controlcenter.policy.MiuiBrightnessController",
                "lambda$onChanged$0", boolean.class, float.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if (isHook) {
                            param.setResult(null);
                        }
                    }
                }
            );

            findAndHookMethod("com.android.systemui.controlcenter.policy.MiuiBrightnessController$5",
                "run",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if (isHook) {
                            param.setResult(null);
                        }
                    }
                }
            );
            hookBrightnessUtils();
        } else {
            findAndHookMethod("com.android.systemui.controlcenter.policy.MiuiBrightnessController$$ExternalSyntheticLambda0",
                "run",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if (isHook) {
                            param.setResult(null);
                        }
                    }
                }
            );

            findAndHookMethod("com.android.systemui.controlcenter.policy.MiuiBrightnessController$2",
                "run",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if (isHook) {
                            param.setResult(null);
                        }
                    }
                }
            );
            hookBrightnessUtils();
        }
    }

    private void hookBrightnessUtils() {
        Class<?> BrightnessUtils = findClassIfExists("com.android.systemui.controlcenter.policy.BrightnessUtils");
        if (BrightnessUtils == null) {
            logE(TAG, lpparam.packageName, "Class com.android.systemui.controlcenter.policy.BrightnessUtils is null!!");
            return;
        }
        try {
            BrightnessUtils.getDeclaredMethod("convertGammaToLinearFloat", int.class, float.class, float.class);
            convertGammaToLinearFloat(BrightnessUtils, true);
        } catch (NoSuchMethodException e) {
            try {
                BrightnessUtils.getDeclaredMethod("convertGammaToLinearFloat", float.class, float.class, int.class);
                convertGammaToLinearFloat(BrightnessUtils, false);
            } catch (NoSuchMethodException ex) {
                logE(TAG, lpparam.packageName, "Find Method convertGammaToLinearFloat is null!!");
            }
        }
    }

    private void convertGammaToLinearFloat(Class<?> clz, boolean b) {
        int maxBrightness = maxBrightness();
        findAndHookMethod(clz, "convertGammaToLinearFloat",
            b ? int.class : float.class, float.class, b ? float.class : int.class,
            new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    if (isHook) {
                        // logE(TAG, this.lpparam.packageName, "convertGammaToLinearFloat int 1: " + param.args[0]);
                        // logE(TAG, this.lpparam.packageName, "convertGammaToLinearFloat float 2: " + param.args[1]);
                        // logE(TAG, this.lpparam.packageName, "convertGammaToLinearFloat float 3: " + param.args[2]);
                        float min = (float) param.args[b ? 1 : 0];
                        float max = (float) param.args[b ? 2 : 1];
                        if (min < 0.001f) {
                            min = 0.00114514f;
                        }
                        min = Math.round(min * 500);
                        max = Math.round(max * 500);
                        float exp;
                        Class<?> BrightnessUtils = XposedHelpers.findClass("com.android.systemui.controlcenter.policy.BrightnessUtils",
                            lpparam.classLoader);
                        int GAMMA_SPACE_MAX = XposedHelpers.getStaticIntField(BrightnessUtils, "GAMMA_SPACE_MAX");
                        float R = XposedHelpers.getStaticFloatField(BrightnessUtils, "R");
                        float A = XposedHelpers.getStaticFloatField(BrightnessUtils, "A");
                        float B = XposedHelpers.getStaticFloatField(BrightnessUtils, "B");
                        float C = XposedHelpers.getStaticFloatField(BrightnessUtils, "C");
                        float norm = MathUtils.norm(0.0f, GAMMA_SPACE_MAX, (int) param.args[b ? 0 : 2]);
                        if (norm <= R) {
                            exp = MathUtils.sq(norm / R);
                        } else {
                            exp = MathUtils.exp((norm - C) / A) + B;
                        }
                        if (min < 10) {
                            min = 12;
                        }
                        // logE("FlashLight", "convertGammaToLinearFloat R: " + R + " A: " + A + " B: " + B + " C: " + C);
                        // logE("FlashLight", "convertGammaToLinearFloat exp: " + exp);
                        float end = MathUtils.lerpNew(min, max, (MathUtils.constrain(exp, 0.0f, 12.0f) / 12.0f));
                        // logE("FlashLight", "convertGammaToLinearFloat min: " + min);
                        // logE("FlashLight", "convertGammaToLinearFloat max: " + max);
                        // logE("FlashLight", "convertGammaToLinearFloat end: " + end);
                        int i = Math.round(end);
                        if (i != 0) {
                            if (maxBrightness != -1 && i > maxBrightness) {
                                i = maxBrightness;
                            }
                            // logE(TAG, this.lpparam.packageName, "convertGammaToLinearFloat i: " + i);
                            writeFile(i);
                        }
                        param.setResult(end);
                    }
                }
            }
        );
    }

    private int maxBrightness() {
        String line;
        BufferedReader reader = null;
        StringBuilder builder = null;
        File file = new File(maxBrightness);
        if (file.exists()) {
            try {
                reader = new BufferedReader(new FileReader(maxBrightness));
                builder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } catch (IOException e) {
                logE(TAG, this.lpparam.packageName, "Error to read: " + maxBrightness, e);
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    logE(TAG, this.lpparam.packageName, "Close reader error: ", e);
                }
            }
        } else {
            logE(TAG, this.lpparam.packageName, "Not Found FlashLight File: " + maxBrightness);
        }

        if (builder != null) {
            try {
                return Integer.parseInt(builder.toString());
            } catch (NumberFormatException e) {
                logE(TAG, lpparam.packageName, "To int E: " + e);
            }
        }
        return -1;
    }

    private void writeFile(int flash) {
        if (exists(mtk)) write(mtk, flash);
        if (exists(torch)) write(torch, flash);
        if (exists(other)) write(other, flash);
    }

    private boolean exists(String path) {
        File file = new File(path);
        return file.exists();
    }

    private void write(String path, int flash) {
        try (FileWriter writer = new FileWriter(path, false)) {
            writer.write(Integer.toString(flash));
            writer.flush();
        } catch (IOException e) {
            logE(TAG, lpparam.packageName, "write " + path + " E: " + e);
        }
    }

    private void setPermission(String paths) {
        // 指定文件的路径
        Path filePath = Paths.get(paths);

        try {
            // 获取当前文件的权限
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(filePath);

            // 添加世界可读写权限
            permissions.add(PosixFilePermission.OTHERS_READ);
            permissions.add(PosixFilePermission.OTHERS_WRITE);
            permissions.add(PosixFilePermission.GROUP_READ);
            permissions.add(PosixFilePermission.GROUP_WRITE);

            // 设置新的权限
            Files.setPosixFilePermissions(filePath, permissions);
        } catch (IOException e) {
            logE(TAG, lpparam.packageName, "SetPermission: " + e);
        }
    }

    private boolean isFlashLightEnabled(Context context) {
        try {
            logE(TAG, "isFlashLightEnabled: " + Settings.System.getInt(context.getContentResolver(), "flash_light_enabled"));
            return Settings.System.getInt(context.getContentResolver(), "flash_light_enabled") == 1;
        } catch (Settings.SettingNotFoundException e) {
            logE(TAG, this.lpparam.packageName, "not found flash_light_enabled: " + e);
            setFlashLightEnabled(context, 0);
            return false;
        }
    }

    private void setFlashLightEnabled(Context context, int set) {
        logE(TAG, "setFlashLightEnabled: ");
        Settings.System.putInt(context.getContentResolver(), "flash_light_enabled", set);
    }
}
