/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.systemframework.statusbarfollow;

import java.util.concurrent.Executor;

/** Self-contained JVM lifecycle/identity tests for the copied server lease. */
public final class ServerBarLifecycleTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Executor owner = Runnable::run;
        ServerBarFollowLifecycle lifecycle = new ServerBarFollowLifecycle();

        try (ServerBarFollowLifecycle.Permit stock = lifecycle.enter(new Object(), new Object())) {
            check(stock != null, "unmanaged controller retains stock behavior");
        }

        Object controller = new Object();
        Object callback = new Object();
        lifecycle.begin(controller, owner, committed -> committed.run());
        lifecycle.created(callback, controller);
        try (ServerBarFollowLifecycle.Permit permit = lifecycle.enter(callback, controller)) {
            check(permit != null, "matching tagged callback enters active lifetime");
            check(lifecycle.enter(new Object(), controller) == null, "untagged callback is denied");
        }

        lifecycle.retire(controller);
        check(lifecycle.isRetired(controller), "restore return and Surface commit retire lease");
        check(lifecycle.enter(callback, controller) == null, "retired lifetime rejects callbacks");
        check(lifecycle.reserveReload(), "reload reservation succeeds after retirement");
        check(lifecycle.awaitReload(0), "retired lifetimes satisfy reload wait");
        lifecycle.cancelReloadReservation();

        Object restored = new Object();
        lifecycle.restoreRetiredControllers(new Object[]{restored});
        check(lifecycle.isRetired(restored), "restored retired owner remains retired");
        check(lifecycle.retiredControllers().length == 2, "restored owners are retained");
        System.out.println("ServerBarLifecycleTest passed");
    }
}
