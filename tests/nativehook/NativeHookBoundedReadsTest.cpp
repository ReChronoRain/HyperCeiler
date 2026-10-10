/* SPDX-License-Identifier: AGPL-3.0-or-later */
// Host regression for the actual byte reader, image bounds and lease gates.
#define WEATHER_CARD_LIFETIME_TEST
#include "targets/weather/weather_card_lifetime.cpp"
#include "targets/weather/weather_binding.h"
#include "targets/weather/weather_material_binding.h"
#include "targets/weather/weather_card_binding.h"
#include "targets/weather/weather_card_control.h"
#include "targets/home/recents_binding.h"
#include "targets/home/recents_policy.h"
#include <algorithm>
#include <cstdio>
#include <cstdlib>

#define CHECK(expression) do { if (!(expression)) { \
    std::fprintf(stderr, "FAIL line %d: %s\n", __LINE__, #expression); std::exit(1); \
} } while (false)

namespace weather_card_lifetime_test {
size_t calls = 0;
bool short_transfer = false;
long call(long number, uintptr_t, uintptr_t local_address, uintptr_t local_count,
          uintptr_t remote_address, uintptr_t remote_count, uintptr_t) noexcept {
    if (number == SYS_getpid) return 123;
    CHECK(number == SYS_process_vm_readv && local_count == 1 && remote_count == 1);
    ++calls;
    const auto &local = *reinterpret_cast<const iovec *>(local_address);
    const auto &remote = *reinterpret_cast<const iovec *>(remote_address);
    CHECK(local.iov_len == remote.iov_len && local.iov_len > 0);
    const auto length = local.iov_len - (short_transfer ? 1 : 0);
    std::memcpy(local.iov_base, remote.iov_base, length);
    return static_cast<long>(length);
}
long page_size() noexcept { return 4096; }
int forward(uintptr_t, void *, size_t, int) noexcept { return 0; }
void before_slot_cas() noexcept {}
} // namespace weather_card_lifetime_test

int main() {
    using weather_card_lifetime::read_bytes;
    using weather_card_lifetime::read_object;
    std::array<std::byte, 4> source{std::byte{1}, std::byte{2}, std::byte{3}, std::byte{4}};
    std::array<std::byte, 6> destination{};
    destination.fill(std::byte{0x55});
    const auto address = reinterpret_cast<uintptr_t>(source.data());
    CHECK(read_bytes(address, std::span(destination).subspan(1, 4)));
    CHECK(destination.front() == std::byte{0x55} && destination.back() == std::byte{0x55});
    CHECK(std::equal(source.begin(), source.end(), destination.begin() + 1));
    uint32_t word = 0;
    CHECK(read_object(address, word) && word == 0x04030201);
    const auto before = weather_card_lifetime_test::calls;
    CHECK(!read_bytes(0, std::span(destination)));
    CHECK(!read_bytes(UINTPTR_MAX - 1, std::span(destination)));
    CHECK(!read_bytes(address, {}));
    CHECK(weather_card_lifetime_test::calls == before);
    weather_card_lifetime_test::short_transfer = true;
    CHECK(!read_object(address, word));
    weather_card_lifetime_test::short_transfer = false;
    std::puts("PASS bounded process-memory reads preserve canaries and reject short/invalid spans");

    // A final one-byte chunk must neither over-read nor overwrite the 256B scratch buffer.
    std::array<uint8_t, 257> expected{};
    for (size_t i = 0; i < expected.size(); ++i) expected[i] = static_cast<uint8_t>(i);
    const std::array<weather_card_lifetime_guard::Guard, 1> lifetime_guards{{
        {0, expected.data(), expected.size()}}};
    const auto lifetime_base = reinterpret_cast<uintptr_t>(expected.data());
    const auto lifetime_calls = weather_card_lifetime_test::calls;
    CHECK(weather_card_lifetime::guards(lifetime_base, lifetime_guards));
    CHECK(weather_card_lifetime_test::calls == lifetime_calls + 2);
    weather_card_lifetime_test::short_transfer = true;
    CHECK(!weather_card_lifetime::guards(lifetime_base, lifetime_guards));
    weather_card_lifetime_test::short_transfer = false;
    constexpr uintptr_t base = 0x100000;
    size_t chunks = 0;
    bool fail_reader = false;
    auto reader = [&](uintptr_t at, std::span<std::byte> output) {
        CHECK(at >= base && at - base < expected.size());
        CHECK(!output.empty() && output.size() <= 256 && output.size() <= expected.size() - (at - base));
        ++chunks;
        if (fail_reader) return false;
        for (size_t i = 0; i < output.size(); ++i) output[i] = static_cast<std::byte>(expected[at - base + i]);
        return true;
    };
    std::array<weather_guard::Guard, 1> weather_guards{{{0, expected}}};
    CHECK(weather_binding::detail::image(base, expected.size(), weather_guards, reader));
    CHECK(chunks == 2);
    chunks = 0;
    CHECK(!weather_binding::detail::image(base, expected.size() - 1, weather_guards, reader));
    CHECK(chunks == 0);
    weather_guards[0].va = UINTPTR_MAX;
    CHECK(!weather_binding::detail::image(base, expected.size(), weather_guards, reader));
    CHECK(chunks == 0);
    std::array<weather_material_guard::Guard, 1> material_guards{{{0, expected}}};
    CHECK(weather_material_binding::detail::image(base, expected.size(), material_guards, reader));
    CHECK(chunks == 2);
    chunks = 0;
    CHECK(!weather_material_binding::detail::image(base, expected.size() - 1, material_guards, reader));
    CHECK(chunks == 0);
    fail_reader = true;
    CHECK(!weather_material_binding::detail::image(base, expected.size(), material_guards, reader));
    CHECK(chunks == 1);
    std::puts("PASS image bounds reject truncated/wrapped guards before reading and preserve partial chunks");

    weather_snapshot::Cache cache;
    weather_snapshot::Snapshot snapshot;
    CHECK(cache.publish(weather_snapshot::unavailable_words(), 1));
    CHECK(cache.load_snapshot(1, snapshot) && !snapshot.available);
    alignas(8) uint64_t card_word = 0;
    weather_card_control::Lease card_lease(card_word);
    snapshot = {true, true, 1, 1, 1000, 3, 2, false};
    CHECK(card_lease.publish(card_lease.load(), snapshot, 1));
    const auto stale = card_lease.load();
    card_lease.cancel();
    CHECK(card_lease.terminal() && !card_lease.publish(stale, snapshot, 1));
    CHECK(!card_lease.clear(stale) && card_lease.terminal());
    alignas(8) uint64_t recents_word = 0;
    home_recents_policy::Lease recents_lease(recents_word);
    hyos_snapshot::Snapshot home;
    home.available = true; home.flags = hyos_snapshot::kAllowed;
    home.epoch = 1; home.revision = 1; home.valid_until = 10'000'000;
    home.config.tweaks.animation_recents_enabled = true;
    home.config.tweaks.animation_recents_rate_percent = 100;
    CHECK(recents_lease.arm(home, 1));
    CHECK(recents_lease.load() > home_recents_policy::kDenied);
    CHECK(!recents_lease.refresh(home, home.valid_until));
    CHECK(recents_lease.load() == home_recents_policy::kDenied);
    CHECK(!recents_lease.arm(home, 1));
    std::puts("PASS snapshots and acquire-loaded leases keep expiry and terminal cancellation semantics");
    return 0;
}
