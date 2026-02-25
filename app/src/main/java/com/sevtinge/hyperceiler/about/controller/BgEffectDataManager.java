package com.sevtinge.hyperceiler.about.controller;

public class BgEffectDataManager {

    public BgEffectData mPhoneLightData;
    public BgEffectData mPhoneDarkData;

    public BgEffectData mPadLightData;
    public BgEffectData mPadDarkData;

    public enum DeviceType {
        PHONE,
        TABLET
    }

    public enum ThemeMode {
        LIGHT,
        DARK
    }

    public class BgEffectData {
        public float colorInterpPeriod;
        public float[] gradientColors1;
        public float[] gradientColors2;
        public float[] gradientColors3;
        public float gradientSpeedChange;
        public float gradientSpeedRest;
        public float uAlphaMulti;
        public float uAlphaOffset;
        public float uLightOffset;
        public float uNoiseScale;
        public float uPointOffset;
        public float uPointRadiusMulti;
        public float[] uPoints;
        public float uSaturateOffset;
        public float uShadowColorMulti;
        public float uShadowColorOffset;
        public float uShadowNoiseScale;
        public float uShadowOffset;
        public float uTranslateY;
    }

    public BgEffectDataManager() {
        mPhoneLightData = new BgEffectData();
        mPhoneLightData.uTranslateY = 0.0f;
        mPhoneLightData.uPoints = new float[]{0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f};
        mPhoneLightData.uAlphaMulti = 1.0f;
        mPhoneLightData.uNoiseScale = 1.5f;
        mPhoneLightData.uPointOffset = 0.2f;
        mPhoneLightData.uPointRadiusMulti = 1.0f;
        mPhoneLightData.uSaturateOffset = 0.2f;
        mPhoneLightData.uLightOffset = 0.1f;
        mPhoneLightData.uAlphaOffset = 0.5f;
        mPhoneLightData.uShadowColorMulti = 0.3f;
        mPhoneLightData.uShadowColorOffset = 0.3f;
        mPhoneLightData.uShadowNoiseScale = 5.0f;
        mPhoneLightData.uShadowOffset = 0.01f;
        mPhoneLightData.colorInterpPeriod = 5.0f;
        mPhoneLightData.gradientSpeedChange = 1.6f;
        mPhoneLightData.gradientSpeedRest = 1.05f;
        mPhoneLightData.gradientColors1 = new float[]{1.0f, 0.9f, 0.94f, 1.0f, 1.0f, 0.84f, 0.89f, 1.0f, 0.97f, 0.73f, 0.82f, 1.0f, 0.64f, 0.65f, 0.98f, 1.0f};
        mPhoneLightData.gradientColors2 = new float[]{0.58f, 0.74f, 1.0f, 1.0f, 1.0f, 0.9f, 0.93f, 1.0f, 0.74f, 0.76f, 1.0f, 1.0f, 0.97f, 0.77f, 0.84f, 1.0f};
        mPhoneLightData.gradientColors3 = new float[]{0.98f, 0.86f, 0.9f, 1.0f, 0.6f, 0.73f, 0.98f, 1.0f, 0.92f, 0.93f, 1.0f, 1.0f, 0.56f, 0.69f, 1.0f, 1.0f};

        mPadLightData = new BgEffectData();
        mPadLightData.uTranslateY = 0.0f;
        mPadLightData.uPoints = new float[]{0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f};
        mPadLightData.uAlphaMulti = 1.0f;
        mPadLightData.uNoiseScale = 1.5f;
        mPadLightData.uPointOffset = 0.2f;
        mPadLightData.uPointRadiusMulti = 1.0f;
        mPadLightData.uSaturateOffset = 0.2f;
        mPadLightData.uLightOffset = 0.1f;
        mPadLightData.uAlphaOffset = 0.5f;
        mPadLightData.uShadowColorMulti = 0.3f;
        mPadLightData.uShadowColorOffset = 0.3f;
        mPadLightData.uShadowNoiseScale = 5.0f;
        mPadLightData.uShadowOffset = 0.01f;
        mPadLightData.colorInterpPeriod = 7.0f;
        mPadLightData.gradientSpeedChange = 1.8f;
        mPadLightData.gradientSpeedRest = 1.0f;
        mPadLightData.gradientColors1 = new float[]{0.99f, 0.77f, 0.86f, 1.0f, 0.74f, 0.76f, 1.0f, 1.0f, 0.72f, 0.74f, 1.0f, 1.0f, 0.98f, 0.76f, 0.8f, 1.0f};
        mPadLightData.gradientColors2 = new float[]{0.66f, 0.75f, 1.0f, 1.0f, 1.0f, 0.86f, 0.91f, 1.0f, 0.74f, 0.76f, 1.0f, 1.0f, 0.97f, 0.77f, 0.84f, 1.0f};
        mPadLightData.gradientColors3 = new float[]{0.97f, 0.79f, 0.85f, 1.0f, 0.65f, 0.68f, 0.98f, 1.0f, 0.66f, 0.77f, 1.0f, 1.0f, 0.72f, 0.73f, 0.98f, 1.0f};

        mPhoneDarkData = new BgEffectData();
        mPhoneDarkData.uTranslateY = 0.0f;
        mPhoneDarkData.uPoints = new float[]{0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f};
        mPhoneDarkData.uAlphaMulti = 1.0f;
        mPhoneDarkData.uNoiseScale = 1.5f;
        mPhoneDarkData.uPointOffset = 0.4f;
        mPhoneDarkData.uPointRadiusMulti = 1.0f;
        mPhoneDarkData.uSaturateOffset = 0.17f;
        mPhoneDarkData.uLightOffset = 0.0f;
        mPhoneDarkData.uAlphaOffset = 0.5f;
        mPhoneDarkData.uShadowColorMulti = 0.3f;
        mPhoneDarkData.uShadowColorOffset = 0.3f;
        mPhoneDarkData.uShadowNoiseScale = 5.0f;
        mPhoneDarkData.uShadowOffset = 0.01f;
        mPhoneDarkData.colorInterpPeriod = 8.0f;
        mPhoneDarkData.gradientSpeedChange = 1.0f;
        mPhoneDarkData.gradientSpeedRest = 1.0f;
        mPhoneDarkData.gradientColors1 = new float[]{0.2f, 0.06f, 0.88f, 0.4f, 0.3f, 0.14f, 0.55f, 0.5f, 0.0f, 0.64f, 0.96f, 0.5f, 0.11f, 0.16f, 0.83f, 0.4f};
        mPhoneDarkData.gradientColors2 = new float[]{0.07f, 0.15f, 0.79f, 0.5f, 0.62f, 0.21f, 0.67f, 0.5f, 0.06f, 0.25f, 0.84f, 0.5f, 0.0f, 0.2f, 0.78f, 0.5f};
        mPhoneDarkData.gradientColors3 = new float[]{0.58f, 0.3f, 0.74f, 0.4f, 0.27f, 0.18f, 0.6f, 0.5f, 0.66f, 0.26f, 0.62f, 0.5f, 0.12f, 0.16f, 0.7f, 0.6f};

        mPadDarkData = new BgEffectData();
        mPadDarkData.uTranslateY = 0.0f;
        mPadDarkData.uPoints = new float[]{0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f};
        mPadDarkData.uAlphaMulti = 1.0f;
        mPadDarkData.uNoiseScale = 1.5f;
        mPadDarkData.uPointOffset = 0.2f;
        mPadDarkData.uPointRadiusMulti = 1.0f;
        mPadDarkData.uSaturateOffset = 0.0f;
        mPadDarkData.uLightOffset = 0.0f;
        mPadDarkData.uAlphaOffset = 0.5f;
        mPadDarkData.uShadowColorMulti = 0.3f;
        mPadDarkData.uShadowColorOffset = 0.3f;
        mPadDarkData.uShadowNoiseScale = 5.0f;
        mPadDarkData.uShadowOffset = 0.01f;
        mPadDarkData.colorInterpPeriod = 7.0f;
        mPadDarkData.gradientSpeedChange = 1.6f;
        mPadDarkData.gradientSpeedRest = 1.2f;
        mPadDarkData.gradientColors1 = new float[]{0.66f, 0.26f, 0.62f, 0.4f, 0.06f, 0.25f, 0.84f, 0.5f, 0.0f, 0.64f, 0.96f, 0.5f, 0.14f, 0.18f, 0.55f, 0.5f};
        mPadDarkData.gradientColors2 = new float[]{0.07f, 0.15f, 0.79f, 0.5f, 0.11f, 0.16f, 0.83f, 0.5f, 0.06f, 0.25f, 0.84f, 0.5f, 0.66f, 0.26f, 0.62f, 0.5f};
        mPadDarkData.gradientColors3 = new float[]{0.58f, 0.3f, 0.74f, 0.5f, 0.11f, 0.16f, 0.83f, 0.5f, 0.66f, 0.26f, 0.62f, 0.5f, 0.27f, 0.18f, 0.6f, 0.6f};
    }

    public BgEffectData getData(DeviceType deviceType, ThemeMode themeMode) {
        switch (deviceType) {
            case PHONE -> {
                return themeMode == ThemeMode.LIGHT ? mPhoneLightData : mPhoneDarkData;
            }
            case TABLET -> {
                return themeMode == ThemeMode.LIGHT ? mPadLightData : mPadDarkData;
            }
            default -> throw new IllegalArgumentException("Unsupported device type: " + deviceType);
        }
    }
}
