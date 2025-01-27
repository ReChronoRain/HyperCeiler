package com.sevtinge.hyperceiler.ui.app.holiday.weather;

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
