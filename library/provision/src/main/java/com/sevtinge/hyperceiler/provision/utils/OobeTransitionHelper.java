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
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.provision.utils;

import android.app.ActivityOptions;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.provision.R;

import java.lang.ref.WeakReference;

public final class OobeTransitionHelper {

    public static final String EXTRA_HOME_REVEAL =
        "com.sevtinge.hyperceiler.extra.OOBE_HOME_REVEAL";
    public static final String EXTRA_PREPARE_HOME =
        "com.sevtinge.hyperceiler.extra.PREPARE_HOME_FOR_OOBE";
    private static boolean sHomeReady;
    private static WeakReference<HomeReadyListener> sHomeReadyListener =
        new WeakReference<>(null);

    private OobeTransitionHelper() {}

    public interface HomeReadyListener {
        void onHomeReadyChanged(boolean ready);
    }

    public static void resetHomeReady() {
        sHomeReady = false;
        notifyHomeReadyChanged();
    }

    public static void markHomeReady() {
        sHomeReady = true;
        notifyHomeReadyChanged();
    }

    public static boolean isHomeReady() {
        return sHomeReady;
    }

    public static void registerHomeReadyListener(@NonNull HomeReadyListener listener) {
        sHomeReadyListener = new WeakReference<>(listener);
        listener.onHomeReadyChanged(sHomeReady);
    }

    public static void unregisterHomeReadyListener(@NonNull HomeReadyListener listener) {
        if (sHomeReadyListener.get() == listener) {
            sHomeReadyListener.clear();
        }
    }

    private static void notifyHomeReadyChanged() {
        HomeReadyListener listener = sHomeReadyListener.get();
        if (listener != null) listener.onHomeReadyChanged(sHomeReady);
    }

    @NonNull
    public static Bundle createPageOptions(@NonNull Context context, boolean forward) {
        int enterAnimation = forward
            ? R.anim.provision_slide_in_right
            : R.anim.provision_slide_in_left;
        int exitAnimation = forward
            ? R.anim.provision_slide_out_left
            : R.anim.provision_slide_out_right;
        return ActivityOptions.makeCustomAnimation(
            context,
            enterAnimation,
            exitAnimation
        ).toBundle();
    }
}
