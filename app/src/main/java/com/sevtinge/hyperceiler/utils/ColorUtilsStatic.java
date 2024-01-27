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
package com.sevtinge.hyperceiler.utils;

import android.graphics.Color;

import androidx.annotation.ColorInt;

public class ColorUtilsStatic {

    /**
     * @return 字符串
     * @color: 参数
     * 类型：int
     * 例如：-1272178
     */
    public static String colorToRGBA(int color) {
        int alpha = color >>> 24;
        int r = (color & 0xff0000) >> 16;
        int g = (color & 0xff00) >> 8;
        int b = color & 0xff;

        return alpha + ", " + r + ", " + g + ", " + b;
    }

    /**
     * @return 字符串
     * @red 红色数值
     * @green 绿色数值
     * @blue 蓝色色数值
     */
    public static String rgbToHex(int red, int green, int blue) {

        String hr = Integer.toHexString(red);
        String hg = Integer.toHexString(green);
        String hb = Integer.toHexString(blue);

        return "#" + hr + hg + hb;
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
        StringBuilder sb = new StringBuilder();
        sb.append("#");
        sb.append(A);
        sb.append(R);
        sb.append(G);
        sb.append(B);
        return sb.toString();
    }
}
