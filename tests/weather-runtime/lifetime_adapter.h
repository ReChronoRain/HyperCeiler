#pragma once
// Controller-routing adapter only. No production page lifetime algorithm is copied.
#include "targets/weather/weather_card_lifetime.h"
#include <atomic>
namespace lifetime_fixture {
inline std::atomic_bool residue{false};
inline std::atomic<int> checks{0};
}
namespace test_card_lifetime {
inline bool has_residue() noexcept {
    lifetime_fixture::checks.fetch_add(1);
    return lifetime_fixture::residue.load();
}
}
