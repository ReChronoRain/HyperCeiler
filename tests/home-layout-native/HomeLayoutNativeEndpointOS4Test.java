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

    /** Mirrors the settings page: default 30, so 60 becomes +30 dp of native delta. */
    private static void checkProviderMapping() {
        try {
            var specs = HomeLayoutNativeEndpointOS4.class.getDeclaredField("PREF_KEYS");
            specs.setAccessible(true); ContentResolver.setSpecs((String[][]) specs.get(null));
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        final java.util.Map<String, Integer> rows = new java.util.HashMap<>();
        rows.put("prefs_key_home_layout_workspace_padding_top_enable", 1);
        rows.put("prefs_key_home_layout_workspace_padding_top", 60);
        rows.put("prefs_key_home_layout_indicator_margin_bottom_enable", 1);
        rows.put("prefs_key_home_layout_indicator_margin_bottom", 700);
        rows.put("prefs_key_home_layout_hotseats_margin_bottom_enable", 1);
        rows.put("prefs_key_home_layout_hotseats_margin_bottom", 0);
        rows.put("prefs_key_home_folder_vertical_spacing_enable", 1);
        rows.put("prefs_key_home_folder_vertical_spacing", 12);
        rows.put("prefs_key_home_folder_columns", 5);
        rows.put("prefs_key_home_layout_searchbar_width_enable", 1);
        rows.put("prefs_key_home_layout_searchbar_margin_bottom_enable", 1);
        rows.put("prefs_key_home_layout_searchbar_margin_bottom", 80);
        // 430 is outside the page's 0..400 range, so it must fall back to the neutral default.
        rows.put("prefs_key_home_layout_searchbar_width", 430);
        rows.put("prefs_key_home_animation_open_rate_enable", 1);
        rows.put("prefs_key_home_animation_open_rate", 65);
        rows.put("prefs_key_home_animation_recents_enable", 1);
        rows.put("prefs_key_home_animation_recents_rate", 40);
        ContentResolver.setRows(rows);

        HomeLayoutNativeEndpointOS4.refreshFromProvider();
        final HomeLayoutNativeEndpointOS4.Snapshot snapshot =
            HomeLayoutNativeEndpointOS4.readPreferences();

        check(snapshot.acknowledgment() == HomeLayoutNativeEndpointOS4.ACK);
        check(snapshot.knobEnabled().length == 8);
        // index 2 = workspace top: 60 - 30 = 30 dp, forwarded unscaled
        check(snapshot.knobEnabled()[2] == 1);
        check(snapshot.knobDeltaDp()[2] == 30);
        // The enlarged slider endpoint maps 700 to +630 dp before the capsule hook inverts it.
        check(snapshot.knobEnabled()[5] == 1);
        check(snapshot.knobDeltaDp()[5] == 630);
        // index 0 = hotseat margin: 0 - 70 = -70 dp
        check(snapshot.knobEnabled()[0] == 1);
        check(snapshot.knobDeltaDp()[0] == -70);
        // index 1 = folder row spacing: 12 dp is an additive value, forwarded as-is
        check(snapshot.knobEnabled()[1] == 1);
        check(snapshot.knobDeltaDp()[1] == 12);
        // OS4 search-bar controls are retired; even persisted values stay inert.
        check(snapshot.knobEnabled()[6] == 0);
        check(snapshot.knobEnabled()[7] == 0);
        check(snapshot.knobDeltaDp()[6] == 0);
        check(snapshot.knobDeltaDp()[7] == 0);
        for (int mode = 0; mode <= 3; ++mode) {
            rows.put("prefs_key_home_other_seek_points", mode);
            ContentResolver.setRows(rows);
            check(HomeLayoutNativeEndpointOS4.refreshFromProvider());
            final var policy = HomeLayoutNativeEndpointOS4.readPreferences();
            final int acceptedMode = mode == 3 ? 0 : mode;
            check(policy.knobDeltaDp()[6] == acceptedMode);
            check(policy.knobDeltaDp()[7] == acceptedMode);
            check(policy.knobEnabled()[6] == (acceptedMode == 0 ? 0 : 1));
        }
        rows.remove("prefs_key_home_other_seek_points");
        // Plus the tweaks run the provider also carries.
        check(snapshot.tweaks().length == HomeLayoutNativeEndpointOS4.TWEAK_COUNT);
        check(snapshot.tweaks()[0] == 5);
        check(snapshot.tweaks()[1] == 1); // a non-default count enables the native patch
        check(snapshot.tweaks()[8] == 0x66); // level 70, the page's system-default icon scale
        check(snapshot.tweaks()[12] == 1);
        check(snapshot.tweaks()[13] == 65);
        check(snapshot.tweaks()[14] == 1);
        check(snapshot.tweaks()[15] == 40);
        check(snapshot.tweaks()[16] == 0);
        rows.put("prefs_key_home_dock_unlock_hotseat", 1);
        ContentResolver.setRows(rows);
        check(HomeLayoutNativeEndpointOS4.refreshFromProvider());
        check(HomeLayoutNativeEndpointOS4.readPreferences().tweaks()[16] == 1);
        rows.put("prefs_key_home_dock_unlock_hotseat", 0);
        ContentResolver.setRows(rows);
        check(HomeLayoutNativeEndpointOS4.refreshFromProvider());
        check(HomeLayoutNativeEndpointOS4.readPreferences().tweaks()[16] == 0);

        // The new negative end moves the capsule in the opposite direction from 700.
        rows.put("prefs_key_home_layout_indicator_margin_bottom", -300);
        ContentResolver.setRows(rows);
        HomeLayoutNativeEndpointOS4.refreshFromProvider();
        final HomeLayoutNativeEndpointOS4.Snapshot lowEnd =
            HomeLayoutNativeEndpointOS4.readPreferences();
        check(lowEnd.knobEnabled()[5] == 1);
        check(lowEnd.knobDeltaDp()[5] == -370);

        // An unset value with the switch enabled resolves to 70, the neutral default.
        rows.remove("prefs_key_home_layout_indicator_margin_bottom");
        ContentResolver.setRows(rows);
        HomeLayoutNativeEndpointOS4.refreshFromProvider();
        final HomeLayoutNativeEndpointOS4.Snapshot defaultIndicator =
            HomeLayoutNativeEndpointOS4.readPreferences();
        check(defaultIndicator.knobEnabled()[5] == 1);
        check(defaultIndicator.knobDeltaDp()[5] == 0);

        final java.util.Map<String, Integer> defaults = new java.util.HashMap<>();
        defaults.put("prefs_key_home_folder_columns", 3);
        ContentResolver.setRows(defaults);
        HomeLayoutNativeEndpointOS4.refreshFromProvider();
        final HomeLayoutNativeEndpointOS4.Snapshot neutral =
            HomeLayoutNativeEndpointOS4.readPreferences();
        check(neutral.tweaks()[0] == 3);
        check(neutral.tweaks()[1] == 0); // default keeps the launcher untouched

        // Out-of-range rates clamp into the page's 30..200 window: the page itself refuses lower
        // values, and a stale schema must not silently revert a "fast" choice to identity.
        final java.util.Map<String, Integer> wild = new java.util.HashMap<>();
        wild.put("prefs_key_home_folder_columns", 0);
        wild.put("prefs_key_home_animation_open_rate", 5);
        wild.put("prefs_key_home_animation_recents_rate", 500);
        ContentResolver.setRows(wild);
        HomeLayoutNativeEndpointOS4.refreshFromProvider();
        final HomeLayoutNativeEndpointOS4.Snapshot tame =
            HomeLayoutNativeEndpointOS4.readPreferences();
        check(tame.tweaks()[0] == 3);
        check(tame.tweaks()[1] == 0);
        check(tame.tweaks()[13] == 30);
        check(tame.tweaks()[15] == 200);
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

    /** A tweaks run that is inside every range: folder 5/on, pad 8/5, fold 8/5, icon, animation. */
    private static int[] tweaksOk() {
        return new int[]{5, 1, 8, 5, 0, 8, 5, 0, 0x70, 0, 0, 0, 0, 100, 0, 100, 0};
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
        check(good.knobEnabled()[0] == 1 && good.knobDeltaDp()[0] == 30);
        check(good.knobEnabled()[count - 1] == 1 && good.knobDeltaDp()[count - 1] == -20);
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

        // A short or out-of-range tweaks run must be refused as a whole, because a
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

        final int[] wildCapacity = tweaksOk();
        wildCapacity[16] = 2;
        final var badCapacity = new HomeLayoutNativeEndpointOS4((uid, pid) -> true,
            () -> new HomeLayoutNativeEndpointOS4.Snapshot(0, 1, 4, 6, new int[count],
                new int[count], wildCapacity));
        check(badCapacity.receive(new Parcel(DESCRIPTOR), 0).gridEnabled() == 0);

        final int[] wildAnimation = tweaksOk();
        wildAnimation[15] = 201;
        final HomeLayoutNativeEndpointOS4 badAnimation = new HomeLayoutNativeEndpointOS4(
            (uid, pid) -> true,
            () -> new HomeLayoutNativeEndpointOS4.Snapshot(0, 0, 4, 6, new int[count],
                new int[count], wildAnimation));
        check(badAnimation.receive(new Parcel(DESCRIPTOR), 0).gridEnabled() == 0);

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
