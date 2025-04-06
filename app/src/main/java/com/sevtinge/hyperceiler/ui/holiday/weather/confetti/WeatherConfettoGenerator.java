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
