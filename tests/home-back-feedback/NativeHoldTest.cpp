/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_back_hold.h"
#include "targets/home/home_back_morph.h"
#include <iostream>
#include <limits>
#include <stdexcept>

int main() {
    home_back_feedback::HoldState state;
    unsigned checks = 0;
    auto check = [&](bool value, const char *message) {
        ++checks;
        if (!value)
            throw std::runtime_error(message);
    };
    auto event = [&](int action, float x, float y, int64_t time) {
        return state.update(1, action, 1, x, y, time, 3, 1200, 7);
    };
    event(0, 4, 1000, 1000);
    event(2, 268, 1000, 1200);
    check(!state.ready(2200), "old 88dp drawing knee must not activate");
    event(2, 399, 1000, 2300);
    check(!state.ready(3300), "below full travel");
    event(2, 400, 1000, 3400);
    check(!state.ready(3400), "only start deciding after full travel");
    check(state.ready(3417), "stable full travel needs no extra 300ms timer");
    event(0, 4, 1000, 4000);
    event(2, 424, 1000, 4010);
    check(!state.ready(4027), "fast movement has not settled");
    check(state.ready(4210), "fast full pull settles within 200ms");
    event(2, 421, 1003, 4220);
    check(state.geometry(true), "small jitter stays armed");
    for (int n = 1; n <= 4; n++)
        event(2, 424 - 3 * n, 1000, 4230 + n * 10);
    check(!state.geometry(true), "small withdrawals accumulate from peak");
    check(!state.ready(5000), "stationary retreat does not rearm above full travel");
    event(2, 424, 1000, 5100);
    check(!state.ready(5100), "repull must be evaluated anew");
    check(state.ready(5200), "repull can rearm without a long fixed timer");

    event(0, 4, 1000, 6000);
    event(2, 604, 1000, 6100);
    check(state.ready(6400), "overpull arms");
    event(2, 592, 1000, 6410);
    check(!state.geometry(true), "overpull still cancels on small retreat");
    check(!state.ready(6800), "overpull retreat stays cancelled");
    event(0, 1196, 1000, 7000);
    event(2, 800, 1100, 7200);
    check(state.ready(7500) && !state.left(), "right full travel");
    event(2, 812, 1100, 7510);
    check(!state.geometry(true), "right retreat");
    event(0, 4, 1000, 8000);
    event(2, 404, 1500, 8200);
    check(!state.ready(8500), "vertical drag");
    event(0, 4, 1000, 9000);
    event(2, 404, 1000, 9010);
    event(1, 404, 1000, 9020);
    check(!state.ready(9020), "fast release remains back");
    event(0, 4, 1000, 10000);
    for (int i = 1; i <= 20; i++) {
        event(2, 4 + i * 20, 1000, 10000 + i * 50);
        state.ready(10000 + i * 50 + 17);
    }
    check(state.ready(11034), "slow pull arms promptly once it reaches full travel");
    event(0, 4, 1000, 12000);
    check(!state.update(1, 2, 2, 420, 1000, 12300, 3, 1200, 7), "multitouch cancels");
    check(!state.ready(12600), "cancel remains cancelled");
    event(0, 4, 1000, 13000);
    check(!state.update(2, 2, 1, 420, 1000, 13300, 3, 1200, 7), "owner changed");
    event(0, 4, 1000, 14000);
    check(!state.update(1, 2, 1, 420, 1000, 14300, 3, 1200, 8), "configuration changed");
    event(0, 4, 1000, 15000);
    check(!event(3, 420, 1000, 15400), "cancel action");
    event(0, 4, 1000, 16000);
    check(!event(2, std::numeric_limits<float>::quiet_NaN(), 1000, 16100), "invalid position");
    event(0, 4, 1000, 17000);
    check(!event(2, 420, 1000, 16999), "time went backwards");
    event(0, 4, 1000, 18000);
    check(!event(2, 420, 1000, 28001), "stale gesture");
    state.update(1, 0, 1, 4, 1000, 29000, 3, 1080, 7);
    state.update(1, 2, 1, 360, 1000, 29200, 3, 1080, 7);
    check(!state.ready(29600), "reference 1080px threshold is above 356px");
    state.update(1, 2, 1, 361, 1000, 29700, 3, 1080, 7);
    check(state.ready(29717), "reference 1080px full travel");

    state.update(1, 0, 1, 4, 600, 31000, 3, 2608, 7, 1200);
    state.update(1, 2, 1, 424, 600, 31200, 3, 2608, 7, 1200);
    check(state.ready(31500) && state.left() && state.limit == 396,
          "landscape left uses short-side threshold");
    state.update(1, 0, 1, 2604, 600, 32000, 3, 2608, 7, 1200);
    state.update(1, 2, 1, 2184, 600, 32200, 3, 2608, 7, 1200);
    check(state.ready(32500) && !state.left() && state.limit == 396,
          "landscape right uses actual screen width");
    state.update(1, 2, 1, 2196, 600, 32510, 3, 2608, 7, 1200);
    check(!state.geometry(true), "landscape small withdrawal cancels");
    state.update(1, 0, 1, 4, 600, 33000, 3, 2608, 7, 1200);
    state.update(1, 2, 1, 399, 600, 33200, 3, 2608, 7, 1200);
    check(!state.ready(33500), "landscape partial pull stays back");
    check(!state.update(1, 2, 1, 424, 600, 33600, 3, 1200, 7, 1200),
          "rotation invalidates an in-flight gesture");
    check(!state.update(1, 0, 1, 4, 600, 34000, 3, 1200, 7, 2608), "invalid short-side dimensions rejected");

    state.update(1, 0, 1, 4, 600, 35000, 3, 2608, 7);
    state.update(1, 2, 1, 424, 600, 35200, 3, 2608, 7);
    check(!state.ready(35400), "uncaptured landscape span has not armed");
    check(state.update(1, 2, 1, 424, 600, 35500, 3, 2608, 7, 1200),
          "first configuration sample can initialize current gesture");
    check(state.ready(35517) && state.limit == 396, "captured short side fixes first landscape gesture");

    home_back_arrow::Morph morph;
    auto sample = [&](uint64_t session, bool ready, uint64_t now) {
        return morph.sample(session, ready, now);
    };
    check(sample(1, false, 0).icon == 1 && !morph.animating, "ordinary arrow is unchanged");
    check(sample(1, true, 10).icon == 0 && morph.animating, "icon transition starts small");
    check(std::abs(sample(1, true, 60).icon - .75f) < .001f, "100ms quadratic icon expansion");
    check(sample(1, true, 110).icon == 1 && !morph.animating, "icon animation finishes at 100ms");
    check(sample(1, false, 120).icon == 0 && morph.animating, "cancel animates incoming back arrow");
    check(sample(1, false, 220).icon == 1 && !morph.animating, "back arrow finishes at 100ms");
    sample(1, true, 230);
    sample(1, true, 270);
    check(sample(1, false, 280).icon == 0 && morph.animating, "interrupted transition reverses");
    check(sample(2, false, 300).icon == 1 && !morph.animating, "new gesture does not inherit animation");
    check(sample(0, false, 310).icon == 1 && !morph.animating, "gesture end stops animation frames");
    std::cout << "Native hold and morph: " << checks << " checks passed\n";
}
