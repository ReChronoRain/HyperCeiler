// SPDX-License-Identifier: AGPL-3.0-or-later
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

/** Authenticated, expiring scene lane, independent of motion/scale and Dock preferences. */
public final class HomeRecentsBarState {
    public static final long MAX_AGE_NS = 6_000_000_000L;
    private record Packet(int uid, int pid, long sequence, long revision, int mask, long deadline) { }
    private int uid = -1, pid = -1;
    private Packet current;
    public synchronized void bind(int newUid, int newPid) {
        if (uid != newUid || pid != newPid) current = null;
        uid = newUid; pid = newPid;
    }
    public synchronized boolean identity(int callerUid, int callerPid) {
        return uid >= 10000 && pid > 0 && uid == callerUid && pid == callerPid;
    }
    public synchronized boolean accept(int callerUid, int callerPid, long sequence,
            long timestamp, long mask, long revision, long confirmedRevision, long now) {
        if (!identity(callerUid, callerPid) || sequence <= 0 || timestamp <= 0 || timestamp > now
            || now - timestamp >= MAX_AGE_NS || mask < 0 || mask > 15
            || ((mask & 8) == 0 && mask != 0) || revision <= 0 || confirmedRevision != revision
            || timestamp > Long.MAX_VALUE - MAX_AGE_NS) return false;
        if (current != null && (sequence <= current.sequence || timestamp < current.deadline - MAX_AGE_NS
            || revision < current.revision
            || (revision == current.revision && mask != current.mask))) return false;
        current = new Packet(callerUid, callerPid, sequence, revision, (int) mask, timestamp + MAX_AGE_NS);
        return true;
    }
    public synchronized boolean overview(long now) {
        return current != null && now > 0 && now < current.deadline
            && (current.mask & 8) != 0 && (current.mask & 7) != 0;
    }
    public synchronized long deadline() { return current == null ? 0 : current.deadline; }
    public synchronized void clear() { current = null; uid = -1; pid = -1; }
}
