/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.content.ContentResolver;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;

/**
 * Host test for the authenticated preference endpoint.
 *
 * Two things are covered:
 *   1. the provider path: the module app's own values are pulled in one pass and turned into the
 *      pixel deltas the native side reads, including the "outside the page's range falls back to the
 *      neutral default" rule;
 *   2. the transaction boundary: caller identity, the interface token, one-way flag and every range
 *      are enforced, and a bad snapshot is refused as a whole instead of being half applied.
 */
public final class HomeLayoutNativeEndpointOS4Test {
    private static final String DESCRIPTOR = "android.view.IWindowManager";

    private static void check(boolean condition) {
        if (!condition) throw new AssertionError();
    }

    /** Mirrors the settings page: default 30, so 60 becomes +30 dp at the stub's 2.75 density. */
    private static void checkProviderMapping() {
        final java.util.Map<String, Integer> rows = new java.util.HashMap<>();
        rows.put("prefs_key_home_layout_workspace_padding_top_enable", 1);
        rows.put("prefs_key_home_layout_workspace_padding_top", 60);
        rows.put("prefs_key_home_layout_hotseats_margin_bottom_enable", 1);
        rows.put("prefs_key_home_layout_hotseats_margin_bottom", 0);
        rows.put("prefs_key_home_layout_searchbar_width_enable", 1);
        // 430 is outside the page's 0..400 range, so it must fall back to the neutral default.
        rows.put("prefs_key_home_layout_searchbar_width", 430);
        ContentResolver.setRows(rows);

        HomeLayoutNativeEndpointOS4.refreshFromProvider();
        final HomeLayoutNativeEndpointOS4.Snapshot snapshot =
            HomeLayoutNativeEndpointOS4.readPreferences();

        check(snapshot.acknowledgment() == HomeLayoutNativeEndpointOS4.ACK);
        check(snapshot.knobEnabled().length == 8);
        // index 2 = workspace top: (60 - 30) * 2.75 = 82.5 -> 83
        check(snapshot.knobEnabled()[2] == 1);
        check(snapshot.knobDeltaPx()[2] == 83);
        // index 0 = hotseat margin: (0 - 70) * 2.75 = -192.5 -> Math.round rounds toward +inf
        check(snapshot.knobEnabled()[0] == 1);
        check(snapshot.knobDeltaPx()[0] == -192);
        // index 7 = search bar width: out of range -> default -> neutral
        check(snapshot.knobEnabled()[7] == 1);
        check(snapshot.knobDeltaPx()[7] == 0);
        // Plus the tweaks run the provider also carries.
        check(snapshot.tweaks().length == HomeLayoutNativeEndpointOS4.TWEAK_COUNT);
        check(snapshot.tweaks()[0] == 3); // folder columns default
        check(snapshot.tweaks()[8] == 0x70); // 100% icon scale
        ContentResolver.setRows(null);
    }

    /** A well-formed snapshot, accepted as long as the caller is the launcher. */
    private static HomeLayoutNativeEndpointOS4.Snapshot accepted(int count) {
        final int[] enabled = new int[count];
        final int[] deltas = new int[count];
        enabled[0] = 1;
        deltas[0] = 30;
        enabled[count - 1] = 1;
        deltas[count - 1] = -20;
        return new HomeLayoutNativeEndpointOS4.Snapshot(0, 1, 5, 7, enabled, deltas, tweaksOk());
    }

    /** A tweaks run that is inside every range: folder 5/on, pad 8/5, fold 8/5, icon, no flags. */
    private static int[] tweaksOk() {
        return new int[]{5, 1, 8, 5, 0, 8, 5, 0, 0x70, 0, 0, 0};
    }

