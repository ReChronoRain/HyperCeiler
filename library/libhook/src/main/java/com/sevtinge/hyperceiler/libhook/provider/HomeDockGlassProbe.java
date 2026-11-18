/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.provider;

import android.content.Context;
import android.os.Bundle;
import android.os.Process;

/** Fixed, unparented HWUI probe in the independent helper, never a desktop layer. */
public final class HomeDockGlassProbe {
    private HomeDockGlassProbe() { }

    public static Bundle run(Context context) {
        if (Process.myUid() != 0) throw new SecurityException("Root helper probe required");
        DockGlassHost host = new DockGlassHost();
        try {
            return host.call(context, "dock_glass_self_test", null, null);
        } finally {
            host.close();
        }
    }
}
