#include "targets/home/home_back_gesture.h"

#include <cassert>
#include <limits>

using namespace home_layout;

int main() {
    const BackGestureInputs portrait{280, 2576, 1200, 2670, 520, 21.0f};
    const BackGestureInputs landscape{31, 1010, 2670, 1201, 421, 15.8f};
    const BackGestureInputs odd_width{0, 1000, 101, 1000, 160, 39.9f};
    auto left = back_window_rect(portrait, 0, {100, 105});
    assert(left && (*left == BackWindowRect{0, 0, 71, 2576}));
    auto stock = back_window_rect(portrait, 1, {});
    assert(stock && (*stock == BackWindowRect{1132, 280, 1200, 2576}));
    auto capped = back_window_rect(odd_width, 0, {60, 400});
    assert(capped && capped->right == 50);

    for (const auto &original : {portrait, landscape, odd_width}) {
        for (int height : {10, 25, 60, 95, 100}) {
            for (int width : {100, 105, 199, 400}) {
                auto core = original;
                const bool custom = height != 60 || width != 100;
                assert(adjust_back_gesture_inputs(core, {height, width}) == custom);
                const auto span = back_width_pixels(core);
                assert(span && *span <= core.screen_width / 2);
                const auto a = back_window_rect(original, 0, {height, width});
                const auto b = back_window_rect(original, 1, {height, width});
                assert(a && (*a == BackWindowRect{0, core.top, *span, core.bottom}));
                assert(b && (*b == BackWindowRect{
                    core.screen_width - *span, core.top, core.screen_width, core.bottom}));
            }
        }
    }

    auto invalid = portrait;
    invalid.width_dp = std::numeric_limits<float>::infinity();
    assert(!back_window_rect(invalid, 0, {}));
    assert(!adjust_back_gesture_inputs(invalid, {100, 400}));
    assert(!back_window_rect(portrait, 2, {}));
    assert(!back_window_rect(portrait, 0, {101, 100}));
}
