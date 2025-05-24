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
package com.sevtinge.hyperceiler.ui.holiday.weather;

/**
 * The source from which confetti will appear. This can be either a line or a point.
 *
 * <p>Please note that the specified source represents the top left corner of the drawn
 * confetti. If you want the confetti to appear from off-screen, you'll have to offset it
 * with the confetti's size.
 */
public class ConfettiSource {
    public final int x0, y0, x1, y1;

    /**
     * Specifies a point source from which all confetti will emit from.
     *
     * @param x x-coordinate of the point relative to the {@link ConfettiView}'s parent.
     * @param y y-coordinate of the point relative to the {@link ConfettiView}'s parent.
     */
    public ConfettiSource(int x, int y) {
        this(x, y, x, y);
    }

    /**
     * Specifies a line source from which all confetti will emit from.
     *
     * @param x0 x-coordinate of the first point relative to the {@link ConfettiView}'s parent.
     * @param y0 y-coordinate of the first point relative to the {@link ConfettiView}'s parent.
     * @param x1 x-coordinate of the second point relative to the {@link ConfettiView}'s parent.
     * @param y1 y-coordinate of the second point relative to the {@link ConfettiView}'s parent.
     */
    public ConfettiSource(int x0, int y0, int x1, int y1) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
    }

    protected float getInitialX(float random) {
        return x0 + (x1 - x0) * random;
    }

    protected float getInitialY(float random) {
        return y0 + (y1 - y0) * random;
    }
}
