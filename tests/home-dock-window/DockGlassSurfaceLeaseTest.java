/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.tests.dock;

import com.sevtinge.hyperceiler.libhook.rules.home.dock.DockGlassSurfaceLease;

public final class DockGlassSurfaceLeaseTest {
    private static final class Fake implements DockGlassSurfaceLease.Operations {
        boolean parented;
        boolean failAttach;
        boolean failDetach;
        int attaches;
        int releases;
        int alphaWrites;
        boolean failAlpha;
        float alpha;

        @Override public void attach(Object parent, float alpha) {
            check(releases == 0, "cannot attach a released handle");
            parented = true;
            attaches++;
            this.alpha = alpha;
            if (failAttach) throw new IllegalStateException("partial attach");
        }
        @Override public void setAlpha(float alpha) {
            check(parented, "must attach before writing opacity");
            alphaWrites++;
            if (failAlpha) throw new IllegalStateException("alpha failed");
            this.alpha = alpha;
        }
        @Override public void detach() {
            if (failDetach) throw new IllegalStateException("detach failed");
            parented = false;
        }
        @Override public void release() {
            check(!parented, "must detach before releasing the last handle");
            releases++;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException expected) {
            return;
        }
        throw new AssertionError("expected simulated failure");
    }

    public static void main(String[] args) {
        Object dock = new Object();
        for (int generation = 0; generation < 12; generation++) {
            Fake remote = new Fake();
            DockGlassSurfaceLease lease = new DockGlassSurfaceLease(remote);
            lease.attach(dock, true);
            lease.attach(dock, false);
            check(lease.isAttached() && remote.attaches == 1, "attach once per generation");
            check(remote.alpha == 1f, "the first attach owns the initial opacity");
            lease.close();
            lease.attach(dock, true); // Late queued WMS request after renderer death.
            lease.close();
            check(!lease.isAttached() && !remote.parented, "no orphan after renderer death");
            check(remote.attaches == 1 && remote.releases == 1, "no resurrection or double release");
        }

        // A rotation creates a new host on an idle desktop. Keep it eligible for capture
        // before its first texture; readiness promotes that same host without reattachment.
        Fake late = new Fake();
        DockGlassSurfaceLease lateLease = new DockGlassSurfaceLease(late);
        lateLease.attach(dock, false);
        check(late.alpha > 0f && late.alpha <= 1f / 255f,
            "warmup must permit capture while bounding unready output");
        lateLease.attach(dock, true);
        check(late.alpha == DockGlassSurfaceLease.WARMUP_ALPHA,
            "a repeated attach must not promote unready material");
        lateLease.setReady(false);
        check(late.alphaWrites == 0, "writing the current readiness is a no-op");
        lateLease.setReady(true);
        check(late.alpha == 1f && late.alphaWrites == 1 && late.attaches == 1,
            "readiness must show the already attached host");
        lateLease.setReady(true);
        check(late.alphaWrites == 1, "opacity writes stay idempotent");
        lateLease.setReady(false);
        check(late.alpha == DockGlassSurfaceLease.WARMUP_ALPHA && late.alphaWrites == 2,
            "refresh dims the old material without starving capture");
        late.failAlpha = true;
        expectFailure(() -> lateLease.setReady(true));
        check(late.alpha == DockGlassSurfaceLease.WARMUP_ALPHA,
            "failed presentation does not acknowledge readiness");
        late.failAlpha = false;
        lateLease.setReady(true);
        check(late.alpha == 1f && late.alphaWrites == 4,
            "retry applies a readiness change whose transaction failed");
        lateLease.close();
        lateLease.setReady(false);
        check(late.alphaWrites == 4, "a retired lease rejects opacity writes");

        Fake unattached = new Fake();
        DockGlassSurfaceLease beforeAttach = new DockGlassSurfaceLease(unattached);
        beforeAttach.setReady(true);
        check(unattached.alphaWrites == 0, "readiness before attach is dropped");

        Fake failed = new Fake();
        failed.failAttach = true;
        DockGlassSurfaceLease partial = new DockGlassSurfaceLease(failed);
        expectFailure(() -> partial.attach(dock, true));
        partial.setReady(true);
        check(failed.alphaWrites == 0, "a partially attached root is not written to");
        failed.failDetach = true;
        expectFailure(partial::close);
        partial.attach(dock, true);
        check(failed.releases == 0 && failed.attaches == 1, "retain failed cleanup handle; reject late attach");
        failed.failDetach = false;
        partial.close();
        check(!failed.parented && failed.releases == 1, "retry retires a partially attached root");

        Fake cancelled = new Fake();
        DockGlassSurfaceLease unparented = new DockGlassSurfaceLease(cancelled);
        unparented.close();
        unparented.attach(dock, true);
        check(cancelled.attaches == 0 && cancelled.releases == 1, "cancel before first attach");
        System.out.println("DockGlassSurfaceLease tests passed");
    }
}
