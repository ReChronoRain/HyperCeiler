/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Authenticated custom transaction carried by MiuiHome's existing IWindowManager Binder. */
public final class DockNativeMotionEndpoint {
    // The established recents transaction remains stable across hot reloads.
    public static final int TRANSACTION_CODE = 0x0048434A;
    /** Independent latest-value lane: recents writes can never erase the Hotseat projection. */
    public static final int AUTO_AIM_TRANSACTION_CODE = 0x0048434B;
    public static final int ACK = 0x48434B32;
    public static final int ACK_REVALIDATE = 0x48434B33;
    private static final String DESCRIPTOR = "android.view.IWindowManager";

    private record Identity(int uid, int pid) { }
    private record State(Identity identity, DockNativeMotion.Sample sample, long sequence) { }
    private record AutoAimState(Identity identity, DockNativeMotion.Sample sample, long sequence) { }
    private record Pending(Identity identity, DockNativeMotion.Sample sample, long sequence) { }
    private record OverviewHit(Identity identity, long timestamp) { }
    private record Revalidation(Identity identity, long publishHits) { }
    private enum AcceptResult { REJECTED, IDENTITY_CHANGED, KEEPALIVE, PROGRESSED }
    private final AtomicReference<State> state = new AtomicReference<>(new State(null, null, 0));
    private final AtomicReference<Pending> pending = new AtomicReference<>();
    private final AtomicReference<AutoAimState> autoAimState =
            new AtomicReference<>(new AutoAimState(null, null, 0));
    private final AtomicReference<Pending> autoAimPending = new AtomicReference<>();
    private final AtomicReference<OverviewHit> overviewHit = new AtomicReference<>();
    private final AtomicReference<Revalidation> revalidation = new AtomicReference<>();
    private final AtomicInteger reported = new AtomicInteger();
    private final Runnable changed;
    private final Runnable keepalive;
    private final Consumer<String> diagnostic;

    public DockNativeMotionEndpoint(Runnable changed, Consumer<String> diagnostic) {
        this(changed, () -> { }, diagnostic);
    }

    public DockNativeMotionEndpoint(Runnable changed, Runnable keepalive,
                                    Consumer<String> diagnostic) {
        this.changed = changed;
        this.keepalive = keepalive;
        this.diagnostic = diagnostic;
    }

    public void bindIdentity(int uid, int pid) {
        if (uid < 10000 || pid <= 0) return;
        Identity replacement = new Identity(uid, pid);
        Pending early = pending.getAndSet(null);
        State rebound = state.updateAndGet(current -> {
            boolean sameIdentity = replacement.equals(current.identity());
            DockNativeMotion.Sample sample = sameIdentity ? current.sample() : null;
            long sequence = sameIdentity ? current.sequence() : 0;
            if (early != null && replacement.equals(early.identity())
                    && early.sequence() > sequence) {
                if (early.sample() != null
                        && DockNativeMotion.hasPublishedProgress(sample, early.sample())) {
                    sample = early.sample();
                }
                sequence = early.sequence();
            }
            if (sameIdentity && sample == current.sample() && sequence == current.sequence()) {
                return current;
            }
            return new State(replacement, sample, sequence);
        });
        OverviewHit hit = overviewHit.get();
        if (hit != null && !replacement.equals(hit.identity())) overviewHit.compareAndSet(hit, null);
        Revalidation requested = revalidation.get();
        if (requested != null && !replacement.equals(requested.identity())) {
            revalidation.compareAndSet(requested, null);
        }
        boolean promoted = early != null && early.sample() != null
                && rebound.sample() == early.sample();
        if (promoted) recordOverview(replacement, early.sample());
        // Covers a packet racing between pending.getAndSet() and the state update.
        if (promotePending(replacement)) promoted = true;

        Pending earlyAim = autoAimPending.getAndSet(null);
        AutoAimState aimRebound = autoAimState.updateAndGet(current -> {
            boolean sameIdentity = replacement.equals(current.identity());
            DockNativeMotion.Sample sample = sameIdentity ? current.sample() : null;
            long sequence = sameIdentity ? current.sequence() : 0;
            if (earlyAim != null && replacement.equals(earlyAim.identity())
                    && earlyAim.sequence() > sequence) {
                if (earlyAim.sample() != null
                        && DockNativeMotion.hasPublishedProgress(sample, earlyAim.sample())) {
                    sample = earlyAim.sample();
                }
                sequence = earlyAim.sequence();
            }
            if (sameIdentity && sample == current.sample() && sequence == current.sequence()) {
                return current;
            }
            return new AutoAimState(replacement, sample, sequence);
        });
        boolean aimPromoted = earlyAim != null && earlyAim.sample() != null
                && aimRebound.sample() == earlyAim.sample();
        if (promoteAutoAimPending(replacement)) aimPromoted = true;
        if (promoted || aimPromoted) notifyChanged();
    }

