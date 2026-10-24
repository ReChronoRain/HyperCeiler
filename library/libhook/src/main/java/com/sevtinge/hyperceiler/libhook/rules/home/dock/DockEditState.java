// SPDX-License-Identifier: AGPL-3.0-or-later
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

/** Separate persistent state lane: motion packets cannot erase an editing transition. */
public final class DockEditState {
    private record Packet(int uid, int pid, long sequence, long revision, boolean editing) { }
    private int uid = -1, pid = -1;
    private Packet current, pending;
    public synchronized boolean bind(int newUid, int newPid) {
        if (newUid < 10000 || newPid <= 0) return false;
        boolean before = hidden(newUid, newPid);
        if (uid != newUid || pid != newPid) { current = null; uid = newUid; pid = newPid; }
        if (pending != null && pending.uid == uid && pending.pid == pid) {
            if (current == null || pending.sequence > current.sequence) current = pending;
            pending = null;
        }
        return before != hidden(newUid, newPid);
    }
    public synchronized boolean accept(int callerUid, int callerPid, long sequence,
            long timestamp, long value, long entryHits, long revision, long now) {
        if (callerUid < 10000 || callerPid <= 0 || (uid >= 0 && uid != callerUid)
            || sequence <= 0 || timestamp <= 0 || timestamp > now
            || now - timestamp > 5_000_000_000L || value < 0 || value > 1
            || revision <= 0 || entryHits != revision) return false;
        boolean matches = uid == callerUid && pid == callerPid;
        Packet prior = matches ? current : pending;
        if (prior != null && prior.uid == callerUid && prior.pid == callerPid
            && (sequence <= prior.sequence || revision < prior.revision
                || (revision == prior.revision && (value == 1) != prior.editing))) return false;
        Packet next = new Packet(callerUid, callerPid, sequence, revision, value == 1);
        if (!matches) { pending = next; return false; }
        boolean changed = current == null ? next.editing : current.editing != next.editing;
        current = next;
        return changed;
    }
    public synchronized boolean hidden(int queryUid, int queryPid) {
        return current != null && uid == queryUid && pid == queryPid && current.editing;
    }
}
