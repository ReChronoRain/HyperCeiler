/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.content.ContentResolver;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** The production endpoint, with deterministic provider faults and publication interleaving. */
public final class HomeLayoutIdleSnapshotTest {
    private static Field cache;
    private static String[][] keys;
    private static int passed, failed;
    private static final String ENABLE = "home_layout_indicator_margin_bottom_enable";
    private static final String MARGIN = "home_layout_indicator_margin_bottom";

    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }
    private static void test(String name, Runnable body) {
        try { body.run(); passed++; System.out.println("PASS " + name); }
        catch (AssertionError error) { failed++; System.out.println("FAIL " + name + ": " + error.getMessage()); }
    }
    private static Map<String, Object> rows(int margin) {
        Map<String, Object> rows = new HashMap<>();
        for (String[] spec : keys) rows.put("prefs_key_" + spec[1], "stringset".equals(spec[0]) ? "0:" : 0);
        rows.put("prefs_key_home_layout_unlock_grids_cell_x", 4);
        rows.put("prefs_key_home_layout_unlock_grids_cell_y", 6);
        rows.put("prefs_key_" + ENABLE, 1);
        rows.put("prefs_key_" + MARGIN, margin);
        return rows;
    }
    private static void publish(Map<String, ?> values) {
        try { cache.set(null, values); }
        catch (IllegalAccessException error) { throw new AssertionError(error); }
    }
    private static boolean same(HomeLayoutNativeEndpointOS4.Snapshot a, HomeLayoutNativeEndpointOS4.Snapshot b) {
        return a.gridEnabled() == b.gridEnabled() && a.cellX() == b.cellX() && a.cellY() == b.cellY()
            && Arrays.equals(a.knobEnabled(), b.knobEnabled()) && Arrays.equals(a.knobDeltaDp(), b.knobDeltaDp())
            && Arrays.equals(a.tweaks(), b.tweaks());
    }
    private static HomeLayoutNativeEndpointOS4.Snapshot establish() {
        PrefsBridge.setTestValues(Map.of());
        ContentResolver.setRows(rows(184));
        check(HomeLayoutNativeEndpointOS4.refreshFromProvider(), "initial provider read");
        var initial = HomeLayoutNativeEndpointOS4.readPreferences();
        check(initial.knobEnabled()[5] == 1 && initial.knobDeltaDp()[5] == 114, "initial capsule margin");
        return initial;
    }
    private static void faultsAtEveryKey(String mode) {
        int lost = 0;
        for (int index = 0; index < keys.length; index++) {
            var initial = establish();
            ContentResolver.setRows(rows(150));
            ContentResolver.failAt(index, mode);
            boolean success = HomeLayoutNativeEndpointOS4.refreshFromProvider();
            if (success || !same(initial, HomeLayoutNativeEndpointOS4.readPreferences())) lost++;
            check(ContentResolver.queryCount() == 1, "must use one atomic provider query");
        }
        check(lost == 0, "incomplete refresh lost last-good snapshot at " + lost + "/" + keys.length + " failure positions");
    }
    public static void main(String[] args) throws Exception {
        Class<?> type = HomeLayoutNativeEndpointOS4.class;
        cache = type.getDeclaredField("cachedValues"); cache.setAccessible(true);
        Field prefKeys = type.getDeclaredField("PREF_KEYS"); prefKeys.setAccessible(true); keys = (String[][]) prefKeys.get(null);
        ContentResolver.setSpecs(keys);
        // Host tests drive refresh explicitly; no real sleeping background worker races the fixture.
        Field refresher = type.getDeclaredField("refresher"); refresher.setAccessible(true); refresher.set(null, new Thread());
        for (String mode : new String[]{"query", "null", "move", "read", "close"}) {
            test(mode + " faults at all " + keys.length + " keys preserve last-good snapshot", () -> faultsAtEveryKey(mode));
        }
        test("3600 idle failure cycles never sink capsule", () -> {
            var initial = establish(); int lost = 0;
            for (int cycle = 0; cycle < 3600; cycle++) {
                ContentResolver.setRows(rows(150)); ContentResolver.failAt(14, "query");
                check(!HomeLayoutNativeEndpointOS4.refreshFromProvider(), "refused provider cycle");
                if (!same(initial, HomeLayoutNativeEndpointOS4.readPreferences())) lost++;
            }
            check(lost == 0, "capsule lost 114dp delta in " + lost + " idle cycles");
        });
        test("recovery accepts new margin then explicit disable", () -> {
            establish(); ContentResolver.setRows(rows(150));
            check(HomeLayoutNativeEndpointOS4.refreshFromProvider(), "provider recovered");
            check(HomeLayoutNativeEndpointOS4.readPreferences().knobDeltaDp()[5] == 80, "recovered slider value");
            var off = rows(150); off.put("prefs_key_" + ENABLE, 0); ContentResolver.setRows(off);
            check(HomeLayoutNativeEndpointOS4.refreshFromProvider(), "explicit off refreshed");
            var disabled = HomeLayoutNativeEndpointOS4.readPreferences();
            check(disabled.knobEnabled()[5] == 0 && disabled.knobDeltaDp()[5] == 0, "intentional disable honored");
        });
        test("known deletion uses defaults, not stale LSPosed enable", () -> {
            establish(); PrefsBridge.setTestValues(Map.of(ENABLE, 1, MARGIN, 184));
            ContentResolver.setRows(Map.of("prefs_key_home_layout_unlock_grids_cell_x", 4));
            check(HomeLayoutNativeEndpointOS4.refreshFromProvider(), "complete sparse snapshot");
            check(HomeLayoutNativeEndpointOS4.readPreferences().knobEnabled()[5] == 0, "deleted key revived from remote cache");
        });
        test("complete empty settings reset clears capsule override", () -> {
            establish(); ContentResolver.setRows(Map.of());
            check(HomeLayoutNativeEndpointOS4.refreshFromProvider(), "complete empty snapshot is valid reset");
            check(HomeLayoutNativeEndpointOS4.readPreferences().knobEnabled()[5] == 0, "reset retained old capsule position");
        });
        test("one native packet uses one provider snapshot", () -> {
            establish(); Map<String, Integer> newer = Map.of(ENABLE, 1, MARGIN, 150);
            Map<String, Integer> switching = new HashMap<>() {
                @Override public Integer get(Object key) {
                    Integer value = super.get(key);
                    if (ENABLE.equals(key)) publish(newer);
                    return value;
                }
            };
            switching.put(ENABLE, 1); switching.put(MARGIN, 184); publish(switching);
            check(HomeLayoutNativeEndpointOS4.readPreferences().knobDeltaDp()[5] == 114, "mixed old enable/new margin within one packet");
            check(HomeLayoutNativeEndpointOS4.readPreferences().knobDeltaDp()[5] == 80, "next packet sees new snapshot");
        });
        System.out.println("IDLE_SNAPSHOT_TEST: " + passed + " passed; " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