    public static boolean handles(int code) {
        return code == TRANSACTION_CODE || code == AUTO_AIM_TRANSACTION_CODE;
    }

    /** Must be called only from IWindowManager.Stub.onTransact while Binder identity is intact. */
    public int receive(int code, Parcel data, int flags) {
        if (!handles(code)) return 0;
        data.enforceInterface(DESCRIPTOR);
        State current = state.get();
        Identity expected = current.identity();
        int callerUid = Binder.getCallingUid();
        int callerPid = Binder.getCallingPid();
        int available = data.dataAvail();
        report(1, "native motion Binder transport observed uid=" + callerUid
            + " pid=" + callerPid + " flags=" + flags + " bytes=" + available);
        // Binder does not preserve the caller PID for asynchronous transactions
        // on this OS4 build. The launcher sends from a detached transport worker,
        // so requiring a synchronous call keeps exact PID authentication without
        // ever blocking its render callback.
        if ((flags & IBinder.FLAG_ONEWAY) != 0) {
            report(2, "native motion Binder rejected: one-way call has no trusted PID");
            return ACK;
        }
        Identity caller = new Identity(callerUid, callerPid);
        if (callerUid < 10000 || callerPid <= 0
                || (expected != null && expected.uid() != callerUid)) {
            report(8, "native motion Binder rejected: caller UID/identity mismatch");
            return ACK;
        }
        if (available != Long.BYTES * 5) {
            report(16, "native motion Binder rejected: payload bytes=" + available);
            return ACK;
        }
        boolean identityMatches = expected != null && expected.equals(caller);
        long sequence = data.readLong();
        long timestamp = data.readLong();
        long packed = data.readLong();
        long entryHits = data.readLong();
        long publishHits = data.readLong();
        boolean autoAim = (packed & 3L) == DockNativeMotion.SCENE_AUTO_AIM;
        if (code == AUTO_AIM_TRANSACTION_CODE && !autoAim) {
            report(512, "auto aim Binder rejected: non-scene-3 payload");
            return ACK;
        }
        AutoAimState aimCurrent = autoAimState.get();
        Pending early = autoAim ? autoAimPending.get() : pending.get();
        long previousSequence = autoAim
                ? identityMatches ? aimCurrent.sequence()
                    : early != null && caller.equals(early.identity()) ? early.sequence() : 0
                : identityMatches ? current.sequence()
                    : early != null && caller.equals(early.identity()) ? early.sequence() : 0;
        DockNativeMotion.Sample sample = DockNativeMotion.validate(
            sequence, timestamp, packed, entryHits, publishHits, previousSequence,
            System.nanoTime());
        if (autoAim) return receiveAutoAim(caller, expected, sample);
        if (sample != null && !identityMatches) {
            boolean pendingProgressed = retainPending(caller, sample);
            if (pendingProgressed && sample.scene() == 1) {
                recordOverview(caller, sample);
            }
            report(4, "native motion Binder retained sample until exact launcher PID binds");
            if (promotePending(caller)) notifyChanged();
            return acknowledgment(caller);
        }
        boolean overviewProgressed = sample != null && sample.scene() == 1
                && DockNativeMotion.hasPublishedProgress(current.sample(), sample);
        if (overviewProgressed) {
            // Preserve a short scene-1 transition even if a concurrent scene-2 packet wins the
            // latest-state CAS before WMS consumes either packet.
            recordOverview(caller, sample);
        }
        AcceptResult accepted = sample == null
                ? AcceptResult.REJECTED : acceptBoundSample(caller, sample);
        if (sample != null && accepted == AcceptResult.IDENTITY_CHANGED) {
            // Window replacement can bind a new PID between the initial state
            // snapshot and this CAS. Isolate the packet until WindowState
            // independently authenticates that exact PID; never apply it to the
            // old launcher identity.
            boolean pendingProgressed = retainPending(caller, sample);
            if (pendingProgressed && sample.scene() == 1) recordOverview(caller, sample);
            if (promotePending(caller)) notifyChanged();
            report(4, "native motion Binder retained sample across launcher PID rebind");
            return acknowledgment(caller);
        }
        if (sample != null) {
            report(32, "native motion Binder sample accepted");
            if (accepted == AcceptResult.PROGRESSED) notifyChanged();
            else if (accepted == AcceptResult.KEEPALIVE) notifyKeepalive();
            if (accepted != AcceptResult.REJECTED
                    && completeRevalidation(caller, sample)) {
                report(256, "native motion semantic hook revalidation recovered progress");
            }
        }
        if (sample == null) report(64, "native motion Binder rejected: invalid or stale sample");
        return acknowledgment(caller);
    }

