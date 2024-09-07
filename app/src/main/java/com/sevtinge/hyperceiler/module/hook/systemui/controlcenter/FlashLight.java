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

import android.animation.ValueAnimator;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.utils.MathUtils;
import com.sevtinge.hyperceiler.utils.TileUtils;

import org.json.JSONException;
import org.json.JSONObject;

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
    public int mode = 0;
    public int lastFlash = -1;
    public boolean isListening = false;
    public boolean isHook = false;

    @Override
    public void init() {
        super.init();
        mode = mPrefsMap.getStringAsInt("security_flash_light_switch", 0);
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
        hookAllConstructors("com.android.systemui.controlcenter.policy.MiuiBrightnessController",
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    listening(mContext, param);
                }
            }
        );
    }

    public void listening(Context mContext, XC_MethodHook.MethodHookParam param) {
        if (!isListening) {
            ContentObserver contentObserver = new ContentObserver(new Handler(mContext.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    super.onChange(selfChange, uri);
                    if (lastFlash != -1) lastFlash = -1;
                    isHook = isFlashLightEnabled(mContext);
                    if (isHook) {
                        String b = getFlashBrightness(mContext);
                        if (b != null) {
                            JSONObject object = FlashBrightness.restore(b);
                            int flash = FlashBrightness.getBrightness(object);
                            int slider = FlashBrightness.getSlider(object);
                            try {
                                XposedHelpers.callMethod(param.thisObject, "animateSliderTo", slider);
                            } catch (Throwable e) {
                                sliderAnimator(slider, param);
                            }
                            writeFile(flash);
                        }
                    }
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
        }
        hookStop();
        hookBrightnessUtils();
    }

    private void hookStop() {
        findAndHookMethod("com.android.systemui.controlcenter.policy.MiuiBrightnessController",
            "onStop", int.class, new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    if (isHook) {
                        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        if (lastFlash != -1) {
                            JSONObject object = new FlashBrightness((int) param.args[0], lastFlash).toJSON();
                            setFlashBrightness(mContext, object.toString());
                        }
                    }
                }
            }
        );
    }

    private void sliderAnimator(int i, XC_MethodHook.MethodHookParam param) {
        boolean isUserSliding = (boolean) XposedHelpers.getObjectField(param.thisObject, "isUserSliding");
        Object toggleSliderBase = XposedHelpers.getObjectField(param.thisObject, "mControl");
        Object mControl = XposedHelpers.getObjectField(param.thisObject, "mControl");
        if (!isUserSliding && toggleSliderBase != null) {
            boolean mControlValueInitialized = (boolean) XposedHelpers.getObjectField(param.thisObject,
                "mControlValueInitialized");
            if (!mControlValueInitialized) {
                XposedHelpers.callMethod(toggleSliderBase, "setValue", i);
                XposedHelpers.setObjectField(param.thisObject, "mControlValueInitialized", true);
            }
            ValueAnimator ofInt = ValueAnimator.ofInt((int) XposedHelpers.callMethod(mControl,
                "getValue"), i);
            XposedHelpers.setObjectField(param.thisObject, "mSliderAnimator", ofInt);
            ofInt.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                    XposedHelpers.setObjectField(param.thisObject, "mExternalChange", true);
                    if (mControl != null) {
                        XposedHelpers.callMethod(mControl, "setValue", animation.getAnimatedValue());
                    }
                    XposedHelpers.setObjectField(param.thisObject, "mExternalChange", false);
                }
            });
            ofInt.setDuration(3000);
            ofInt.start();
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
                            lastFlash = i;
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
        boolean bmtk = exists(mtk);
        boolean btorch = exists(torch);
        boolean bother = exists(other);
        switch (mode) {
            case 0, 1 -> {
                if (bmtk) write(mtk, flash);
                if (btorch) write(torch, flash);
                if (bother) write(other, flash);
            }
            case 2 -> {
                if (bmtk)
                    zero(mtk, flash);
                if (bother) {
                    zero(other, flash);
                    break; // 根据 CC0126 所述，不同时操作。
                }
                if (btorch)
                    zero(torch, flash);
            }
            case 3 -> {
                if (bmtk)
                    flashSwitch(mtk, flash);

                if (bother) {
                    flashSwitch(other, flash);
                    break; // 根据 CC0126 所述，不同时操作。
                }
                if (btorch)
                    flashSwitch(torch, flash);
            }
        }
    }

    private void zero(String path, int flash) {
        write(path, 0);
        write(path, flash);
    }

    private void flashSwitch(String path, int flash) {
        write(path, flash);
        write(flashSwitch, 1);
        write(flashSwitch, 0);
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
            return Settings.System.getInt(context.getContentResolver(), "flash_light_enabled") == 1;
        } catch (Settings.SettingNotFoundException e) {
            logE(TAG, this.lpparam.packageName, "not found flash_light_enabled: " + e);
            setFlashLightEnabled(context, 0);
            return false;
        }
    }

    private void setFlashLightEnabled(Context context, int set) {
        Settings.System.putInt(context.getContentResolver(), "flash_light_enabled", set);
    }

    private String getFlashBrightness(Context context) {
        return Settings.System.getString(context.getContentResolver(), "flash_light_brightness");
    }

    private void setFlashBrightness(Context context, String set) {
        Settings.System.putString(context.getContentResolver(), "flash_light_brightness", set);
    }

    public static class FlashBrightness {
        private final int slider;

        private final int brightness;

        public FlashBrightness(int s, int b) {
            slider = s;
            brightness = b;
        }

        public JSONObject toJSON() {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("slider", slider);
                jsonObject.put("brightness", brightness);
            } catch (JSONException e) {
                logE("FlashBrightness", "toJSON: " + e);
            }
            return jsonObject;
        }

        public static int getSlider(JSONObject jsonObject) {
            try {
                return jsonObject.getInt("slider");
            } catch (JSONException e) {
                logE("FlashBrightness", "getSlider: " + e);
            }
            return -1;
        }

        public static int getBrightness(JSONObject jsonObject) {
            try {
                return jsonObject.getInt("brightness");
            } catch (JSONException e) {
                logE("FlashBrightness", "getBrightness: " + e);
            }
            return -1;
        }

        public static JSONObject restore(String json) {
            try {
                return new JSONObject(json);
            } catch (JSONException e) {
                logE("FlashBrightness", "restore: " + e);
            }
            return new JSONObject();
        }
    }
}
