/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.content.ContentResolver;

/** The launcher endpoint remains prompt on writes without polling the app every 1.5 seconds. */
public final class HomeLayoutRefreshCadenceTest {
    private static void check(boolean value, String reason) {
        if (!value) throw new AssertionError(reason);
    }

    private static void awaitQueries(int count) throws InterruptedException {
        final long deadline = System.nanoTime() + 2_000_000_000L;
        while (ContentResolver.queryCount() < count && System.nanoTime() < deadline)
            Thread.sleep(10);
        check(ContentResolver.queryCount() >= count, "notification did not refresh snapshot");
    }

    public static void main(String[] args) throws Exception {
        var specs = HomeLayoutNativeEndpointOS4.class.getDeclaredField("PREF_KEYS");
        specs.setAccessible(true);
        var interval = HomeLayoutNativeEndpointOS4.class.getDeclaredField("PREFS_REFRESH_MS");
        interval.setAccessible(true);
        check(interval.getLong(null) == 60_000L, "idle fallback is not one minute");
        ContentResolver.setSpecs((String[][]) specs.get(null));
        ContentResolver.setRows(java.util.Map.of(
            "prefs_key_home_layout_workspace_padding_top_enable", 1,
            "prefs_key_home_layout_workspace_padding_top", 60));
        HomeLayoutNativeEndpointOS4.readPreferences();
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
        final long published = System.nanoTime() + 2_000_000_000L;
        while (HomeLayoutNativeEndpointOS4.readPreferences().knobDeltaDp()[2] != 60
            && System.nanoTime() < published) Thread.sleep(10);
        check(HomeLayoutNativeEndpointOS4.readPreferences().knobDeltaDp()[2] == 60,
            "new physical revision not published after notification");
        Thread.sleep(250);
        check(ContentResolver.queryCount() <= 3, "change burst was not rate-limited");
        check(HomeLayoutNativeEndpointOS4.pauseForHotReload(), "worker failed to retire");
        check(ContentResolver.unregistrationCount() == 1, "observer leaked after retirement");
        ContentResolver.signalChange("content://com.sevtinge.hyperceiler.provider.sharedprefs/"
            + "pref/integer/prefs_key_home_layout_workspace_padding_top");
        Thread.sleep(100);
        check(ContentResolver.queryCount() <= 3, "retired endpoint still queried provider");
        System.out.println("home layout refresh cadence: PASS (idle 60s; change wake; burst cap; observer cleanup)");
    }
}
