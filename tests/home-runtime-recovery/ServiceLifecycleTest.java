/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.home.runtime;

import android.os.Handler;
import android.os.SystemProperties;

/** Exercises the production onUnbind body, with SDK scheduling and stopSelf fixtures. */
public final class ServiceLifecycleTest {
    public static void main(String[] args) {
        for(String backend:new String[]{"root-socket","root-probe"}) {
            var service=new HomeConfigRootService();SystemProperties.backend=backend;
            if(service.onUnbind(null))throw new AssertionError();Handler.drain();
            if(service.stops!=0)throw new AssertionError("Active daemon must outlive its client");
        }
        for(String backend:new String[]{"wms","java",""}) {
            var service=new HomeConfigRootService();SystemProperties.backend=backend;
            service.onUnbind(null);if(service.stops!=0)throw new AssertionError("Stop must leave libsu's iterator first");
            Handler.drain();if(service.stops!=1)throw new AssertionError("Disabled daemon retained");
        }
        var service=new HomeConfigRootService();SystemProperties.backend="wms";service.onUnbind(null);
        SystemProperties.backend="root-socket";Handler.drain();
        if(service.stops!=0)throw new AssertionError("New active backend must retain its daemon");
        System.out.println("Production service unbind PASS active modes, disabled modes, deferred stop, re-enabled before dispatch");
    }
}