    private int receiveAutoAim(Identity caller, Identity expected,
                               DockNativeMotion.Sample sample) {
        boolean identityMatches = expected != null && expected.equals(caller);
        if (sample != null && !identityMatches) {
            retainAutoAimPending(caller, sample);
            report(1024, "auto aim Binder retained sample until exact launcher PID binds");
            if (promoteAutoAimPending(caller)) notifyChanged();
            return ACK;
        }
        AcceptResult accepted = sample == null
                ? AcceptResult.REJECTED : acceptBoundAutoAim(caller, sample);
        if (sample != null && accepted == AcceptResult.IDENTITY_CHANGED) {
            retainAutoAimPending(caller, sample);
            if (promoteAutoAimPending(caller)) notifyChanged();
            report(1024, "auto aim Binder retained sample across launcher PID rebind");
            return ACK;
        }
        if (accepted == AcceptResult.PROGRESSED) {
            report(2048, "auto aim Binder sample accepted on independent lane");
            notifyChanged();
        } else if (accepted == AcceptResult.KEEPALIVE) {
            notifyKeepalive();
        }
        if (sample == null) report(4096, "auto aim Binder rejected: invalid or stale sample");
        return ACK;
    }

    public void requestHookRevalidation(int uid, int pid) {
        if (uid < 10000 || pid <= 0) return;
        Identity identity = new Identity(uid, pid);
        State current = state.get();
        long baseline = identity.equals(current.identity()) && current.sample() != null
                ? current.sample().publishHits() : 0;
        revalidation.updateAndGet(requested -> requested != null
                && identity.equals(requested.identity()) ? requested
                : new Revalidation(identity, baseline));
    }

    private int acknowledgment(Identity caller) {
        Revalidation requested = revalidation.get();
        if (requested != null && caller.equals(requested.identity())) {
            report(128, "native motion requested semantic hook revalidation");
            return ACK_REVALIDATE;
        }
        return ACK;
    }

    private boolean completeRevalidation(Identity caller, DockNativeMotion.Sample sample) {
        for (;;) {
            Revalidation requested = revalidation.get();
            if (requested == null || !caller.equals(requested.identity())
                    || sample.publishHits() <= requested.publishHits()) return false;
            if (revalidation.compareAndSet(requested, null)) return true;
        }
    }

    private void recordOverview(Identity identity, DockNativeMotion.Sample sample) {
        if (identity != null && sample.scene() == 1) {
            overviewHit.updateAndGet(current -> current != null
                    && identity.equals(current.identity())
                    && current.timestamp() >= sample.uptimeNanos() ? current
                    : new OverviewHit(identity, sample.uptimeNanos()));
        }
    }

    private boolean retainPending(Identity identity, DockNativeMotion.Sample sample) {
        for (;;) {
            Pending current = pending.get();
            if (current != null && identity.equals(current.identity())
                    && current.sequence() >= sample.sequence()) return false;
            DockNativeMotion.Sample previous = current != null
                    && identity.equals(current.identity()) ? current.sample() : null;
            boolean progressed = DockNativeMotion.hasPublishedProgress(previous, sample);
            DockNativeMotion.Sample effective = progressed ? sample : previous;
            if (pending.compareAndSet(current,
                    new Pending(identity, effective, sample.sequence()))) return progressed;
        }
    }

    private boolean retainAutoAimPending(Identity identity, DockNativeMotion.Sample sample) {
        for (;;) {
            Pending current = autoAimPending.get();
            if (current != null && identity.equals(current.identity())
                    && current.sequence() >= sample.sequence()) return false;
            DockNativeMotion.Sample previous = current != null
                    && identity.equals(current.identity()) ? current.sample() : null;
            boolean progressed = DockNativeMotion.hasPublishedProgress(previous, sample);
            DockNativeMotion.Sample effective = progressed ? sample : previous;
            if (autoAimPending.compareAndSet(current,
                    new Pending(identity, effective, sample.sequence()))) return progressed;
        }
    }

    private AcceptResult acceptBoundSample(Identity identity, DockNativeMotion.Sample sample) {
        for (;;) {
            State current = state.get();
            if (!identity.equals(current.identity())) return AcceptResult.IDENTITY_CHANGED;
            if (sample.sequence() <= current.sequence()) return AcceptResult.REJECTED;
            boolean progressed = DockNativeMotion.hasPublishedProgress(current.sample(), sample);
            DockNativeMotion.Sample effective = progressed ? sample : current.sample();
            if (state.compareAndSet(current,
                    new State(identity, effective, sample.sequence()))) {
                return progressed ? AcceptResult.PROGRESSED : AcceptResult.KEEPALIVE;
            }
        }
    }

