package com.sevtinge.hyperceiler.ui.fragment.settings.core;

import android.app.Activity;
import android.content.Intent;

import com.sevtinge.hyperceiler.ui.fragment.settings.SettingsActivity;

/**
 * This interface marks a class that it wants to listen to
 * {@link Activity#onActivityResult(int, int, Intent)}.
 *
 * Whenever {@link SettingsActivity} receives an activity result, it will
 * propagate the data to this interface so it has a chance to inspect and handle activity results.
 */
public interface OnActivityResultListener {
}