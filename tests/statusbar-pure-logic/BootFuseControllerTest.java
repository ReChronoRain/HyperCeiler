/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/** Self-contained JVM tests for the injected boot-fuse state machine. */
public final class BootFuseControllerTest {
    private static final class Store implements BootFuseController.Store {
        String value = "";
        boolean readFails;
        boolean writeFails;
        int writes;
        @Override public String read() { return readFails ? null : value; }
        @Override public boolean write(String value) {
            ++writes;
            if (writeFails) return false;
            this.value = value;
            return true;
        }
        @Override public boolean clear() { value = ""; return true; }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static BootFuseController controller(Store store) {
        return new BootFuseController(store, System::currentTimeMillis, () -> Thread.currentThread().getId());
    }

    public static void main(String[] args) throws Exception {
        normalCompletion();
        twoIncompleteAttemptsTrip();
        explicitReset();
        writeFailureAndCorruptState();
        reentrancyAndConcurrency();
        System.out.println("BootFuseControllerTest passed");
    }

    private static void normalCompletion() {
        Store store = new Store();
        BootFuseController first = controller(store);
        check(first.beginInstall("") == BootFuseController.Decision.ALLOW, "first install allowed");
        check(first.inspectState() == BootFuseController.State.TRYING, "trying marker written");
        check(first.markBootCompleted(), "normal completion clears marker");
        check(first.inspectState() == BootFuseController.State.IDLE, "idle after boot");
    }

    private static void twoIncompleteAttemptsTrip() {
        Store store = new Store();
        check(controller(store).beginInstall("") == BootFuseController.Decision.ALLOW, "attempt one allowed");
        check(controller(store).beginInstall("") == BootFuseController.Decision.ALLOW, "attempt two allowed");
        check(controller(store).beginInstall("") == BootFuseController.Decision.TRIPPED, "attempt three trips");
    }

    private static void explicitReset() {
        Store store = new Store();
        controller(store).beginInstall("");
        controller(store).beginInstall("");
        controller(store).beginInstall("");
        check(controller(store).inspectState() == BootFuseController.State.TRIPPED, "fuse is tripped");
        check(controller(store).beginInstall("reset-1") == BootFuseController.Decision.ALLOW, "explicit reset permits one install");
        check(controller(store).markBootCompleted(), "reset attempt can complete");
    }

    private static void writeFailureAndCorruptState() {
        Store store = new Store();
        store.writeFails = true;
        check(controller(store).beginInstall("") == BootFuseController.Decision.ERROR, "write failure denies install");
        store.writeFails = false;
        store.value = "not-a-snapshot";
        check(controller(store).beginInstall("") == BootFuseController.Decision.ERROR, "corrupt marker denies install");
        store.readFails = true;
        check(controller(store).beginInstall("") == BootFuseController.Decision.ERROR, "read failure denies install");
    }

    private static void reentrancyAndConcurrency() throws Exception {
        Store store = new Store();
        BootFuseController controller = controller(store);
        check(controller.beginInstall("") == BootFuseController.Decision.ALLOW, "first claim");
        check(controller.beginInstall("") == BootFuseController.Decision.REENTERED, "same-thread reentry denied");
        check(store.writes == 1, "reentry does not count an attempt");

        Store concurrentStore = new Store();
        BootFuseController concurrent = controller(concurrentStore);
        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger denied = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    if (concurrent.beginInstall("") == BootFuseController.Decision.ALLOW) allowed.incrementAndGet();
                    else denied.incrementAndGet();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            });
            threads.add(thread);
            thread.start();
        }
        start.countDown();
        for (Thread thread : threads) thread.join();
        check(allowed.get() == 1, "exactly one concurrent claim installs");
        check(denied.get() == 7, "concurrent claims are denied");
    }
}
