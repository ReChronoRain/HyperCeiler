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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.ui.holiday.weather.confetto;

import android.graphics.Bitmap;

import com.sevtinge.hyperceiler.ui.holiday.weather.PrecipType;

public final class ConfettoInfo {
    private Bitmap mCustomBitmap;
    private PrecipType mPrecipType;
    private float mScaleFactor;

    public ConfettoInfo(PrecipType type) {
        this(type, 0f);
    }

    public ConfettoInfo(PrecipType type, float scale) {
        this(type, scale, null);
    }

    public ConfettoInfo(PrecipType type, float scale, Bitmap bitmap) {
        mPrecipType = type;
        mScaleFactor = scale;
        mCustomBitmap = bitmap;
    }

    public ConfettoInfo(PrecipType type, float f, Bitmap bitmap, int i) {
        this(type, f, (i & 4) != 0 ? null : bitmap);
    }

    public Bitmap getCustomBitmap() {
        return mCustomBitmap;
    }

    public void setCustomBitmap(Bitmap bitmap) {
        mCustomBitmap = bitmap;
    }

    public PrecipType getPrecipType() {
        return mPrecipType;
    }

    public void setPrecipType(PrecipType type) {
        mPrecipType = type;
    }

    public float getScaleFactor() {
        return mScaleFactor;
    }

    public void setScaleFactor(float scale) {
        mScaleFactor = scale;
    }
}