    private static void checkTransactionBoundary() {
        final int count = HomeLayoutNativeEndpointOS4.knobCount();
        check(count == 8);

        Binder.setCallingIdentityForTest(10100, 42);
        final HomeLayoutNativeEndpointOS4 endpoint = new HomeLayoutNativeEndpointOS4(
            (uid, pid) -> uid == 10100 && pid == 42,
            () -> accepted(count));
        final var good = endpoint.receive(new Parcel(DESCRIPTOR), 0);
        check(good.acknowledgment() == HomeLayoutNativeEndpointOS4.ACK);
        check(good.gridEnabled() == 1 && good.cellX() == 5 && good.cellY() == 7);
        check(good.knobEnabled()[0] == 1 && good.knobDeltaPx()[0] == 30);
        check(good.knobEnabled()[count - 1] == 1 && good.knobDeltaPx()[count - 1] == -20);
        check(endpoint.receive(new Parcel(DESCRIPTOR), IBinder.FLAG_ONEWAY).gridEnabled() == 0);
        check(endpoint.receive(new Parcel(DESCRIPTOR, 1L), 0).gridEnabled() == 0);
        Binder.setCallingIdentityForTest(10200, 42);
        check(endpoint.receive(new Parcel(DESCRIPTOR), 0).gridEnabled() == 0);
        Binder.setCallingIdentityForTest(10100, 43);
        check(endpoint.receive(new Parcel(DESCRIPTOR), 0).gridEnabled() == 0);
        Binder.setCallingIdentityForTest(10100, 42);

        final int[] wrongCount = new int[count - 1];
        final HomeLayoutNativeEndpointOS4 badCount = new HomeLayoutNativeEndpointOS4(
            (uid, pid) -> true,
            () -> new HomeLayoutNativeEndpointOS4.Snapshot(0, 1, 5, 7, wrongCount, wrongCount,
                tweaksOk()));
        check(badCount.receive(new Parcel(DESCRIPTOR), 0).gridEnabled() == 0);

        final int[] outOfRange = new int[count];
        outOfRange[2] = 2401;
        final HomeLayoutNativeEndpointOS4 tooLarge = new HomeLayoutNativeEndpointOS4(
            (uid, pid) -> true,
            () -> new HomeLayoutNativeEndpointOS4.Snapshot(0, 0, 4, 6, new int[count], outOfRange,
                tweaksOk()));
        check(tooLarge.receive(new Parcel(DESCRIPTOR), 0).gridEnabled() == 0);

        final int[] badCell = new int[count];
        badCell[1] = 1;
        final HomeLayoutNativeEndpointOS4 cell = new HomeLayoutNativeEndpointOS4(
            (uid, pid) -> true,
            () -> new HomeLayoutNativeEndpointOS4.Snapshot(0, 1, 10, 7, badCell, new int[count],
                tweaksOk()));
        check(cell.receive(new Parcel(DESCRIPTOR), 0).gridEnabled() == 0);

        // Protocol v3: a short or out-of-range tweaks run must be refused as a whole, because a
        // partially applied snapshot would patch the launcher with values nobody chose.
        final HomeLayoutNativeEndpointOS4 badTweaks = new HomeLayoutNativeEndpointOS4(
            (uid, pid) -> true,
            () -> new HomeLayoutNativeEndpointOS4.Snapshot(0, 0, 4, 6, new int[count],
                new int[count], new int[11]));
        check(badTweaks.receive(new Parcel(DESCRIPTOR), 0).gridEnabled() == 0);

        final int[] wildFolder = tweaksOk();
        wildFolder[0] = 17;
        final HomeLayoutNativeEndpointOS4 badFolder = new HomeLayoutNativeEndpointOS4(
            (uid, pid) -> true,
            () -> new HomeLayoutNativeEndpointOS4.Snapshot(0, 0, 4, 6, new int[count],
                new int[count], wildFolder));
        check(badFolder.receive(new Parcel(DESCRIPTOR), 0).gridEnabled() == 0);

        boolean descriptorRejected = false;
        try {
            endpoint.receive(new Parcel("not.window.manager"), 0);
        } catch (SecurityException expected) {
            descriptorRejected = true;
        }
        check(descriptorRejected);
    }

    public static void main(String[] args) {
        checkProviderMapping();
        checkTransactionBoundary();
    }
}
