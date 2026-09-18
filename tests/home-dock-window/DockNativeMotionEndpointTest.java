package com.sevtinge.hyperceiler.tests.dock;

/* SPDX-License-Identifier: AGPL-3.0-or-later */
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockNativeMotion;
import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockNativeMotionEndpoint;
import java.util.concurrent.atomic.AtomicInteger;

public class DockNativeMotionEndpointTest {
    private static final int UID = 10139;
    private static final int PID = 6170;

    private static Parcel packet(long sequence, long time, int scene, double scale,
                                 long entryHits, long publishHits) {
        long packed = (Double.doubleToRawLongBits(scale) & ~3L) | scene;
        return new Parcel("android.view.IWindowManager", sequence, time, packed,
                entryHits, publishHits);
    }

    private static void check(boolean value) {
        if (!value) throw new AssertionError();
    }

    public static void main(String[] args) {
        AtomicInteger changed = new AtomicInteger();
        AtomicInteger keepalives = new AtomicInteger();
        DockNativeMotionEndpoint endpoint = new DockNativeMotionEndpoint(
                changed::incrementAndGet, keepalives::incrementAndGet, ignored -> { });
        endpoint.bindIdentity(UID, PID);
        Binder.setCallingIdentityForTest(UID, PID);

        long firstTime = System.nanoTime();
        check(endpoint.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(1, firstTime, 0, 1, 4, 0), 0) == DockNativeMotionEndpoint.ACK);
        check(endpoint.latest(UID, PID) == null);
        check(changed.get() == 0); // Entry-only keepalive is not a motion sample.
        check(keepalives.get() == 1); // It may still exercise the frame-channel watchdog.

