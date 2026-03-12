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

import static com.sevtinge.hyperceiler.utils.PersistConfig.isLunarNewYearThemeView;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowInsets;
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
    Bitmap mHeaderSourceBitmap;
    Bitmap mHeaderTiledBitmap;
    int mHeaderResId;
    int mLastHeaderWidth = -1;
    int mLastHeaderHeight = -1;
    boolean mUsingTiledHeader = false;

    int mRotation;

    static int opt = 2;
    private static WeakReference<WeatherView> weatherView;
    private static WeakReference<GravitySensor> angleListener;

    public static void init(Activity activity) {
        if (isLunarNewYearThemeView) {
            WeatherView existingWeatherView = activity.findViewById(R.id.weather_view);
            if (activity.findViewById(R.id.holiday_header) != null && existingWeatherView != null) {
                restoreExistingHolidayAnimation(activity, existingWeatherView);
                return;
            }
            new HolidayHelper(activity);
        }
    }

    private static void restoreExistingHolidayAnimation(Activity activity, WeatherView existingWeatherView) {
        if (existingWeatherView == null) {
            return;
        }

        int rotation = Objects.requireNonNull(activity.getDisplay()).getRotation();
        final float densityScale = 0.85f;

        existingWeatherView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        existingWeatherView.setPrecipType(opt == 3 ? PrecipType.CLEAR : PrecipType.SNOW);
        existingWeatherView.setSpeed(opt == 1 ? 50 : (opt == 2 ? 35 : 0));
        float baseEmissionRate = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
                ? (opt == 1 ? 8f : 4f)
                : (opt == 1 ? 4f : 2f);
        existingWeatherView.setEmissionRate(baseEmissionRate * densityScale);
        existingWeatherView.setFadeOutPercent(opt == 3 ? 1.0f : 0.75f);
        existingWeatherView.setAngle(0);

        setWeatherGenerator(existingWeatherView, opt == 1
                ? new SnowGenerator(activity)
                : (opt == 2 ? new FlowerGenerator(activity) : new CoinGenerator(activity)));

        existingWeatherView.getConfettiManager().setRotationalVelocity(0, opt == 3 ? 15 : 45);
        if (opt == 3) {
            existingWeatherView.getConfettiManager().setTTL(30000);
        }
        existingWeatherView.resetWeather();
        existingWeatherView.setVisibility(View.VISIBLE);
        weatherView = new WeakReference<>(existingWeatherView);

        GravitySensor listener = new GravitySensor(activity, existingWeatherView);
        listener.setOrientation(rotation);
        listener.setSpeed(opt == 1 ? 50 : 35);
        listener.start();
        angleListener = new WeakReference<>(listener);
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
        if (parent == null) {
            return;
        }

        FrameLayout mNavHostView = mHolidayView.findViewById(R.id.nav_host);

        parent.removeView(mContentView);

        if (mContentView.getParent() != mNavHostView) {
            mNavHostView.addView(mContentView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }

        if (mHolidayView.getParent() != parent) {
            parent.addView(mHolidayView, 0, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }

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
        mHeaderResId = opt == 1 ? R.drawable.newyear_header : (opt == 2 ? R.drawable.lunar_newyear_header : R.drawable.crypto_header);
        mHeaderView.setImageResource(mHeaderResId);

        if (isPad()) {
            mHeaderView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mHeaderView.setAdjustViewBounds(false);
            bindHeaderToTopBar();
        } else {
            applyPhoneHeaderStyle();
        }

        mHeaderView.setVisibility(View.VISIBLE);
    }

    private void applyPhoneHeaderStyle() {
        if (mHeaderTiledBitmap != null && !mHeaderTiledBitmap.isRecycled()) {
            mHeaderTiledBitmap.recycle();
            mHeaderTiledBitmap = null;
        }
        mUsingTiledHeader = false;
        mLastHeaderWidth = -1;
        mLastHeaderHeight = -1;

        mHeaderView.setImageResource(mHeaderResId);
        mHeaderView.setScaleType(ImageView.ScaleType.FIT_START);
        mHeaderView.setAdjustViewBounds(true);

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mHeaderView.getLayoutParams();
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.leftMargin = 0;
        lp.topMargin = 0;
        mHeaderView.setLayoutParams(lp);
    }

    private void bindHeaderToTopBar() {
        View.OnLayoutChangeListener syncListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                updateHeaderLayout();
        mHolidayView.addOnLayoutChangeListener(syncListener);
        mContentView.addOnLayoutChangeListener(syncListener);
        mHeaderView.post(this::updateHeaderLayout);
    }

    private void updateHeaderLayout() {
        if (mHeaderView == null || mContentView == null) {
            return;
        }

        int headerWidth = Math.max(0, mContentView.getWidth());
        int headerLeft = getLeftRelativeToAncestor(mContentView, mHolidayView);
        int headerTop = getTopRelativeToAncestor(mContentView, mHolidayView);
        int fallbackHeight = resolveStatusBarHeight() + resolveTopBarHeight();
        int targetBottom = headerTop + fallbackHeight;

        View searchAnchorView = findSearchAnchorView();
        if (searchAnchorView != null && searchAnchorView.getHeight() > 0) {
            int searchTop = getTopRelativeToAncestor(searchAnchorView, mHolidayView);
            int searchGap = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2,
                    mContext.getResources().getDisplayMetrics());
            targetBottom = Math.max(targetBottom, searchTop - searchGap);
        }

        int headerHeight = Math.max(1, targetBottom - headerTop);

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mHeaderView.getLayoutParams();
        if (lp.width != headerWidth || lp.height != headerHeight || lp.leftMargin != headerLeft || lp.topMargin != headerTop) {
            lp.width = headerWidth;
            lp.height = headerHeight;
            lp.leftMargin = headerLeft;
            lp.topMargin = headerTop;
            mHeaderView.setLayoutParams(lp);
        }

        applyHeaderImageStrategy(headerWidth, headerHeight);
    }

    private void applyHeaderImageStrategy(int headerWidth, int headerHeight) {
        if (headerWidth <= 0 || headerHeight <= 0 || mHeaderResId == 0) {
            return;
        }

        int tiledThresholdWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 700,
                mContext.getResources().getDisplayMetrics());
        boolean shouldUseTiled = headerWidth > tiledThresholdWidth;

        if (!shouldUseTiled) {
            if (mUsingTiledHeader) {
                mHeaderView.setImageResource(mHeaderResId);
                mUsingTiledHeader = false;
                mLastHeaderWidth = -1;
                mLastHeaderHeight = -1;
            }
            return;
        }

        if (mUsingTiledHeader && mLastHeaderWidth == headerWidth && mLastHeaderHeight == headerHeight && mHeaderTiledBitmap != null) {
            return;
        }

        if (mHeaderSourceBitmap == null) {
            mHeaderSourceBitmap = BitmapFactory.decodeResource(mContext.getResources(), mHeaderResId);
        }
        if (mHeaderSourceBitmap == null || mHeaderSourceBitmap.isRecycled()) {
            return;
        }

        Bitmap tiled = buildHorizontalTiledBitmap(mHeaderSourceBitmap, headerWidth, headerHeight);
        if (tiled == null) {
            return;
        }

        if (mHeaderTiledBitmap != null && !mHeaderTiledBitmap.isRecycled() && mHeaderTiledBitmap != tiled) {
            mHeaderTiledBitmap.recycle();
        }
        mHeaderTiledBitmap = tiled;
        mHeaderView.setImageBitmap(tiled);
        mUsingTiledHeader = true;
        mLastHeaderWidth = headerWidth;
        mLastHeaderHeight = headerHeight;
    }

    private Bitmap buildHorizontalTiledBitmap(Bitmap source, int outWidth, int outHeight) {
        int srcWidth = source.getWidth();
        int srcHeight = source.getHeight();
        if (srcWidth <= 0 || srcHeight <= 0 || outWidth <= 0 || outHeight <= 0) {
            return null;
        }

        float scale = (float) outHeight / (float) srcHeight;
        int tileWidth = Math.max(1, Math.round(srcWidth * scale));

        Bitmap scaledTile = Bitmap.createScaledBitmap(source, tileWidth, outHeight, true);
        if (scaledTile == null) {
            return null;
        }

        Bitmap output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        for (int left = 0; left < outWidth; left += tileWidth) {
            canvas.drawBitmap(scaledTile, left, 0, null);
        }

        if (scaledTile != source && !scaledTile.isRecycled()) {
            scaledTile.recycle();
        }
        return output;
    }

    private int resolveTopBarHeight() {
        TypedValue typedValue = new TypedValue();
        if (mContext.getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            return TypedValue.complexToDimensionPixelSize(typedValue.data, mContext.getResources().getDisplayMetrics());
        }
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 56,
                mContext.getResources().getDisplayMetrics());
    }

    private int resolveStatusBarHeight() {
        if (mHolidayView != null) {
            WindowInsets insets = mHolidayView.getRootWindowInsets();
            if (insets != null) {
                return insets.getInsets(WindowInsets.Type.statusBars()).top;
            }
        }

        int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return mContext.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private View findSearchAnchorView() {
        String[] candidateIds = new String[]{"search_bar", "header_view", "search_container", "search_panel"};
        for (String idName : candidateIds) {
            int resId = mContext.getResources().getIdentifier(idName, "id", mContext.getPackageName());
            if (resId == 0) {
                continue;
            }
            View candidate = mContentView.findViewById(resId);
            if (candidate != null && candidate.getVisibility() == View.VISIBLE && candidate.getWidth() > 0) {
                return candidate;
            }
        }
        return null;
    }

    private int getLeftRelativeToAncestor(View view, View ancestor) {
        int left = 0;
        View current = view;
        while (current != null && current != ancestor) {
            left += current.getLeft();
            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return left;
    }

    private int getTopRelativeToAncestor(View view, View ancestor) {
        int top = 0;
        View current = view;
        while (current != null && current != ancestor) {
            top += current.getTop();
            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return top;
    }

    private void setWeatherGenerator(ConfettoGenerator generator) {
        setWeatherGenerator(weatherView != null ? weatherView.get() : null, generator);
    }

    private static void setWeatherGenerator(WeatherView targetView, ConfettoGenerator generator) {
        try {
            if (targetView == null) {
                return;
            }
            ConfettiManager manager = targetView.getConfettiManager();
            Field confettoGenerator = ConfettiManager.class.getDeclaredField("confettoGenerator");
            confettoGenerator.setAccessible(true);
            confettoGenerator.set(manager, generator);
        } catch (Throwable t) {
            AndroidLog.w("HolidayHelper", "Failed to set custom ConfettoGenerator", t);
        }
    }
}
