package com.sevtinge.hyperceiler.main.page.about.controller;

public class BgEffectDataManager {

    public BgEffectData mDataPhoneLight;
    public BgEffectData mDataPadLight;
    public BgEffectData mDataPhoneDark;
    public BgEffectData mDataPadDark;

    public BgEffectDataManager() {
        mDataPhoneLight = new BgEffectData();
        mDataPhoneLight.uTranslateY = 0.0f;
        mDataPhoneLight.uPoints = new float[]{0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f};
        mDataPhoneLight.uAlphaMulti = 1.0f;
        mDataPhoneLight.uNoiseScale = 1.5f;
        mDataPhoneLight.uPointOffset = 0.2f;
        mDataPhoneLight.uPointRadiusMulti = 1.0f;
        mDataPhoneLight.uSaturateOffset = 0.2f;
        mDataPhoneLight.uLightOffset = 0.1f;
        mDataPhoneLight.uAlphaOffset = 0.5f;
        mDataPhoneLight.uShadowColorMulti = 0.3f;
        mDataPhoneLight.uShadowColorOffset = 0.3f;
        mDataPhoneLight.uShadowNoiseScale = 5.0f;
        mDataPhoneLight.uShadowOffset = 0.01f;
        mDataPhoneLight.colorInterpPeriod = 5.0f;
        mDataPhoneLight.gradientSpeedChange = 1.6f;
        mDataPhoneLight.gradientSpeedRest = 1.05f;
        mDataPhoneLight.gradientColors1 = new float[]{1.0f, 0.9f, 0.94f, 1.0f, 1.0f, 0.84f, 0.89f, 1.0f, 0.97f, 0.73f, 0.82f, 1.0f, 0.64f, 0.65f, 0.98f, 1.0f};
        mDataPhoneLight.gradientColors2 = new float[]{0.58f, 0.74f, 1.0f, 1.0f, 1.0f, 0.9f, 0.93f, 1.0f, 0.74f, 0.76f, 1.0f, 1.0f, 0.97f, 0.77f, 0.84f, 1.0f};
        mDataPhoneLight.gradientColors3 = new float[]{0.98f, 0.86f, 0.9f, 1.0f, 0.6f, 0.73f, 0.98f, 1.0f, 0.92f, 0.93f, 1.0f, 1.0f, 0.56f, 0.69f, 1.0f, 1.0f};

        mDataPadLight = new BgEffectData();
        mDataPadLight.uTranslateY = 0.0f;
        mDataPadLight.uPoints = new float[]{0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f};
        mDataPadLight.uAlphaMulti = 1.0f;
        mDataPadLight.uNoiseScale = 1.5f;
        mDataPadLight.uPointOffset = 0.2f;
        mDataPadLight.uPointRadiusMulti = 1.0f;
        mDataPadLight.uSaturateOffset = 0.2f;
        mDataPadLight.uLightOffset = 0.1f;
        mDataPadLight.uAlphaOffset = 0.5f;
        mDataPadLight.uShadowColorMulti = 0.3f;
        mDataPadLight.uShadowColorOffset = 0.3f;
        mDataPadLight.uShadowNoiseScale = 5.0f;
        mDataPadLight.uShadowOffset = 0.01f;
        mDataPadLight.colorInterpPeriod = 7.0f;
        mDataPadLight.gradientSpeedChange = 1.8f;
        mDataPadLight.gradientSpeedRest = 1.0f;
        mDataPadLight.gradientColors1 = new float[]{0.99f, 0.77f, 0.86f, 1.0f, 0.74f, 0.76f, 1.0f, 1.0f, 0.72f, 0.74f, 1.0f, 1.0f, 0.98f, 0.76f, 0.8f, 1.0f};
        mDataPadLight.gradientColors2 = new float[]{0.66f, 0.75f, 1.0f, 1.0f, 1.0f, 0.86f, 0.91f, 1.0f, 0.74f, 0.76f, 1.0f, 1.0f, 0.97f, 0.77f, 0.84f, 1.0f};
        mDataPadLight.gradientColors3 = new float[]{0.97f, 0.79f, 0.85f, 1.0f, 0.65f, 0.68f, 0.98f, 1.0f, 0.66f, 0.77f, 1.0f, 1.0f, 0.72f, 0.73f, 0.98f, 1.0f};

        mDataPhoneDark = new BgEffectData();
        mDataPhoneDark.uTranslateY = 0.0f;
        mDataPhoneDark.uPoints = new float[]{0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f};
        mDataPhoneDark.uAlphaMulti = 1.0f;
        mDataPhoneDark.uNoiseScale = 1.5f;
        mDataPhoneDark.uPointOffset = 0.4f;
        mDataPhoneDark.uPointRadiusMulti = 1.0f;
        mDataPhoneDark.uSaturateOffset = 0.17f;
        mDataPhoneDark.uLightOffset = 0.0f;
        mDataPhoneDark.uAlphaOffset = 0.5f;
        mDataPhoneDark.uShadowColorMulti = 0.3f;
        mDataPhoneDark.uShadowColorOffset = 0.3f;
        mDataPhoneDark.uShadowNoiseScale = 5.0f;
        mDataPhoneDark.uShadowOffset = 0.01f;
        mDataPhoneDark.colorInterpPeriod = 8.0f;
        mDataPhoneDark.gradientSpeedChange = 1.0f;
        mDataPhoneDark.gradientSpeedRest = 1.0f;
        mDataPhoneDark.gradientColors1 = new float[]{0.2f, 0.06f, 0.88f, 0.4f, 0.3f, 0.14f, 0.55f, 0.5f, 0.0f, 0.64f, 0.96f, 0.5f, 0.11f, 0.16f, 0.83f, 0.4f};
        mDataPhoneDark.gradientColors2 = new float[]{0.07f, 0.15f, 0.79f, 0.5f, 0.62f, 0.21f, 0.67f, 0.5f, 0.06f, 0.25f, 0.84f, 0.5f, 0.0f, 0.2f, 0.78f, 0.5f};
        mDataPhoneDark.gradientColors3 = new float[]{0.58f, 0.3f, 0.74f, 0.4f, 0.27f, 0.18f, 0.6f, 0.5f, 0.66f, 0.26f, 0.62f, 0.5f, 0.12f, 0.16f, 0.7f, 0.6f};

        mDataPadDark = new BgEffectData();
        mDataPadDark.uTranslateY = 0.0f;
        mDataPadDark.uPoints = new float[]{0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f};
        mDataPadDark.uAlphaMulti = 1.0f;
        mDataPadDark.uNoiseScale = 1.5f;
        mDataPadDark.uPointOffset = 0.2f;
        mDataPadDark.uPointRadiusMulti = 1.0f;
        mDataPadDark.uSaturateOffset = 0.0f;
        mDataPadDark.uLightOffset = 0.0f;
        mDataPadDark.uAlphaOffset = 0.5f;
        mDataPadDark.uShadowColorMulti = 0.3f;
        mDataPadDark.uShadowColorOffset = 0.3f;
        mDataPadDark.uShadowNoiseScale = 5.0f;
        mDataPadDark.uShadowOffset = 0.01f;
        mDataPadDark.colorInterpPeriod = 7.0f;
        mDataPadDark.gradientSpeedChange = 1.6f;
        mDataPadDark.gradientSpeedRest = 1.2f;
        mDataPadDark.gradientColors1 = new float[]{0.66f, 0.26f, 0.62f, 0.4f, 0.06f, 0.25f, 0.84f, 0.5f, 0.0f, 0.64f, 0.96f, 0.5f, 0.14f, 0.18f, 0.55f, 0.5f};
        mDataPadDark.gradientColors2 = new float[]{0.07f, 0.15f, 0.79f, 0.5f, 0.11f, 0.16f, 0.83f, 0.5f, 0.06f, 0.25f, 0.84f, 0.5f, 0.66f, 0.26f, 0.62f, 0.5f};
        mDataPadDark.gradientColors3 = new float[]{0.58f, 0.3f, 0.74f, 0.5f, 0.11f, 0.16f, 0.83f, 0.5f, 0.66f, 0.26f, 0.62f, 0.5f, 0.27f, 0.18f, 0.6f, 0.6f};
    }

    public BgEffectData getData(DeviceType deviceType, ThemeMode themeMode) {
        return switch (deviceType.ordinal()) {
            case 0 -> themeMode == ThemeMode.LIGHT ? mDataPhoneLight : mDataPhoneDark;
            case 1 -> themeMode == ThemeMode.LIGHT ? mDataPadLight : mDataPadDark;
            default -> throw new IllegalArgumentException("Unsupported device type: " + deviceType);
        };
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

    public enum DeviceType {
        PHONE,
        TABLET
    }

    public enum ThemeMode {
        LIGHT,
        DARK
    }

}
