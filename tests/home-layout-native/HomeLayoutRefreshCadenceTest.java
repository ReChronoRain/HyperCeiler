/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.SystemClock;
import com.sevtinge.hyperceiler.libhook.provider.HomeLayoutPrefsSnapshot;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/** Executes the production worker against the host's observer and Provider transport fixtures. */
public final class HomeLayoutRefreshCadenceTest {
    private static final String PREF = "content://com.sevtinge.hyperceiler.provider.sharedprefs/pref/";
    private static final String MARGIN = "prefs_key_home_layout_workspace_padding_top";
    private static final String PACKAGE = "package:com.sevtinge.hyperceiler";
    private static final int HAPTIC = (57 << 4) | 9;

    private static final class Listener implements IBinder {
        final AtomicInteger changes = new AtomicInteger();
        final AtomicInteger retirements = new AtomicInteger();
        DeathRecipient death;

        @Override public String getInterfaceDescriptor() { return "com.sevtinge.hyperceiler.home.ConfigObserver"; }
        @Override public void linkToDeath(DeathRecipient recipient, int flags) { death = recipient; }
        @Override public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
            check(death == recipient, "unexpected death recipient");
            death = null;
            return true;
        }
        @Override public boolean transact(int code, Parcel data, Parcel reply, int flags) {
            data.enforceInterface(getInterfaceDescriptor());
            check(data.dataAvail() == 0 && reply == null && flags == FLAG_ONEWAY, "observer wire shape");
            if (code == FIRST_CALL_TRANSACTION) changes.incrementAndGet();
            else if (code == FIRST_CALL_TRANSACTION + 1) retirements.incrementAndGet();
            else throw new AssertionError("unexpected observer callback");
            return true;
        }
    }

    private static void subscribe(Listener listener) throws Exception {
        final var endpoint = new HomeLayoutNativeEndpointOS4((uid, pid) -> uid == 10234 && pid == 345,
            HomeLayoutNativeEndpointOS4::readPreferences);
        final Parcel request = new Parcel("android.view.IWindowManager");
        request.writeStrongBinder(listener);
        request.setDataPosition(0);
        final Parcel reply = Parcel.obtain();
        Binder.setCallingIdentityForTest(1000, 1);
        try {
            endpoint.observe(request, reply, 0);
            throw new AssertionError("non-launcher observer registered");
        } catch (SecurityException expected) { }
        Binder.setCallingIdentityForTest(10234, 345);
        endpoint.observe(request, reply, 0);
        reply.setDataPosition(0);
        check(reply.readInt() == 0 && reply.readInt() == 0x48435831 && reply.readInt() == 1
            && reply.dataAvail() == 0, "registration acknowledgement");
        check(listener.changes.get() == 1, "registration did not signal initial sync");
    }

    private static void check(boolean value, String reason) {
        if (!value) throw new AssertionError(reason);
    }

    private static void await(BooleanSupplier ready, long ms, String reason) throws InterruptedException {
        final long deadline = System.nanoTime() + ms * 1_000_000L;
        while (!ready.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(10);
        check(ready.getAsBoolean(), reason);
    }

    public static void main(String[] args) throws Exception {
        ContentResolver.setSpecs(HomeLayoutPrefsSnapshot.specs());
        ContentResolver.setRows(Map.of(
            "prefs_key_allow_hook", 1,
            MARGIN + "_enable", 1,
            MARGIN, 60,
            HomeLayoutPrefsSnapshot.HAPTIC_PROFILE_V1, HAPTIC));
        HomeLayoutNativeEndpointOS4.startCombinedPrefetch();
        await(() -> HomeLayoutNativeEndpointOS4.recentsHapticProfileWord() == HAPTIC, 2000, "initial revision");
        check(ContentResolver.queryCount() == 1, "startup must read one complete revision");
        check(ContentResolver.registrationCount() == 1, "observer not registered");
        var initial = HomeLayoutNativeEndpointOS4.readCombinedSnapshot();
        check(initial.available() == 1 && initial.flags() != 0, "initial authorization missing");
        final var superseded = new Listener();
        subscribe(superseded);
        final var dead = new Listener();
        subscribe(dead);
        check(superseded.death == null, "replacement observer left old registration");
        dead.death.binderDied();
        final var listener = new Listener();
        subscribe(listener);

        Thread.sleep(1800); // More than the old idle polling interval.
        SystemClock.source = () -> System.nanoTime() + 6_000_000_000L;
        var renewed = HomeLayoutNativeEndpointOS4.readCombinedSnapshot();
        check(renewed.revision() == initial.revision() && renewed.deadline() > initial.deadline()
            && renewed.available() == 1 && renewed.flags() == initial.flags(), "idle authorization expired");
        check(HomeLayoutNativeEndpointOS4.recentsHapticProfileWord() == HAPTIC
            && ContentResolver.queryCount() == 1, "idle lease or vibration woke the Provider");
        check(listener.changes.get() == 1, "idle publication woke the launcher observer");
        SystemClock.source = System::nanoTime;

        ContentResolver.signalChange(PREF + "integer/prefs_key_unrelated_setting");
        Thread.sleep(200);
        check(ContentResolver.queryCount() == 1, "unrelated setting woke the layout reader");
        check(listener.changes.get() == 1, "unrelated setting woke the launcher observer");

        ContentResolver.putRow(MARGIN, 90);
        ContentResolver.putRow(HomeLayoutPrefsSnapshot.HAPTIC_PROFILE_V1, HAPTIC + 16);
        for (int index = 0; index < 20; ++index)
            ContentResolver.signalChange(PREF + "integer/prefs_key_home_recent_card_haptic_strength");
        await(() -> HomeLayoutNativeEndpointOS4.recentsHapticProfileWord() == HAPTIC + 16, 2000,
            "vibration settings did not publish");
        check(HomeLayoutNativeEndpointOS4.readPreferences().knobDeltaDp()[2] == 60,
            "layout and vibration did not publish together");
        check(ContentResolver.queryCount() <= 3, "change burst was not coalesced");
        await(() -> listener.changes.get() > 1, 2000, "settings did not notify the launcher");
        check(superseded.changes.get() == 1, "obsolete launcher observer was notified");
        check(dead.changes.get() == 1, "dead launcher observer was notified");

        ContentResolver.failAt(0, "null");
        Context.signalPackage(new Intent(Intent.ACTION_PACKAGE_REPLACED, Uri.parse(PACKAGE)));
        check(HomeLayoutNativeEndpointOS4.recentsHapticProfileWord() == -1
            && HomeLayoutNativeEndpointOS4.readCombinedSnapshot().available() == 0,
            "package replacement left the authorization live");
        check(HomeLayoutNativeEndpointOS4.readPreferences().knobDeltaDp()[2] == 60,
            "package replacement discarded the last good layout");
        await(() -> ContentResolver.queryCount() >= 4, 13_000, "failed read recovery did not run");
        Thread.sleep(1800);
        check(ContentResolver.queryCount() == 4, "failed Provider was repeatedly started");

        ContentResolver.setRows(Map.of());
        Context.signalPackage(new Intent(Intent.ACTION_PACKAGE_DATA_CLEARED, Uri.parse(PACKAGE)));
        check(HomeLayoutNativeEndpointOS4.readPreferences().knobDeltaDp()[2] == 0,
            "data clear kept stale layout");
        await(() -> ContentResolver.queryCount() == 1, 2000, "package data clear did not refresh");
        check(HomeLayoutNativeEndpointOS4.pauseForHotReload(), "worker failed to retire");
        check(listener.retirements.get() == 1 && listener.death == null, "launcher observer not retired");
        check(ContentResolver.unregistrationCount() == 1, "observer leaked after retirement");
        ContentResolver.signalChange(PREF + "integer/" + MARGIN);
        Thread.sleep(200);
        check(ContentResolver.queryCount() == 1, "retired endpoint still queried Provider");
        System.out.println("PASS idle configuration, short leases, haptic changes, bounded retries, data clear and retirement");
    }
}