        long overviewTime = System.nanoTime();
        check(endpoint.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(2, overviewTime, 1, .98, 5, 1), 0) == DockNativeMotionEndpoint.ACK);
        DockNativeMotion.Sample overview = endpoint.latest(UID, PID);
        check(overview != null && overview.sequence() == 2 && changed.get() == 1);

        // A newer keepalive may advance replay protection, but must retain the original sample
        // timestamp/sequence so it expires instead of keeping stale motion alive.
        long keepaliveTime = System.nanoTime();
        check(endpoint.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(3, keepaliveTime, 1, .98, 50, 1), 0) == DockNativeMotionEndpoint.ACK);
        DockNativeMotion.Sample afterKeepalive = endpoint.latest(UID, PID);
        check(afterKeepalive != null && afterKeepalive.sequence() == 2
                && afterKeepalive.uptimeNanos() == overviewTime && changed.get() == 1);
        check(keepalives.get() == 2);

        // The exact UID and PID are both required; sharing only the launcher UID is insufficient.
        Binder.setCallingIdentityForTest(UID, PID + 1);
        check(endpoint.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(4, System.nanoTime(), 2, 1, 51, 2), 0) == DockNativeMotionEndpoint.ACK);
        check(endpoint.latest(UID, PID).sequence() == 2);
        check(endpoint.latest(UID, PID + 1) == null);

        Binder.setCallingIdentityForTest(UID, PID);
        long beforeFastTransition = System.nanoTime();
        check(endpoint.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(4, System.nanoTime(), 1, .97, 52, 2), 0) == DockNativeMotionEndpoint.ACK);
        check(endpoint.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(5, System.nanoTime(), 2, 1, 53, 3), 0) == DockNativeMotionEndpoint.ACK);
        check(endpoint.latest(UID, PID).scene() == 2);
        check(endpoint.sawOverviewSince(UID, PID, beforeFastTransition));

        endpoint.requestHookRevalidation(UID, PID);
        check(endpoint.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(6, System.nanoTime(), 2, 1, 54, 3), 0)
                == DockNativeMotionEndpoint.ACK_REVALIDATE);
        check(endpoint.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(7, System.nanoTime(), 2, 1, 55, 3), 0)
                == DockNativeMotionEndpoint.ACK_REVALIDATE);
        // The request survives repeated keepalives and clears only when a hook
        // publishes new progress beyond the baseline captured by the request.
        endpoint.requestHookRevalidation(UID, PID);
        check(endpoint.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(8, System.nanoTime(), 2, 1, 56, 4), 0)
                == DockNativeMotionEndpoint.ACK);

        check(endpoint.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(9, System.nanoTime(), 2, 1, 57, 4), IBinder.FLAG_ONEWAY)
                == DockNativeMotionEndpoint.ACK);

        // Scene 1 received before WindowState identity binding remains observable even if scene 2
        // replaces the pending latest sample before the bind completes.
        AtomicInteger earlyChanged = new AtomicInteger();
        DockNativeMotionEndpoint early = new DockNativeMotionEndpoint(
                earlyChanged::incrementAndGet, ignored -> { });
        long earlySceneOne = System.nanoTime();
        check(early.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(1, earlySceneOne, 1, .98, 1, 1), 0) == DockNativeMotionEndpoint.ACK);
        check(early.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(2, System.nanoTime(), 2, 1, 2, 2), 0) == DockNativeMotionEndpoint.ACK);
        early.bindIdentity(UID, PID);
        check(early.latest(UID, PID).scene() == 2);
        check(early.sawOverviewSince(UID, PID, earlySceneOne));
        check(earlyChanged.get() == 1);

        DockNativeMotionEndpoint earlyKeepalive = new DockNativeMotionEndpoint(
                () -> { throw new AssertionError("keepalive notified motion"); }, ignored -> { });
        check(earlyKeepalive.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(1, System.nanoTime(), 0, 1, 7, 0), 0) == DockNativeMotionEndpoint.ACK);
        check(earlyKeepalive.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(2, System.nanoTime(), 0, 1, 8, 0), 0) == DockNativeMotionEndpoint.ACK);
        earlyKeepalive.bindIdentity(UID, PID);
        check(earlyKeepalive.latest(UID, PID) == null);

        // A new launcher PID using the same authenticated UID may publish before
        // its replacement WindowState binds. Keep it isolated, then promote it
        // only after the exact new PID is independently confirmed.
        DockNativeMotionEndpoint restarted = new DockNativeMotionEndpoint(
                () -> { }, ignored -> { });
        restarted.bindIdentity(UID, PID);
        Binder.setCallingIdentityForTest(UID, PID + 20);
        check(restarted.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(1, System.nanoTime(), 1, .98, 1, 1), 0)
                == DockNativeMotionEndpoint.ACK);
        check(restarted.latest(UID, PID + 20) == null);
        restarted.bindIdentity(UID, PID + 20);
        check(restarted.latest(UID, PID + 20) != null);

        DockNativeMotionEndpoint wrongUid = new DockNativeMotionEndpoint(
                () -> { }, ignored -> { });
        wrongUid.bindIdentity(UID, PID);
        Binder.setCallingIdentityForTest(UID + 1, PID + 30);
        check(wrongUid.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(1, System.nanoTime(), 1, .98, 1, 1), 0)
                == DockNativeMotionEndpoint.ACK);
        wrongUid.bindIdentity(UID + 1, PID + 30);
        check(wrongUid.latest(UID + 1, PID + 30) == null);

        // AUTO_AIM owns an independent transaction/latest lane. A scene-2 write in the same
        // Flutter frame must not erase the live projected scale before WMS consumes it.
        AtomicInteger aimChanged = new AtomicInteger();
        AtomicInteger aimKeepalive = new AtomicInteger();
        DockNativeMotionEndpoint aimEndpoint = new DockNativeMotionEndpoint(
                aimChanged::incrementAndGet, aimKeepalive::incrementAndGet, ignored -> { });
        aimEndpoint.bindIdentity(UID, PID);
        Binder.setCallingIdentityForTest(UID, PID);
        long aimTime = System.nanoTime();
        check(aimEndpoint.receive(DockNativeMotionEndpoint.AUTO_AIM_TRANSACTION_CODE,
                packet(1, aimTime, DockNativeMotion.SCENE_AUTO_AIM, .413, 1, 1), 0)
                == DockNativeMotionEndpoint.ACK);
        DockNativeMotion.Sample aimSample = aimEndpoint.latestAutoAim(UID, PID);
        check(aimSample != null && aimSample.scene() == DockNativeMotion.SCENE_AUTO_AIM
                && Double.doubleToRawLongBits(aimSample.scale())
                    == (Double.doubleToRawLongBits(.413) & ~3L));
        check(aimChanged.get() == 1);
        check(aimEndpoint.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(1, System.nanoTime(), 2, 1, 100, 50), 0)
                == DockNativeMotionEndpoint.ACK);
        check(aimEndpoint.latest(UID, PID).scene() == 2
                && aimEndpoint.latestAutoAim(UID, PID) == aimSample);
        check(aimEndpoint.receive(DockNativeMotionEndpoint.AUTO_AIM_TRANSACTION_CODE,
                packet(2, System.nanoTime(), DockNativeMotion.SCENE_AUTO_AIM, .413, 2, 1), 0)
                == DockNativeMotionEndpoint.ACK);
        check(aimEndpoint.latestAutoAim(UID, PID) == aimSample && aimChanged.get() == 2
                && aimKeepalive.get() == 1);

        // Accept scene 3 on the legacy code during a live module/native hot-reload, but still
        // route it into the independent lane instead of replacing recents state.
        check(aimEndpoint.receive(DockNativeMotionEndpoint.TRANSACTION_CODE,
                packet(3, System.nanoTime(), DockNativeMotion.SCENE_AUTO_AIM, .72, 3, 2), 0)
                == DockNativeMotionEndpoint.ACK);
        check(aimEndpoint.latest(UID, PID).scene() == 2
                && Math.abs(aimEndpoint.latestAutoAim(UID, PID).scale() - .72) < 1e-12);

        AtomicInteger earlyAimChanged = new AtomicInteger();
        DockNativeMotionEndpoint earlyAim = new DockNativeMotionEndpoint(
                earlyAimChanged::incrementAndGet, ignored -> { });
        check(earlyAim.receive(DockNativeMotionEndpoint.AUTO_AIM_TRANSACTION_CODE,
                packet(1, System.nanoTime(), DockNativeMotion.SCENE_AUTO_AIM, .38, 7, 4), 0)
                == DockNativeMotionEndpoint.ACK);
        check(earlyAim.latestAutoAim(UID, PID) == null);
        earlyAim.bindIdentity(UID, PID);
        check(Math.abs(earlyAim.latestAutoAim(UID, PID).scale() - .38) < 1e-12
                && earlyAim.latest(UID, PID) == null && earlyAimChanged.get() == 1);

        // The dedicated transaction is fail-closed for every non-scene-3 payload.
        check(earlyAim.receive(DockNativeMotionEndpoint.AUTO_AIM_TRANSACTION_CODE,
                packet(2, System.nanoTime(), 2, .91, 8, 5), 0)
                == DockNativeMotionEndpoint.ACK);
        check(Math.abs(earlyAim.latestAutoAim(UID, PID).scale() - .38) < 1e-12);

        System.out.println("DockNativeMotionEndpoint tests passed");
    }
}
