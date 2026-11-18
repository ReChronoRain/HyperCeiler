// SPDX-License-Identifier: AGPL-3.0-or-later
package com.sevtinge.hyperceiler.libhook.provider;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/** Gives the existing module renderer a system-owned binding while its loop is visible. */
public final class XiaoheiziRageService extends Service {
    private final IBinder token = new Binder();

    @Override public IBinder onBind(Intent intent) {
        return token;
    }
}
