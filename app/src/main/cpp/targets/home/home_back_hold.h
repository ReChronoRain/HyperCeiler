/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <algorithm>
#include <cmath>
#include <cstdint>

namespace home_back_feedback {
struct HoldState {
    static constexpr uint64_t sample_ms = 17;
    uintptr_t owner = 0;
    uint64_t revision = 0, start = 0, latest = 0, sampled = 0, full_since = 0;
    float start_x = 0, start_y = 0, x = 0, y = 0, density = 1, width = 0;
    float limit = 0, basis = 0, peak = 0, rearm_from = 0, slow_x = 0, fast_x = 0;
    bool active = false, retracting = false, basis_known = false;
    void reset() {
        active = false;
        full_since = 0;
    }
    bool left() const {
        return start_x < width / 2;
    }
    float inward() const {
        return left() ? x - start_x : start_x - x;
    }
    bool geometry(bool armed) const {
        return active && !retracting && inward() >= limit - (armed ? 4 * density : 0) &&
               std::abs(y - start_y) <= std::abs(x - start_x);
    }
    void advance(uint64_t now) {
        if (!active || now < sampled)
            return;
        uint64_t count = std::min<uint64_t>((now - sampled) / sample_ms, 600);
        if (!count)
            return;
        // Legacy GestureStubView follows two positions at 17 ms, with 1/4 and 1/2 gains.
        slow_x = x + (slow_x - x) * std::pow(.75f, float(count));
        fast_x = x + (fast_x - x) * std::pow(.5f, float(count));
        sampled += count * sample_ms;
    }
    bool update(uintptr_t receiver, int action, int pointers, float px, float py, int64_t time, float scale,
                float screen_width, uint64_t config, float short_side = 0) {
        float span = short_side == 0 ? screen_width : short_side;
        if (pointers != 1 || !std::isfinite(px) || !std::isfinite(py) || !std::isfinite(scale) ||
            scale < .5f || scale > 8 || !std::isfinite(screen_width) || screen_width <= 0 || time <= 0 ||
            !std::isfinite(span) || span <= 0 || span > screen_width || action == 3 || action == 5 ||
            action == 6) {
            reset();
            return false;
        }
        uint64_t stamp = uint64_t(time);
        if (action == 0) {
            active = true;
            owner = receiver;
            revision = config;
            start = latest = sampled = stamp;
            full_since = 0;
            start_x = x = slow_x = fast_x = px;
            start_y = y = py;
            density = scale;
            width = screen_width;
            basis = span;
            basis_known = short_side > 0;
            limit = basis * 33.f / 100.f;
            peak = rearm_from = 0;
            retracting = false;
            return true;
        }
        if (active && !basis_known && short_side > 0 && width == screen_width && owner == receiver &&
            revision == config) {
            basis_known = true;
            if (basis != span) {
                basis = span;
                limit = basis * 33.f / 100.f;
                full_since = 0;
            }
        }
        if (!active || owner != receiver || revision != config || width != screen_width || basis != span ||
            density != scale || stamp < latest || stamp - start > 10000 || (action != 1 && action != 2)) {
            reset();
            return false;
        }
        advance(stamp);
        bool was_full = geometry(false) && full_since != 0;
        latest = stamp;
        x = px;
        y = py;
        float distance = inward();
        if (retracting) {
            rearm_from = std::min(rearm_from, distance);
            if (distance >= limit && distance - rearm_from >= 4 * density) {
                retracting = false;
                peak = distance;
            }
        } else {
            peak = std::max(peak, distance);
            if (peak >= limit && peak - distance >= 4 * density) {
                retracting = true;
                rearm_from = distance;
            }
        }
        if (!geometry(false))
            full_since = 0;
        else if (!was_full)
            full_since = stamp;
        return true;
    }
    uint64_t deadline() const {
        return sampled + sample_ms;
    }
    bool ready(uint64_t now) {
        advance(now);
        return geometry(false) && full_since && now >= full_since + sample_ms && now - start <= 10000 &&
               std::abs(x - slow_x) < 20 && std::abs(x - fast_x) < 20;
    }
};
} // namespace home_back_feedback
