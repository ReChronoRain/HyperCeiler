package com.sevtinge.hyperceiler.common.base;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.common.utils.PrefsConfigurator;

import fan.preference.PreferenceFragment;

public abstract class BasePreferenceFragment extends PreferenceFragment {

    public int getThemeRes() {
        return 0;
    }

    public abstract int getPreferenceScreenResId();

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        onCreatePreferencesBefore(savedInstanceState, rootKey);
        onCreatePreferencesAfter(savedInstanceState, rootKey);
    }

    public void initPrefs() {}

    public void onCreatePreferencesBefore(Bundle savedInstanceState, String rootKey) {
        PrefsConfigurator.setup(this);
        if (getPreferenceScreenResId() != 0) {
            setPreferencesFromResource(getPreferenceScreenResId(), rootKey);
            initPrefs();
        }
    }

    public void onCreatePreferencesAfter(Bundle savedInstanceState, String rootKey) {

    }

    public void setTitle(int titleResId) {
        setTitle(getResources().getString(titleResId));
    }

    public void setTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            getActivity().setTitle(title);
        }
    }

    public String getFragmentName(Fragment fragment) {
        return fragment.getClass().getName();
    }

    public String getPreferenceTitle(Preference preference) {
        return preference.getTitle().toString();
    }

    public String getPreferenceKey(Preference preference) {
        return preference.getKey();
    }

    public void finish() {
        getActivity().finish();
    }

    protected final Context getPrefContext() {
        return getPreferenceManager().getContext();
    }

    protected final boolean isActivityAlive() {
        Activity activity = getActivity();
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    protected final boolean isFragmentContextAvailable() {
        return isAdded() && getContext() != null && isActivityAlive();
    }

    protected final boolean isFragmentUiAvailable() {
        return isFragmentContextAvailable() && getView() != null;
    }

    @Nullable
    protected final Context getSafeContext() {
        return isFragmentContextAvailable() ? getContext() : null;
    }

    protected final void runOnUiThreadIfAlive(@NonNull Runnable action) {
        if (!isFragmentContextAvailable()) {
            return;
        }
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (isFragmentContextAvailable()) {
                action.run();
            }
            return;
        }
        activity.runOnUiThread(() -> {
            if (isFragmentContextAvailable()) {
                action.run();
            }
        });
    }

    protected final void postIfViewAlive(@Nullable View view, @NonNull Runnable action) {
        if (view == null) {
            return;
        }
        view.post(() -> {
            if (isFragmentUiAvailable()) {
                action.run();
            }
        });
    }
}
