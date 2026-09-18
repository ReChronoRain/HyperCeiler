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

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.function.IntUnaryOperator;

/** In-memory leases only. UID ownership is verified by the Android caller. */
public final class DockGlassProcessPolicy {
    private record Owner(int uid, int pid) {}
    private final IdentityHashMap<Object, Owner> owners = new IdentityHashMap<>();

    public synchronized void acquire(Object token, int uid) {
        if (uid < 10000) throw new IllegalArgumentException("An application UID is required");
        owners.put(token, new Owner(uid, -1));
    }

    public synchronized void setPid(Object token, int pid) {
        Owner owner = owners.get(token);
        if (owner != null && pid > 0) owners.put(token, new Owner(owner.uid, pid));
    }

    public synchronized void release(Object token) { owners.remove(token); }
    public synchronized void clear() { owners.clear(); }

    /** A lease is protectable only after its renderer PID still resolves to the verified UID. */
    public synchronized boolean hasLiveOwner(IntUnaryOperator uidForPid) {
        for (Owner owner : owners.values()) {
            if (owner.pid > 0 && uidForPid.applyAsInt(owner.pid) == owner.uid) return true;
        }
        return false;
    }

    public synchronized int[] filter(int[] requested, boolean pids, IntUnaryOperator uidForPid) {
        if (owners.isEmpty()) return requested;
        int[] result = new int[requested.length];
        int size = 0;
        for (int value : requested) {
            boolean protectedOwner = false;
            for (Owner owner : owners.values()) {
                if (value == (pids ? owner.pid : owner.uid) && value > 0
                        && (!pids || uidForPid.applyAsInt(value) == owner.uid)) {
                    protectedOwner = true;
                    break;
                }
            }
            if (!protectedOwner) result[size++] = value;
        }
        return size == requested.length ? requested : Arrays.copyOf(result, size);
    }
}
