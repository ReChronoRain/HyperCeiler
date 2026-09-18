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

/** One renderer generation. All operations run on the same serial worker. */
public final class DockGlassSurfaceLease implements AutoCloseable {
    /** Keep the capture layer eligible for composition while limiting unready output to 1/255. */
    public static final float WARMUP_ALPHA = 1f / 255f;
    private final Operations operations;
    private boolean attached;
    private boolean retired;
    private boolean released;
    /** Last readiness presentation applied to the host root. */
    private boolean ready;

    public interface Operations {
        /** Reparent, set alpha and show the root in one transaction. The parent owns hiding. */
        void attach(Object parent, float alpha);
        /** Opacity-only write on an already attached host root. Must not reparent or hide. */
        void setAlpha(float alpha);
        void detach();
        void release();
    }

    public DockGlassSurfaceLease(Operations operations) {
        this.operations = operations;
    }

    public boolean isAttached() {
        return attached && !retired;
    }

    public void attach(Object parent, boolean ready) {
        if (retired || attached) return;
        operations.attach(parent, ready ? 1f : WARMUP_ALPHA);
        this.ready = ready;
        attached = true;
    }

    /**
     * Switch between capture warmup and the ready material, without hiding the host.
     *
     * <p>Waiting for a texture with a hidden capture layer can prevent the first texture from
     * arriving. Alpha zero can also exclude a buffered layer from composition. Keep a small
     * positive alpha until readiness; the compositor blur/tint still supplies the Dock background.
     * Actual launcher/rotation visibility belongs to the common WMS parent, so warmup cannot
     * show a Dock whose parent is hidden. Readiness arrives after the one-time attachment and
     * must be able to promote the same generation without another reparent.
     *
     * <p>Idempotent, so a traversal repeating the current value costs nothing.
     */
    public void setReady(boolean ready) {
        if (retired || released || !attached) return;
        if (this.ready == ready) return;
        operations.setAlpha(ready ? 1f : WARMUP_ALPHA);
        this.ready = ready;
    }

    @Override
    public void close() {
        if (released) return;
        // Retire BEFORE detaching: even a failed cleanup must reject late attaches.
        retired = true;
        // Also detach after a partially failed attach. Do not release the last
        // handle on failure; close() can retry without orphaning a visible root.
        operations.detach();
        operations.release();
        attached = false;
        released = true;
    }
}
