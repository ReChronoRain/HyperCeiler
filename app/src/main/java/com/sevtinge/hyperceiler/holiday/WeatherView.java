package com.sevtinge.hyperceiler.holiday;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.sevtinge.hyperceiler.holiday.weather.ConfettiManager;
import com.sevtinge.hyperceiler.holiday.weather.PrecipType;
import com.sevtinge.hyperceiler.holiday.weather.WeatherData;
import com.sevtinge.hyperceiler.holiday.weather.confetti.MutableRectSource;
import com.sevtinge.hyperceiler.holiday.weather.confetti.WeatherConfettoGenerator;
import com.sevtinge.hyperceiler.holiday.weather.confetto.ConfettoInfo;

public class WeatherView extends FrameLayout {

    private int angle;
    private double angleRadians;
    private final ConfettiManager confettiManager;
    private final MutableRectSource confettiSource;
    private final ConfettoInfo confettoInfo;
    private float emissionRate;
    private float fadeOutPercent = 1.0f;
    private PrecipType precipType = PrecipType.CLEAR;
    private float scaleFactor = 1.0f;
    private int speed;


    public WeatherView( Context context, AttributeSet attrs) {
        super(context, attrs);
        confettoInfo = new ConfettoInfo(PrecipType.CLEAR, 1.0f, null, 4);
        confettiSource = new MutableRectSource(0, 0, 0, 0);
        confettiManager = new ConfettiManager(context,
                new WeatherConfettoGenerator(confettoInfo), confettiSource, this)
                .setEmissionDuration(Long.MAX_VALUE)
                .enableFadeOut(input -> coerceIn(fadeOutPercent - input, 0f, 1f))
                .animate();
    }


    public MutableRectSource getConfettiSource() {
        return confettiSource;
    }

    public ConfettiManager getConfettiManager() {
        return confettiManager;
    }

    public int getAngle() {
        return angle;
    }

    public void setAngle(int value) {
        angle = value;
        angleRadians = Math.toRadians(value);
        updateVelocities();
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(float value) {
        scaleFactor = value;
        confettoInfo.setScaleFactor(value);
    }

    public double getAngleRadians() {
        return angleRadians;
    }

    public int getSpeed() {
        return speed;
    }

    public final void setSpeed(int value) {
        speed = value;
        updateVelocities();
    }

    public float getFadeOutPercent() {
        return fadeOutPercent;
    }

    public void setFadeOutPercent(float f) {
        fadeOutPercent = f;
    }

    public float getEmissionRate() {
        return emissionRate;
    }

    public void setEmissionRate(float value) {
        emissionRate = value;
        updateEmissionRate();
    }

    public PrecipType getPrecipType() {
        return precipType;
    }

    public void setPrecipType(PrecipType value) {
        precipType = value;
        confettoInfo.setPrecipType(value);
    }

    public void setCustomBitmap(Bitmap bitmap) {
        confettoInfo.setPrecipType(PrecipType.CUSTOM);
        confettoInfo.setCustomBitmap(bitmap);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        setConfettiBoundsToSelf();
    }

    public void setWeatherData(WeatherData weatherData) {
        setPrecipType(weatherData.getPrecipType());
        setEmissionRate(weatherData.getEmissionRate());
        setSpeed(weatherData.getSpeed());
        resetWeather();
    }

    public void resetWeather() {
        confettiManager.animate();
    }

    private void setConfettiBoundsToSelf() {
        // Coerce to prevent asymptotes of the tan() function breaking things
        double offscreenSpawnDistance = coerceIn((float) Math.tan(angleRadians), (float) -5.0d, (float) 5.0d) * getHeight();
        confettiManager.setBound(new Rect(0, 0, getWidth(), getHeight()));
        confettiSource.setBounds(Math.min((int) (-offscreenSpawnDistance), 0), 0, Math.max((int) (getWidth() - offscreenSpawnDistance), getWidth()), 0);
    }

    private void updateEmissionRate() {
        confettiManager.setEmissionRate(emissionRate);
    }

    private void updateVelocities() {
        float yVelocity = ((float) Math.cos(angleRadians)) * speed;
        float xVelocity = ((float) Math.sin(angleRadians)) * speed;
        confettiManager.setVelocityY(yVelocity, yVelocity * 0.05f).setVelocityX(xVelocity, 0.05f * xVelocity).setInitialRotation(-this.angle);
        setConfettiBoundsToSelf();
    }


    public float coerceIn(float f, float minimumValue, float maximumValue) {
        if (f < minimumValue) return minimumValue;
        if (f > minimumValue) return maximumValue;
        return f;
    }
}
