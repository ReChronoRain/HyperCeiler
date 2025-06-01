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

import com.sevtinge.hyperceiler.ui.holiday.weather.confetto.Confetto;
import com.sevtinge.hyperceiler.ui.holiday.weather.confetto.ConfettoGenerator;
import com.sevtinge.hyperceiler.ui.holiday.weather.confetto.ConfettoInfo;

import java.util.Random;

public class WeatherConfettoGenerator implements ConfettoGenerator {

    private final ConfettoInfo mConfettoInfo;

    public WeatherConfettoGenerator(ConfettoInfo confettoInfo) {
        mConfettoInfo = confettoInfo;
    }

    public final ConfettoInfo getConfettoInfo() {
        return mConfettoInfo;
    }

    @Override
    public Confetto generateConfetto(Random random) {
        return new MotionBlurBitmapConfetto(mConfettoInfo);
    }
}
