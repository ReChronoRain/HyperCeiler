/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

final class BackHoldState {
    long serial;
    long owner;
    long revision;
    long start;
    long moved;
    long latest;
    float startX;
    float startY;
    float anchorX;
    float anchorY;
    float x;
    float y;
    float density;
    float width;
    boolean active;

    void reset() {
        active = false;
        serial++;
    }

    boolean update(long receiver, int action, int pointers, float px, float py, long time,
        float scale, float screenWidth, long config) {
        if (pointers != 1 || !Float.isFinite(px) || !Float.isFinite(py)
            || !Float.isFinite(scale) || scale < .5f || scale > 8
            || !Float.isFinite(screenWidth) || screenWidth <= 0 || time <= 0
            || action == 3 || action == 5 || action == 6) {
            reset();
            return false;
        }
        if (action == 0) {
            reset();
            active = true;
            owner = receiver;
            revision = config;
            start = moved = latest = time;
            startX = anchorX = x = px;
            startY = anchorY = y = py;
            density = scale;
            width = screenWidth;
            return true;
        }
        if (!active || owner != receiver || revision != config || time < latest
            || time - start > 10000 || (action != 1 && action != 2)) {
            reset();
            return false;
        }
        latest = time;
        x = px;
        y = py;
        if (Math.hypot(x - anchorX, y - anchorY) > 6 * density) {
            moved = time;
            anchorX = x;
            anchorY = y;
        }
        return true;
    }

    boolean left() {
        return startX < width / 2;
    }

    boolean geometry(boolean armed) {
        float dx = x - startX;
        float dy = y - startY;
        return active && (left() ? dx : -dx) >= (armed ? 32 : 48) * density
            && Math.abs(dy) <= Math.abs(dx);
    }

    long deadline() {
        return Math.max(start + 350, moved + 300);
    }

    boolean ready(long now) {
        return geometry(false) && now >= deadline() && now - start <= 10000;
    }
}
