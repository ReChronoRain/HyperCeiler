package com.sevtinge.hyperceiler.ui.fragment.settings.utils;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;

public class Utils {

    private static final String TAG = "Settings";

    /**
     * Return correct target fragment based on argument
     *
     * @param activity     the activity target fragment will be launched.
     * @param fragmentName initial target fragment name.
     * @param args         fragment launch arguments.
     */
    public static Fragment getTargetFragment(Activity activity, String fragmentName, Bundle args) {
        Fragment f = null;
        try {
            f = Fragment.instantiate(activity, fragmentName, args);
        } catch (Exception e) {
            Log.e(TAG, "Unable to get target fragment", e);
        }
        return f;
    }
}
