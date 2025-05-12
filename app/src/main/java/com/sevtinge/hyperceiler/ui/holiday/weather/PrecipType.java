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

public enum PrecipType implements WeatherData {

    CLEAR {
        private final float emissionRate = 0f;
        private final int speed = 0;

        @Override
        public float getEmissionRate() {
            return emissionRate;
        }

        @Override
        public int getSpeed() {
            return speed;
        }
    },

    SNOW {
        private final float emissionRate = 10f;
        private final int speed = 250;

        @Override
        public float getEmissionRate() {
            return emissionRate;
        }

        @Override
        public int getSpeed() {
            return speed;
        }
    },

    RAIN {
        private final float emissionRate = 100f;
        private final int speed = (int) (250 * (5.5 / 1.5));

        @Override
        public float getEmissionRate() {
            return emissionRate;
        }

        @Override
        public int getSpeed() {
            return speed;
        }
    },

    CUSTOM {
        private final float emissionRate = 10f;
        private final int speed = 250;

        @Override
        public float getEmissionRate() {
            return emissionRate;
        }

        @Override
        public int getSpeed() {
            return speed;
        }
    };

    @Override
    public PrecipType getPrecipType() {
        return this;
    }
}
