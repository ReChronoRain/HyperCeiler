/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
#include <cstddef>
#include <memory>
#include <vector>

struct BackIconPreview {
    uint64_t session = 0;
    bool left = false;
    uint32_t width = 0, height = 0;
    std::vector<std::byte> pixels;
};
struct BackVisualState {
    uint64_t session = 0;
    bool ready = false;
    std::shared_ptr<const BackIconPreview> icon;
};
BackVisualState hc_back_feedback_visual(bool left);
bool hc_back_feedback_presented(uint64_t session);

void hc_back_feedback_start();
void hc_back_feedback_short_side(float pixels);
bool hc_back_feedback_touch(uintptr_t owner, int action, int pointers, float x, float y, int64_t time,
                            float density, float width, uint64_t revision);
void hc_back_feedback_cancel();
void hc_back_feedback_commit();
