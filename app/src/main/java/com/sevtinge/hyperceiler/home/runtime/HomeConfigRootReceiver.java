/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.home.runtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class HomeConfigRootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        HomeConfigRootClient.start(context);
    }
}