    private AcceptResult acceptBoundAutoAim(Identity identity, DockNativeMotion.Sample sample) {
        for (;;) {
            AutoAimState current = autoAimState.get();
            if (!identity.equals(current.identity())) return AcceptResult.IDENTITY_CHANGED;
            if (sample.sequence() <= current.sequence()) return AcceptResult.REJECTED;
            boolean progressed = DockNativeMotion.hasPublishedProgress(current.sample(), sample);
            DockNativeMotion.Sample effective = progressed ? sample : current.sample();
            if (autoAimState.compareAndSet(current,
                    new AutoAimState(identity, effective, sample.sequence()))) {
                return progressed ? AcceptResult.PROGRESSED : AcceptResult.KEEPALIVE;
            }
        }
    }

    /** Promote only after WindowState has independently authenticated this exact UID/PID. */
    private boolean promotePending(Identity identity) {
        for (;;) {
            Pending early = pending.get();
            State current = state.get();
            if (early == null || !identity.equals(early.identity())
                    || !identity.equals(current.identity())) return false;
            if (early.sequence() <= current.sequence()) {
                pending.compareAndSet(early, null);
                return false;
            }
            boolean progressed = early.sample() != null && DockNativeMotion.hasPublishedProgress(
                    current.sample(), early.sample());
            DockNativeMotion.Sample effective = progressed ? early.sample() : current.sample();
            if (state.compareAndSet(current,
                    new State(identity, effective, early.sequence()))) {
                pending.compareAndSet(early, null);
                if (progressed) recordOverview(identity, early.sample());
                return progressed;
            }
        }
    }

    private boolean promoteAutoAimPending(Identity identity) {
        for (;;) {
            Pending early = autoAimPending.get();
            AutoAimState current = autoAimState.get();
            if (early == null || !identity.equals(early.identity())
                    || !identity.equals(current.identity())) return false;
            if (early.sequence() <= current.sequence()) {
                autoAimPending.compareAndSet(early, null);
                return false;
            }
            boolean progressed = early.sample() != null && DockNativeMotion.hasPublishedProgress(
                    current.sample(), early.sample());
            DockNativeMotion.Sample effective = progressed ? early.sample() : current.sample();
            if (autoAimState.compareAndSet(current,
                    new AutoAimState(identity, effective, early.sequence()))) {
                autoAimPending.compareAndSet(early, null);
                return progressed;
            }
        }
    }

    private void report(int bit, String message) {
        int current;
        do {
            current = reported.get();
            if ((current & bit) != 0) return;
        } while (!reported.compareAndSet(current, current | bit));
        try {
            diagnostic.accept(message);
        } catch (RuntimeException ignored) {
            // Diagnostics are optional and must not affect Binder handling.
        }
    }

    private void notifyChanged() {
        try {
            changed.run();
        } catch (RuntimeException ignored) {
            // Motion notification is optional; never unwind into WMS.
        }
    }

    private void notifyKeepalive() {
        try {
            keepalive.run();
        } catch (RuntimeException ignored) {
            // Health notification is optional; never unwind into WMS.
        }
    }

    public DockNativeMotion.Sample latest(int uid, int pid) {
        State current = state.get();
        Identity expected = current.identity();
        DockNativeMotion.Sample sample = current.sample();
        long now = System.nanoTime();
        return expected != null && expected.equals(new Identity(uid, pid))
                && sample != null && sample.uptimeNanos() <= now
                && now - sample.uptimeNanos() <= DockNativeMotion.MAX_AGE_NS ? sample : null;
    }

    public DockNativeMotion.Sample latestAutoAim(int uid, int pid) {
        AutoAimState current = autoAimState.get();
        Identity expected = current.identity();
        DockNativeMotion.Sample sample = current.sample();
        long now = System.nanoTime();
        return expected != null && expected.equals(new Identity(uid, pid))
                && sample != null && sample.uptimeNanos() <= now
                && now - sample.uptimeNanos() <= DockNativeMotion.AUTO_AIM_MAX_AGE_NS
                ? sample : null;
    }

    public boolean sawOverviewSince(int uid, int pid, long timestamp) {
        OverviewHit hit = overviewHit.get();
        return hit != null && hit.identity().equals(new Identity(uid, pid))
            && hit.timestamp() >= timestamp;
    }
}
