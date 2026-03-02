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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.holiday;

import static com.sevtinge.hyperceiler.common.utils.PersistConfig.isLunarNewYearThemeView;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;

import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.holiday.weather.ConfettiManager;
import com.sevtinge.hyperceiler.holiday.weather.PrecipType;
import com.sevtinge.hyperceiler.holiday.weather.confetto.ConfettoGenerator;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Objects;

import fan.appcompat.app.AppCompatActivity;

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

    public static void init(Activity activity) {
        if (isLunarNewYearThemeView) {
            new HolidayHelper(activity);
        }
    }

    public HolidayHelper(Activity activity) {
        mContext = activity;
        mRotation = Objects.requireNonNull(mContext.getDisplay()).getRotation();
        mContentView = activity.findViewById(android.R.id.content);
        mHolidayView = LayoutInflater.from(mContext).inflate(R.layout.layout_holiday, mContentView, false);
        initialize(activity instanceof AppCompatActivity);
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

        final float densityScale = 0.85f;

        mWeatherView.setPrecipType(opt == 3 ? PrecipType.CLEAR : PrecipType.SNOW);
        mWeatherView.setSpeed(opt == 1 ? 50 : (opt == 2 ? 35 : 0));
        float baseEmissionRate = mRotation == Surface.ROTATION_90 || mRotation == Surface.ROTATION_270
            ? (opt == 1 ? 8f : 4f)
            : (opt == 1 ? 4f : 2f);
        mWeatherView.setEmissionRate(baseEmissionRate * densityScale);
        mWeatherView.setFadeOutPercent(opt == 3 ? 1.0f : 0.75f);
        mWeatherView.setAngle(0);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mWeatherView.getLayoutParams();
        lp.height = mContext.getResources().getDisplayMetrics().heightPixels / (mRotation == Surface.ROTATION_90 || mRotation == Surface.ROTATION_270 ? (opt == 1 ? 2 : 3) : (opt == 1 ? 3 : 4));
        mWeatherView.setLayoutParams(lp);
        setWeatherGenerator(opt == 1 ? new SnowGenerator(mContext) : (opt == 2 ? new FlowerGenerator(mContext) : new CoinGenerator(mContext)));
        mWeatherView.resetWeather();
        mWeatherView.setVisibility(View.VISIBLE);
        mWeatherView.getConfettiManager().setRotationalVelocity(0, opt == 3 ? 15 : 45);
        if (opt == 3) {
            mWeatherView.getConfettiManager().setTTL(30000);
        }
        GravitySensor listener = new GravitySensor(mContext, mWeatherView);
        listener.setOrientation(mRotation);
        listener.setSpeed(opt == 1 ? 50 : 35);
        listener.start();

        setupHeaderView(opt);
        angleListener = new WeakReference<>(listener);
    }

    public static void pauseAnimation() {
        if (weatherView != null) {
            WeatherView view = weatherView.get();
            if (view != null) {
                view.getConfettiManager().terminate();
            }
        }

        if (angleListener != null) {
            GravitySensor listener = angleListener.get();
            if (listener != null) {
                listener.stop();
            }
        }
    }

    public static void resumeAnimation() {
        if (weatherView != null) {
            WeatherView view = weatherView.get();
            if (view != null) {
                view.resetWeather();
            }
        }

        if (angleListener != null) {
            GravitySensor listener = angleListener.get();
            if (listener != null) {
                listener.start();
            }
        }
    }

    private void setupHeaderView(int opt) {
        int headerResId = opt == 1 ? R.drawable.newyear_header : (opt == 2 ? R.drawable.lunar_newyear_header : R.drawable.crypto_header);
        mHeaderView.setImageResource(headerResId);

        if (isPad()) {
            int maxHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 92,
                    mContext.getResources().getDisplayMetrics());
            mHeaderView.setMaxHeight(maxHeight);
        }

        mHeaderView.setVisibility(View.VISIBLE);
    }

    private void setWeatherGenerator(ConfettoGenerator generator) {
        try {
            ConfettiManager manager = weatherView.get().getConfettiManager();
            Field confettoGenerator = ConfettiManager.class.getDeclaredField("confettoGenerator");
            confettoGenerator.setAccessible(true);
            confettoGenerator.set(manager, generator);
        } catch (Throwable t) {
            AndroidLog.w("HolidayHelper", "Failed to set custom ConfettoGenerator", t);
        }
    }
}
