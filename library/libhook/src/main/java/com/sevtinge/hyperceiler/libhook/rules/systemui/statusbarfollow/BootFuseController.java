/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow;

import java.util.Objects;

/**
 * Pure boot-fuse state machine. Storage, time and thread identity are injected so the
 * transition rules can be tested without Android or real filesystem access.
 */
public final class BootFuseController {
    public enum State { IDLE, TRYING, TRIPPED }

    public enum Decision {
        ALLOW,
        BLOCKED,
        TRIPPED,
        ERROR,
        REENTERED,
        BUSY
    }

    public interface Store {
        /** Return null only when the marker cannot be read. */
        String read();

        boolean write(String value);

        boolean clear();
    }

    public interface Clock {
        long nowMillis();
    }

    public interface ThreadIdentity {
        long id();
    }

    private static final int MAX_TOKEN = 64;
    private static final int MAX_SNAPSHOT = 512;

    private final Store store;
    private final Clock clock;
    private final ThreadIdentity threads;
    private final Object lock = new Object();

    private boolean installClaimed;
    private long installThread = Long.MIN_VALUE;

    public BootFuseController(Store store, Clock clock, ThreadIdentity threads) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
        this.threads = Objects.requireNonNull(threads);
    }

    /**
     * Claim one installation attempt. A previous TRYING marker means one incomplete boot;
     * after two incomplete markers the next attempt is tripped and denied.
     */
    public Decision beginInstall(String resetToken) {
        final long thread = threads.id();
        synchronized (lock) {
            if (installClaimed) {
                return installThread == thread ? Decision.REENTERED : Decision.BUSY;
            }
            installClaimed = true;
            installThread = thread;

            Snapshot snapshot = readSnapshot();
            if (snapshot == null) return Decision.ERROR;

            if (validResetToken(resetToken) && !resetToken.equals(snapshot.resetToken)) {
                snapshot = snapshot.resetTo(clock.nowMillis(), resetToken);
                if (!writeSnapshot(snapshot)) return Decision.ERROR;
            }

            if (snapshot.state == State.TRIPPED) return Decision.TRIPPED;

            if (snapshot.state == State.TRYING) {
                int incomplete = snapshot.incomplete + 1;
                State next = incomplete >= 2 ? State.TRIPPED : State.TRYING;
                Snapshot updated = new Snapshot(next, snapshot.attempt + 1,
                    incomplete, clock.nowMillis(), snapshot.resetToken);
                if (!writeSnapshot(updated)) return Decision.ERROR;
                return next == State.TRIPPED ? Decision.TRIPPED : Decision.ALLOW;
            }

            Snapshot started = new Snapshot(State.TRYING, snapshot.attempt + 1,
                0, clock.nowMillis(), snapshot.resetToken);
            return writeSnapshot(started) ? Decision.ALLOW : Decision.ERROR;
        }
    }

    /** Clear a successful attempt after the platform confirms boot completion. */
    public boolean markBootCompleted() {
        synchronized (lock) {
            Snapshot snapshot = readSnapshot();
            if (snapshot == null) return false;
            if (snapshot.state == State.TRIPPED) return false;
            if (snapshot.state == State.IDLE) return true;
            Snapshot cleared = new Snapshot(State.IDLE, snapshot.attempt,
                0, clock.nowMillis(), snapshot.resetToken);
            return writeSnapshot(cleared);
        }
    }

    /** Explicit user reset; never automatically called by the state machine. */
    public boolean reset(String resetToken) {
        if (!validResetToken(resetToken)) return false;
        synchronized (lock) {
            Snapshot snapshot = readSnapshot();
            if (snapshot == null) return false;
            Snapshot cleared = snapshot.resetTo(clock.nowMillis(), resetToken);
            installClaimed = false;
            installThread = Long.MIN_VALUE;
            return writeSnapshot(cleared);
        }
    }

    public State inspectState() {
        synchronized (lock) {
            Snapshot snapshot = readSnapshot();
            return snapshot == null ? State.TRIPPED : snapshot.state;
        }
    }

    private Snapshot readSnapshot() {
        String raw = store.read();
        if (raw == null) return null;
        if (raw.isEmpty()) return Snapshot.initial();
        return Snapshot.parse(raw);
    }

    private boolean writeSnapshot(Snapshot snapshot) {
        String raw = snapshot.serialize();
        return raw.length() <= MAX_SNAPSHOT && store.write(raw);
    }

    private static boolean validResetToken(String token) {
        return token != null && !token.isEmpty() && token.length() <= MAX_TOKEN;
    }

    static final class Snapshot {
        final State state;
        final int attempt;
        final int incomplete;
        final long updatedAt;
        final String resetToken;

        Snapshot(State state, int attempt, int incomplete, long updatedAt, String resetToken) {
            this.state = Objects.requireNonNull(state);
            this.attempt = Math.max(0, attempt);
            this.incomplete = Math.max(0, incomplete);
            this.updatedAt = Math.max(0, updatedAt);
            this.resetToken = resetToken == null ? "" : resetToken;
        }

        static Snapshot initial() {
            return new Snapshot(State.IDLE, 0, 0, 0, "");
        }

        Snapshot resetTo(long now, String token) {
            return new Snapshot(State.IDLE, attempt, 0, now, token);
        }

        String serialize() {
            return "v=1\nstate=" + state.name() + "\nattempt=" + attempt
                + "\nincomplete=" + incomplete + "\nupdated=" + updatedAt
                + "\nreset=" + resetToken + "\n";
        }

        static Snapshot parse(String raw) {
            String[] lines = raw.split("\n", -1);
            if (lines.length != 7 || !"v=1".equals(lines[0])) return null;
            State state;
            try {
                state = State.valueOf(value(lines[1], "state"));
                int attempt = Integer.parseInt(value(lines[2], "attempt"));
                int incomplete = Integer.parseInt(value(lines[3], "incomplete"));
                long updated = Long.parseLong(value(lines[4], "updated"));
                String token = value(lines[5], "reset");
                if (attempt < 0 || incomplete < 0 || updated < 0
                    || token.length() > MAX_TOKEN || !validResetToken(token) && !token.isEmpty())
                    return null;
                return new Snapshot(state, attempt, incomplete, updated, token);
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        private static String value(String line, String key) {
            String prefix = key + "=";
            return line.startsWith(prefix) ? line.substring(prefix.length()) : "";
        }
    }
}
