package com.sevtinge.hyperceiler.ui.app.holiday;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.app.holiday.weather.ConfettiManager;
import com.sevtinge.hyperceiler.ui.app.holiday.weather.PrecipType;
import com.sevtinge.hyperceiler.ui.app.holiday.weather.confetto.ConfettoGenerator;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

import fan.navigator.app.NavigatorActivity;

public class HolidayHelper {

    Context mContext;
    View mHolidayView;
    ViewGroup mContentView;
    WeatherView mWeatherView;
    ImageView mHeaderView;

    int mRotation;

    static int opt = 2;
    private static WeakReference<WeatherView> weatherView;
    private static WeakReference<GravitySensor> angleListener;

    public HolidayHelper(Activity activity) {
        mContext = activity;
        mRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        mContentView  = activity.findViewById(android.R.id.content);
        mHolidayView = LayoutInflater.from(mContext).inflate(R.layout.layout_holiday, mContentView, false);
        initialize(activity instanceof NavigatorActivity);
    }

    public void initialize(boolean isNavigatorActivity) {
        if (isNavigatorActivity) {
            setupForNavigatorActivity();
        }
    }

    public void setupForNavigatorActivity() {
        ViewGroup parent = (ViewGroup) mContentView.getParent();
        FrameLayout mNavHostView = mHolidayView.findViewById(R.id.nav_host);

        parent.removeAllViews();

        mNavHostView.addView(mContentView);
        parent.addView(mHolidayView, 0);

        initView();
        initHoliday();
    }

    private void initView() {
        mWeatherView = mHolidayView.findViewById(R.id.weather_view);
        mHeaderView = mHolidayView.findViewById(R.id.holiday_header);
    }

    private void initHoliday() {
        mWeatherView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        weatherView = new WeakReference<>(mWeatherView);
        GravitySensor listener = null;
        if (opt == 1) {
            mWeatherView.setPrecipType(PrecipType.SNOW);
            mWeatherView.setSpeed(50);
            mWeatherView.setEmissionRate(mRotation == Surface.ROTATION_90 || mRotation == Surface.ROTATION_270 ? 8 : 4);
            mWeatherView.setFadeOutPercent(0.75f);
            mWeatherView.setAngle(0);
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)mWeatherView.getLayoutParams();
            lp.height = mContext.getResources().getDisplayMetrics().heightPixels / (mRotation == Surface.ROTATION_90 || mRotation == Surface.ROTATION_270 ? 2 : 3);
            mWeatherView.setLayoutParams(lp);
            setWeatherGenerator(new SnowGenerator(mContext));
            mWeatherView.resetWeather();
            mWeatherView.setVisibility(View.VISIBLE);
            mWeatherView.getConfettiManager().setRotationalVelocity(0, 45);

            listener = new GravitySensor(mContext, mWeatherView);
            listener.setOrientation(mRotation);
            listener.setSpeed(50);
            listener.start();

            mHeaderView.setImageResource(R.drawable.newyear_header);
            mHeaderView.setVisibility(View.VISIBLE);
        } else if (opt == 2) {
            mWeatherView.setPrecipType(PrecipType.SNOW);
            mWeatherView.setSpeed(35);
            mWeatherView.setEmissionRate(mRotation == Surface.ROTATION_90 || mRotation == Surface.ROTATION_270 ? 4 : 2);
            mWeatherView.setFadeOutPercent(0.75f);
            mWeatherView.setAngle(0);
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)mWeatherView.getLayoutParams();
            lp.height = mContext.getResources().getDisplayMetrics().heightPixels / (mRotation == Surface.ROTATION_90 || mRotation == Surface.ROTATION_270 ? 3 : 4);
            mWeatherView.setLayoutParams(lp);
            setWeatherGenerator(new FlowerGenerator(mContext));
            mWeatherView.resetWeather();
            mWeatherView.setVisibility(View.VISIBLE);
            mWeatherView.getConfettiManager().setRotationalVelocity(0, 45);

            listener = new GravitySensor(mContext, mWeatherView);
            listener.setOrientation(mRotation);
            listener.setSpeed(35);
            listener.start();

            mHeaderView.setImageResource(R.drawable.lunar_newyear_header);
            mHeaderView.setVisibility(View.VISIBLE);
        } else if (opt == 3) {
            mWeatherView.setPrecipType(PrecipType.CLEAR);
            mWeatherView.setSpeed(0);
            mWeatherView.setEmissionRate(0.6f);
            mWeatherView.setFadeOutPercent(1.0f);
            mWeatherView.setAngle(0);
            setWeatherGenerator(new CoinGenerator(mContext));
            mWeatherView.resetWeather();
            mWeatherView.setVisibility(View.VISIBLE);
            mWeatherView.getConfettiManager().setRotationalVelocity(0, 15).setTTL(30000);

            mHeaderView.setImageResource(R.drawable.crypto_header);
            mHeaderView.setVisibility(View.VISIBLE);
        }
        angleListener = new WeakReference<>(listener);
    }

    private void setWeatherGenerator(ConfettoGenerator generator) {
        try {
            ConfettiManager manager = weatherView.get().getConfettiManager();
            Field confettoGenerator = ConfettiManager.class.getDeclaredField("confettoGenerator");
            confettoGenerator.setAccessible(true);
            confettoGenerator.set(manager, generator);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
