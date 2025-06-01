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
package com.sevtinge.hyperceiler.ui.holiday.weather.confetti;

import com.sevtinge.hyperceiler.ui.holiday.weather.ConfettiSource;

public class MutableRectSource extends ConfettiSource {

    private volatile int x;
    private volatile int y;
    private volatile int otherX;
    private volatile int otherY;
    public MutableRectSource(int x, int y) {
        super(x, y);
    }

    public MutableRectSource(int x0, int y0, int x1, int y1) {
        super(x0, y0, x1, y1);
        this.x = x;
        this.y = y;
        this.otherX = otherX;
        this.otherY = otherY;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getOtherX() {
        return otherX;
    }

    public void setOtherX(int otherX) {
        this.otherX = otherX;
    }

    public int getOtherY() {
        return otherY;
    }

    public void setOtherY(int otherY) {
        this.otherY = otherY;
    }

    public int getXRange() {
        return this.otherX - this.x;
    }

    public int getYRange() {
        return this.otherY - this.y;
    }

    @Override
    protected float getInitialX(float random) {
        return (getXRange() * random) + this.x;
    }

    @Override
    protected float getInitialY(float random) {
        return (getYRange() * random) + this.y;
    }

    public void setBounds(int x0, int y0, int x1, int y1) {
        this.x = x0;
        this.y = y0;
        this.otherX = x1;
        this.otherY = y1;
    }
}
