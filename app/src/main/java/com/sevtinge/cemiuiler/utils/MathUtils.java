package com.sevtinge.cemiuiler.utils;

import android.annotation.SuppressLint;
import android.graphics.Rect;

import com.sevtinge.cemiuiler.utils.log.AndroidLogUtils;

import java.lang.reflect.Method;

public class MathUtils {
    private static final float DEG_TO_RAD = 0.017453292f;
    private static final float RAD_TO_DEG = 57.295784f;

    private MathUtils() {
    }

    public static float abs(float v) {
        return v > 0.0f ? v : -v;
    }

    public static int constrain(int amount, int low, int high) {
        return amount < low ? low : Math.min(amount, high);
    }

    public static long constrain(long amount, long low, long high) {
        return amount < low ? low : Math.min(amount, high);
    }

    public static float constrain(float amount, float low, float high) {
        return amount < low ? low : Math.min(amount, high);
    }

    public static float log(float a) {
        return (float) Math.log(a);
    }

    public static float exp(float a) {
        return (float) Math.exp(a);
    }

    public static float pow(float a, float b) {
        return (float) Math.pow(a, b);
    }

    public static float sqrt(float a) {
        return (float) Math.sqrt(a);
    }

    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    public static float max(int a, int b) {
        return Math.max(a, b);
    }

    public static float max(float a, float b, float c) {
        if (a > b) {
            if (a > c) {
                return a;
            }
        } else if (b > c) {
            return b;
        }
        return c;
    }

    public static float max(int a, int b, int c) {
        int i;
        if (a > b) {
            i = Math.max(a, c);
        } else {
            i = Math.max(b, c);
        }
        return i;
    }

    public static float min(float a, float b) {
        return Math.min(a, b);
    }

    public static float min(int a, int b) {
        return Math.min(a, b);
    }

    public static float min(float a, float b, float c) {
        if (a < b) {
            if (a < c) {
                return a;
            }
        } else if (b < c) {
            return b;
        }
        return c;
    }

    public static float min(int a, int b, int c) {
        int i;
        if (a < b) {
            i = Math.min(a, c);
        } else {
            i = Math.min(b, c);
        }
        return i;
    }

    public static float dist(float x1, float y1, float x2, float y2) {
        float x = x2 - x1;
        float y = y2 - y1;
        return (float) Math.hypot(x, y);
    }

    public static float dist(float x1, float y1, float z1, float x2, float y2, float z2) {
        float x = x2 - x1;
        float y = y2 - y1;
        float z = z2 - z1;
        return (float) Math.sqrt((x * x) + (y * y) + (z * z));
    }

    public static float mag(float a, float b) {
        return (float) Math.hypot(a, b);
    }

    public static float mag(float a, float b, float c) {
        return (float) Math.sqrt((a * a) + (b * b) + (c * c));
    }

    public static float sq(float v) {
        return v * v;
    }

    public static float dot(float v1x, float v1y, float v2x, float v2y) {
        return (v1x * v2x) + (v1y * v2y);
    }

    public static float cross(float v1x, float v1y, float v2x, float v2y) {
        return (v1x * v2y) - (v1y * v2x);
    }

    public static float radians(float degrees) {
        return DEG_TO_RAD * degrees;
    }

    public static float degrees(float radians) {
        return RAD_TO_DEG * radians;
    }

    public static float acos(float value) {
        return (float) Math.acos(value);
    }

    public static float asin(float value) {
        return (float) Math.asin(value);
    }

    public static float atan(float value) {
        return (float) Math.atan(value);
    }

    public static float atan2(float a, float b) {
        return (float) Math.atan2(a, b);
    }

    public static float tan(float angle) {
        return (float) Math.tan(angle);
    }

    public static float lerp(float start, float stop, float amount) {
        return ((stop - start) * amount) + start;
    }

    public static float lerpNew(float start, float stop, float amount) {
        int value = Math.round((((stop - start) * amount) + start));
        return Math.min(value, stop);
    }

    public static float lerp(int start, int stop, float amount) {
        return lerp(start, stop, amount);
    }

    public static float lerpInv(float a, float b, float value) {
        if (a != b) {
            return (value - a) / (b - a);
        }
        return 0.0f;
    }

    public static float saturate(float value) {
        return constrain(value, 0.0f, 1.0f);
    }

    public static float lerpInvSat(float a, float b, float value) {
        return saturate(lerpInv(a, b, value));
    }

    public static float lerpDeg(float start, float end, float amount) {
        float minAngle = (((end - start) + 180.0f) % 360.0f) - 180.0f;
        return (minAngle * amount) + start;
    }

    public static float norm(float start, float stop, float value) {
        return (value - start) / (stop - start);
    }

    public static float map(float minStart, float minStop, float maxStart, float maxStop, float value) {
        return ((maxStop - maxStart) * ((value - minStart) / (minStop - minStart))) + maxStart;
    }

    public static float constrainedMap(float rangeMin, float rangeMax, float valueMin, float valueMax, float value) {
        return lerp(rangeMin, rangeMax, lerpInvSat(valueMin, valueMax, value));
    }

    public static float smoothStep(float start, float end, float x) {
        return constrain((x - start) / (end - start), 0.0f, 1.0f);
    }

    public static int addOrThrow(int a, int b) throws IllegalArgumentException {
        if (b == 0) {
            return a;
        }
        if (b > 0 && a <= Integer.MAX_VALUE - b) {
            return a + b;
        }
        if (b < 0 && a >= Integer.MIN_VALUE - b) {
            return a + b;
        }
        throw new IllegalArgumentException("Addition overflow: " + a + " + " + b);
    }

    public static void fitRect(Rect outToResize, int largestSide) {
        if (outToResize.isEmpty()) {
            return;
        }
        float maxSize = Math.max(outToResize.width(), outToResize.height());
        try {
            Class<?> rectClass = Class.forName("android.graphics.Rect");
            // 获取带有 @hide 注解的方法名称
            String methodName = "scale";
            // 获取方法
            @SuppressLint("DiscouragedPrivateApi") Method scaleMethod = rectClass.getDeclaredMethod(methodName, float.class);
            // 创建一个 Rect 实例
            Rect rectInstance = new Rect(outToResize);
            // 设置方法可访问（即强行访问）
            scaleMethod.setAccessible(true);
            // 调用带有 @hide 注解的方法
            scaleMethod.invoke(rectInstance, largestSide / maxSize);
        } catch (Exception e) {
            AndroidLogUtils.LogE("Call Method scale error: ", e);
        }
    }

}
