/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.tests.dock;

import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockGlassProcessPolicy;
import java.util.Arrays;

public final class DockGlassProcessPolicyTest {
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Renderer guard scope/lifetime regression");
    }

    public static void main(String[] args) {
        DockGlassProcessPolicy policy = new DockGlassProcessPolicy();
        Object first = new Object();
        Object second = new Object();
        int[] batch = {1000, 10371, 11000};
        check(policy.filter(batch, false, null) == batch);
        policy.acquire(first, 10371);
        policy.acquire(second, 10371);
        check(!policy.hasLiveOwner(pid -> 10371));
        check(Arrays.equals(policy.filter(batch, false, null), new int[]{1000, 11000}));
        check(batch[1] == 10371); // Caller-owned request array is unchanged.
        check(policy.filter(new int[]{10371}, false, null).length == 0);
        policy.setPid(first, 20001);
        check(policy.hasLiveOwner(pid -> 10371));
        check(!policy.hasLiveOwner(pid -> 11000));
        int[] pids = {20001, 20002};
        check(Arrays.equals(policy.filter(pids, true, pid -> 10371), new int[]{20002}));
        check(policy.filter(pids, true, pid -> 11000) == pids); // PID reused by another app.
        policy.release(first);
        check(!policy.hasLiveOwner(pid -> 10371));
        check(policy.filter(pids, true, pid -> 10371) == pids);
        check(policy.filter(batch, false, null).length == 2); // Resize handover still owns UID.
        policy.release(second);
        check(policy.filter(batch, false, null) == batch);
        policy.acquire(first, 10371);
        policy.clear();
        check(policy.filter(batch, false, null) == batch);
        System.out.println("DockGlassProcessPolicy tests passed");
    }
}
