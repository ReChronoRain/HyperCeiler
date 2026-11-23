/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.content.ContentResolver;

/** The launcher endpoint remains prompt on writes without polling the app every 1.5 seconds. */
public final class HomeLayoutRefreshCadenceTest {
    private static final String PREF_URI = "content://com.sevtinge.hyperceiler.provider.sharedprefs/pref";

    private static void check(boolean value, String reason) {
        if (!value) throw new AssertionError(reason);
    }

    private static void awaitQueries(int count) throws InterruptedException {
        final long deadline = System.nanoTime() + 2_000_000_000L;
        while (ContentResolver.queryCount() < count && System.nanoTime() < deadline)
            Thread.sleep(10);
        check(ContentResolver.queryCount() >= count, "notification did not refresh snapshot");
    }

    private static long awaitRecentsLeaseAfter(long previousDeadline) throws InterruptedException {
        final long timeout = System.nanoTime() + 2_000_000_000L;
        do {
            final var preference = HomeLayoutNativeEndpointOS4.recentsBarPreference();
            if (preference.enabled() && preference.deadline() > previousDeadline)
                return preference.deadline();
            Thread.sleep(10);
        } while (System.nanoTime() < timeout);
        throw new AssertionError("enabled status bar policy did not publish a fresh lease");
    }

    private static void checkDemandRecovery(String mode) throws Exception {
        final boolean observerUnavailable = "observer".equals(mode);
        ContentResolver.registrationFails = observerUnavailable;
        ContentResolver.wrongSchema(!observerUnavailable);
        HomeLayoutNativeEndpointOS4.startCombinedPrefetch();
        awaitQueries(1);
        Thread.sleep(observerUnavailable ? 1800 : 3300);
        check(ContentResolver.queryCount() == 1, "fallback retried without a launcher request");

        final var endpoint = new HomeLayoutNativeEndpointOS4((uid, pid) -> uid == 10100 && pid == 42,
            HomeLayoutNativeEndpointOS4::readReadyPreferences);
        final var data = new android.os.Parcel("android.view.IWindowManager");
        android.os.Binder.setCallingIdentityForTest(10101, 42);
        endpoint.receive(data, 0);
        HomeLayoutNativeEndpointOS4.recentsBarPreference();
        Thread.sleep(200);
        check(ContentResolver.queryCount() == 1, "unauthorized request or idle Recents probe woke layout reader");

        android.os.Binder.setCallingIdentityForTest(10100, 42);
        if (observerUnavailable) {
            endpoint.receive(data, 0);
            awaitQueries(2);
        }
        ContentResolver.registrationFails = false;
        ContentResolver.wrongSchema(false);
        ContentResolver.putRow("prefs_key_home_layout_workspace_padding_top", 90);
        if (observerUnavailable) {
            for (int index = 0; index < 20; ++index) endpoint.receive(data, 0);
            Thread.sleep(250);
            check(ContentResolver.queryCount() == 2, "request burst bypassed the recovery backoff");
        } else if ("notification".equals(mode)) {
            ContentResolver.signalChange(PREF_URI);
        } else {
            endpoint.receive(data, 0);
        }
        final int expectedQueries = observerUnavailable ? 3 : 2;
        awaitQueries(expectedQueries);
        awaitPadding(60);
        check(ContentResolver.registrationCount() == 1, "settings observer did not recover");
        endpoint.receive(data, 0);
        Thread.sleep(200);
        check(ContentResolver.queryCount() == expectedQueries, "healthy cache reread the provider on demand");
        check(HomeLayoutNativeEndpointOS4.pauseForHotReload(), "recovered worker failed to retire");
        System.out.println("home layout demand recovery: PASS (" + mode + ")");
    }

    private static void awaitPadding(int expected) throws InterruptedException {
        final long timeout = System.nanoTime() + 2_000_000_000L;
        while (HomeLayoutNativeEndpointOS4.readPreferences().knobDeltaDp()[2] != expected
            && System.nanoTime() < timeout) Thread.sleep(10);
        check(HomeLayoutNativeEndpointOS4.readPreferences().knobDeltaDp()[2] == expected,
            "new physical revision not published");
    }

    public static void main(String[] args) throws Exception {
        var specs = HomeLayoutNativeEndpointOS4.class.getDeclaredField("PREF_KEYS");
        specs.setAccessible(true);
        ContentResolver.setSpecs((String[][]) specs.get(null));
        ContentResolver.recentsExtras = true;
        ContentResolver.setRows(java.util.Map.of(
            "prefs_key_allow_hook", 1,
            "prefs_key_home_layout_workspace_padding_top_enable", 1,
            "prefs_key_home_layout_workspace_padding_top", 60));
        if (args.length != 0) {
            checkDemandRecovery(args[0]);
            return;
        }
        HomeLayoutNativeEndpointOS4.startCombinedPrefetch();
        awaitQueries(1);
        check(ContentResolver.registrationCount() == 1, "observer not registered");
        Thread.sleep(1800);
        check(ContentResolver.queryCount() == 1, "idle endpoint still polls at 1.5 seconds");

        ContentResolver.signalChange("content://com.sevtinge.hyperceiler.provider.sharedprefs/"
            + "pref/integer/prefs_key_unrelated_setting");
        Thread.sleep(200);
        check(ContentResolver.queryCount() == 1, "unrelated setting woke layout reader");

        ContentResolver.putRow("prefs_key_home_layout_workspace_padding_top", 90);
        for (int index = 0; index < 20; ++index) {
            ContentResolver.signalChange("content://com.sevtinge.hyperceiler.provider.sharedprefs/"
                + "pref/integer/prefs_key_home_layout_workspace_padding_top");
        }
        awaitQueries(2);
        awaitPadding(60);
        Thread.sleep(250);
        check(ContentResolver.queryCount() <= 3, "change burst was not rate-limited");

        ContentResolver.putRow("prefs_key_home_recent_hide_status_bar_in_task_view", 1);
        ContentResolver.signalChange(PREF_URI);
        final long firstLeaseDeadline = awaitRecentsLeaseAfter(0);
        awaitRecentsLeaseAfter(firstLeaseDeadline);

        check(HomeLayoutNativeEndpointOS4.pauseForHotReload(), "worker failed to retire");
        check(ContentResolver.unregistrationCount() == 1, "observer leaked after retirement");
        final int retired = ContentResolver.queryCount();
        ContentResolver.signalChange(PREF_URI);
        Thread.sleep(100);
        check(ContentResolver.queryCount() == retired, "retired endpoint still queried provider");
        System.out.println("home layout refresh cadence: PASS (idle; change wake; burst cap; status bar lease; observer cleanup)");
    }
}
