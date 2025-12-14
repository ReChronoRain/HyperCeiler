/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.main.page.about.controller;

import static com.sevtinge.hyperceiler.common.utils.PersistConfig.isLunarNewYearThemeView;

public class BgEffectDataManager {
    public BgEffectData dataPadDark;
    public BgEffectData dataPadLight;
    public BgEffectData dataPhoneDark;
    public BgEffectData dataPhoneLight;

    public BgEffectDataManager() {
        BgEffectData bgEffectDataPhoneLight = new BgEffectData(this);
        this.dataPhoneLight = bgEffectDataPhoneLight;
        bgEffectDataPhoneLight.uTranslateY = 0.0f;
        bgEffectDataPhoneLight.uPoints = new float[]{0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f};
        bgEffectDataPhoneLight.uAlphaMulti = 1.0f;
        bgEffectDataPhoneLight.uNoiseScale = 1.5f;
        bgEffectDataPhoneLight.uPointOffset = 0.2f;
        bgEffectDataPhoneLight.uPointRadiusMulti = 1.0f;
        bgEffectDataPhoneLight.uSaturateOffset = 0.2f;
        bgEffectDataPhoneLight.uLightOffset = 0.1f;
        bgEffectDataPhoneLight.uAlphaOffset = 0.5f;
        bgEffectDataPhoneLight.uShadowColorMulti = 0.3f;
        bgEffectDataPhoneLight.uShadowColorOffset = 0.3f;
        bgEffectDataPhoneLight.uShadowNoiseScale = 5.0f;
        bgEffectDataPhoneLight.uShadowOffset = 0.01f;
        bgEffectDataPhoneLight.colorInterpPeriod = 5.0f;
        bgEffectDataPhoneLight.gradientSpeedChange = 1.6f;
        bgEffectDataPhoneLight.gradientSpeedRest = 1.05f;
        if (isLunarNewYearThemeView) {
            bgEffectDataPhoneLight.gradientColors1 = new float[]{1.0f, 0.83f, 0.68f, 1.0f, 0.92f, 0.56f, 0.47f, 1.0f, 0.98f, 0.74f, 0.72f, 1.0f, 1.0f, 0.62f, 0.53f, 1.0f};
            bgEffectDataPhoneLight.gradientColors2 = new float[]{1.0f, 0.83f, 0.68f, 1.0f, 0.92f, 0.56f, 0.47f, 1.0f, 0.98f, 0.74f, 0.72f, 1.0f, 1.0f, 0.62f, 0.53f, 1.0f};
            bgEffectDataPhoneLight.gradientColors3 = new float[]{1.0f, 0.83f, 0.68f, 1.0f, 0.92f, 0.56f, 0.47f, 1.0f, 0.98f, 0.74f, 0.72f, 1.0f, 1.0f, 0.62f, 0.53f, 1.0f};
        } else {
            bgEffectDataPhoneLight.gradientColors1 = new float[]{1.0f, 0.9f, 0.94f, 1.0f, 1.0f, 0.84f, 0.89f, 1.0f, 0.97f, 0.73f, 0.82f, 1.0f, 0.64f, 0.65f, 0.98f, 1.0f};
            bgEffectDataPhoneLight.gradientColors2 = new float[]{0.58f, 0.74f, 1.0f, 1.0f, 1.0f, 0.9f, 0.93f, 1.0f, 0.74f, 0.76f, 1.0f, 1.0f, 0.97f, 0.77f, 0.84f, 1.0f};
            bgEffectDataPhoneLight.gradientColors3 = new float[]{0.98f, 0.86f, 0.9f, 1.0f, 0.6f, 0.73f, 0.98f, 1.0f, 0.92f, 0.93f, 1.0f, 1.0f, 0.56f, 0.69f, 1.0f, 1.0f};
        }

        BgEffectData bgEffectDataPadLight = new BgEffectData(this);
        this.dataPadLight = bgEffectDataPadLight;
        bgEffectDataPadLight.uTranslateY = 0.0f;
        bgEffectDataPadLight.uPoints = new float[]{0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f};
        bgEffectDataPadLight.uAlphaMulti = 1.0f;
        bgEffectDataPadLight.uNoiseScale = 1.5f;
        bgEffectDataPadLight.uPointOffset = 0.2f;
        bgEffectDataPadLight.uPointRadiusMulti = 1.0f;
        bgEffectDataPadLight.uSaturateOffset = 0.2f;
        bgEffectDataPadLight.uLightOffset = 0.1f;
        bgEffectDataPadLight.uAlphaOffset = 0.5f;
        bgEffectDataPadLight.uShadowColorMulti = 0.3f;
        bgEffectDataPadLight.uShadowColorOffset = 0.3f;
        bgEffectDataPadLight.uShadowNoiseScale = 5.0f;
        bgEffectDataPadLight.uShadowOffset = 0.01f;
        bgEffectDataPadLight.colorInterpPeriod = 7.0f;
        bgEffectDataPadLight.gradientSpeedChange = 1.8f;
        bgEffectDataPadLight.gradientSpeedRest = 1.0f;
        if (isLunarNewYearThemeView) {
            bgEffectDataPadLight.gradientColors1 = new float[]{1.0f, 0.83f, 0.68f, 1.0f, 0.92f, 0.56f, 0.47f, 1.0f, 0.98f, 0.74f, 0.72f, 1.0f, 1.0f, 0.62f, 0.53f, 1.0f};
            bgEffectDataPadLight.gradientColors2 = new float[]{1.0f, 0.83f, 0.68f, 1.0f, 0.92f, 0.56f, 0.47f, 1.0f, 0.98f, 0.74f, 0.72f, 1.0f, 1.0f, 0.62f, 0.53f, 1.0f};
            bgEffectDataPadLight.gradientColors3 = new float[]{1.0f, 0.83f, 0.68f, 1.0f, 0.92f, 0.56f, 0.47f, 1.0f, 0.98f, 0.74f, 0.72f, 1.0f, 1.0f, 0.62f, 0.53f, 1.0f};
        } else {
            bgEffectDataPadLight.gradientColors1 = new float[]{0.99f, 0.77f, 0.86f, 1.0f, 0.74f, 0.76f, 1.0f, 1.0f, 0.72f, 0.74f, 1.0f, 1.0f, 0.98f, 0.76f, 0.8f, 1.0f};
            bgEffectDataPadLight.gradientColors2 = new float[]{0.66f, 0.75f, 1.0f, 1.0f, 1.0f, 0.86f, 0.91f, 1.0f, 0.74f, 0.76f, 1.0f, 1.0f, 0.97f, 0.77f, 0.84f, 1.0f};
            bgEffectDataPadLight.gradientColors3 = new float[]{0.97f, 0.79f, 0.85f, 1.0f, 0.65f, 0.68f, 0.98f, 1.0f, 0.66f, 0.77f, 1.0f, 1.0f, 0.72f, 0.73f, 0.98f, 1.0f};
        }

        BgEffectData bgEffectDataPhoneDark = new BgEffectData(this);
        this.dataPhoneDark = bgEffectDataPhoneDark;
        bgEffectDataPhoneDark.uTranslateY = 0.0f;
        bgEffectDataPhoneDark.uPoints = new float[]{0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f};
        bgEffectDataPhoneDark.uAlphaMulti = 1.0f;
        bgEffectDataPhoneDark.uNoiseScale = 1.5f;
        bgEffectDataPhoneDark.uPointOffset = 0.4f;
        bgEffectDataPhoneDark.uPointRadiusMulti = 1.0f;
        bgEffectDataPhoneDark.uSaturateOffset = 0.17f;
        bgEffectDataPhoneDark.uLightOffset = 0.0f;
        bgEffectDataPhoneDark.uAlphaOffset = 0.5f;
        bgEffectDataPhoneDark.uShadowColorMulti = 0.3f;
        bgEffectDataPhoneDark.uShadowColorOffset = 0.3f;
        bgEffectDataPhoneDark.uShadowNoiseScale = 5.0f;
        bgEffectDataPhoneDark.uShadowOffset = 0.01f;
        bgEffectDataPhoneDark.colorInterpPeriod = 8.0f;
        bgEffectDataPhoneDark.gradientSpeedChange = 1.0f;
        bgEffectDataPhoneDark.gradientSpeedRest = 1.0f;
        if (isLunarNewYearThemeView) {
            bgEffectDataPhoneDark.gradientColors1 = new float[]{0.58f, 0.4f, 0.28f, 1.0f, 0.48f, 0.12f, 0.1f, 1.0f, 0.56f, 0.28f, 0.12f, 1.0f, 0.46f, 0.16f, 0.11f, 1.0f};
            bgEffectDataPhoneDark.gradientColors2 = new float[]{0.58f, 0.4f, 0.28f, 1.0f, 0.48f, 0.12f, 0.1f, 1.0f, 0.56f, 0.28f, 0.12f, 1.0f, 0.46f, 0.16f, 0.11f, 1.0f};
            bgEffectDataPhoneDark.gradientColors3 = new float[]{0.58f, 0.4f, 0.28f, 1.0f, 0.48f, 0.12f, 0.1f, 1.0f, 0.56f, 0.28f, 0.12f, 1.0f, 0.46f, 0.16f, 0.11f, 1.0f};
        } else {
            bgEffectDataPhoneDark.gradientColors1 = new float[]{0.2f, 0.06f, 0.88f, 0.4f, 0.3f, 0.14f, 0.55f, 0.5f, 0.0f, 0.64f, 0.96f, 0.5f, 0.11f, 0.16f, 0.83f, 0.4f};
            bgEffectDataPhoneDark.gradientColors2 = new float[]{0.07f, 0.15f, 0.79f, 0.5f, 0.62f, 0.21f, 0.67f, 0.5f, 0.06f, 0.25f, 0.84f, 0.5f, 0.0f, 0.2f, 0.78f, 0.5f};
            bgEffectDataPhoneDark.gradientColors3 = new float[]{0.58f, 0.3f, 0.74f, 0.4f, 0.27f, 0.18f, 0.6f, 0.5f, 0.66f, 0.26f, 0.62f, 0.5f, 0.12f, 0.16f, 0.7f, 0.6f};
        }

        BgEffectData bgEffectDataPadDark = new BgEffectData(this);
        this.dataPadDark = bgEffectDataPadDark;
        bgEffectDataPadDark.uTranslateY = 0.0f;
        bgEffectDataPadDark.uPoints = new float[]{0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f};
        bgEffectDataPadDark.uAlphaMulti = 1.0f;
        bgEffectDataPadDark.uNoiseScale = 1.5f;
        bgEffectDataPadDark.uPointOffset = 0.2f;
        bgEffectDataPadDark.uPointRadiusMulti = 1.0f;
        bgEffectDataPadDark.uSaturateOffset = 0.0f;
        bgEffectDataPadDark.uLightOffset = 0.0f;
        bgEffectDataPadDark.uAlphaOffset = 0.5f;
        bgEffectDataPadDark.uShadowColorMulti = 0.3f;
        bgEffectDataPadDark.uShadowColorOffset = 0.3f;
        bgEffectDataPadDark.uShadowNoiseScale = 5.0f;
        bgEffectDataPadDark.uShadowOffset = 0.01f;
        bgEffectDataPadDark.colorInterpPeriod = 7.0f;
        bgEffectDataPadDark.gradientSpeedChange = 1.6f;
        bgEffectDataPadDark.gradientSpeedRest = 1.2f;
        if (isLunarNewYearThemeView) {
            bgEffectDataPadDark.gradientColors1 = new float[]{0.58f, 0.4f, 0.28f, 1.0f, 0.48f, 0.12f, 0.1f, 1.0f, 0.56f, 0.28f, 0.12f, 1.0f, 0.46f, 0.16f, 0.11f, 1.0f};
            bgEffectDataPadDark.gradientColors2 = new float[]{0.58f, 0.4f, 0.28f, 1.0f, 0.48f, 0.12f, 0.1f, 1.0f, 0.56f, 0.28f, 0.12f, 1.0f, 0.46f, 0.16f, 0.11f, 1.0f};
            bgEffectDataPadDark.gradientColors3 = new float[]{0.58f, 0.4f, 0.28f, 1.0f, 0.48f, 0.12f, 0.1f, 1.0f, 0.56f, 0.28f, 0.12f, 1.0f, 0.46f, 0.16f, 0.11f, 1.0f};
        } else {
            bgEffectDataPadDark.gradientColors1 = new float[]{0.66f, 0.26f, 0.62f, 0.4f, 0.06f, 0.25f, 0.84f, 0.5f, 0.0f, 0.64f, 0.96f, 0.5f, 0.14f, 0.18f, 0.55f, 0.5f};
            bgEffectDataPadDark.gradientColors2 = new float[]{0.07f, 0.15f, 0.79f, 0.5f, 0.11f, 0.16f, 0.83f, 0.5f, 0.06f, 0.25f, 0.84f, 0.5f, 0.66f, 0.26f, 0.62f, 0.5f};
            bgEffectDataPadDark.gradientColors3 = new float[]{0.58f, 0.3f, 0.74f, 0.5f, 0.11f, 0.16f, 0.83f, 0.5f, 0.66f, 0.26f, 0.62f, 0.5f, 0.27f, 0.18f, 0.6f, 0.6f};
        }
    }

    public BgEffectData getData(DeviceType deviceType, ThemeMode themeMode) {
        int ordinal = deviceType.ordinal();
        if (ordinal == 0) {
            return themeMode == ThemeMode.LIGHT ? this.dataPhoneLight : this.dataPhoneDark;
        }
        if (ordinal == 1) {
            return themeMode == ThemeMode.LIGHT ? this.dataPadLight : this.dataPadDark;
        }
        throw new IllegalArgumentException("Unsupported device type: " + deviceType);
    }

    public static class BgEffectData {
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

        public BgEffectData(BgEffectDataManager bgEffectDataManager) {
        }
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
