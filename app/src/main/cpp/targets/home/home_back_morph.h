/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <algorithm>
#include <cstdint>

namespace home_back_arrow {
struct MorphFrame {
    float icon = 1;
};
struct Morph {
    uint64_t session = 0, changed = 0;
    bool recent = false, animating = false;
    MorphFrame sample(uint64_t gesture, bool ready, uint64_t now) {
        if (session != gesture || !gesture) {
            *this = {};
            session = gesture;
        }
        if (!gesture)
            return {};
        if (ready != recent) {
            recent = ready;
            changed = now;
            animating = true;
        }
        if (!animating)
            return {};
        float age = float(now >= changed ? now - changed : 0);
        float progress = std::clamp(age / 100, 0.f, 1.f);
        animating = age < 100;
        return {1 - (1 - progress) * (1 - progress)};
    }
};
} // namespace home_back_arrow
