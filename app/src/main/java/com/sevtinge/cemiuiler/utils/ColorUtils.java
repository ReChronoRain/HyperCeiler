package com.sevtinge.cemiuiler.utils;

import android.graphics.Color;

import moralnorm.annotation.ColorInt;

public class ColorUtils {

    /**
     * @color:  参数
     *          类型：int
     *          例如：-1272178
     *
     * @return 字符串
     * */
    public static String colorToRGBA(int color) {
        int alpha = color >>> 24;
        int r = ( color & 0xff0000 ) >> 16;
        int g = ( color & 0xff00 ) >> 8;
        int b = color & 0xff;

        return alpha + ", " + r + ", " + g + ", " + b;
    }

    /**
     * @red     红色数值
     * @green   绿色数值
     * @blue    蓝色色数值
     *
     * @return 字符串
     * */
    public static String rgbToHex(int red, int green, int blue){

        String hr = Integer.toHexString(red);
        String hg = Integer.toHexString(green);
        String hb = Integer.toHexString(blue);

        return  "#"+hr + hg + hb;
    }

    /**
     * 将 颜色值 转化为 #AARRGGBB
     *
     * @param color -1272178
     * @return #AARRGGBB
     */
    public static String colorToHexARGB(@ColorInt int color) {
        // 转化为16进制字符串
        String A = Integer.toHexString(Color.alpha(color));
        String R = Integer.toHexString(Color.red(color));
        String G = Integer.toHexString(Color.green(color));
        String B = Integer.toHexString(Color.blue(color));

        // 判断获取到的R,G,B值的长度 如果长度等于1 给R,G,B值的前边添0
        A = A.length() == 1 ? "0" + A : A;
        R = R.length() == 1 ? "0" + R : R;
        G = G.length() == 1 ? "0" + G : G;
        B = B.length() == 1 ? "0" + B : B;
        //
        StringBuffer sb = new StringBuffer();
        sb.append("#");
        sb.append(A);
        sb.append(R);
        sb.append(G);
        sb.append(B);
        return sb.toString();
    }
}
