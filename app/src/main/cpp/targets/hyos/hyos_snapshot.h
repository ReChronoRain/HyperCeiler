/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include "targets/home/home_layout_config.h"
#include <array>
#include <atomic>
#include <bit>
#include <cstdint>
#include <mutex>

namespace hyos_snapshot {
constexpr int32_t kTransaction = 0x00484353;
constexpr int32_t kAck = 0x48435331;
constexpr size_t kWordCount = 47;
constexpr int32_t kByteCount = 188;
constexpr int64_t kMaxLeaseNanos = 5'000'000'000LL;
constexpr uint32_t kAllowed = 1;
constexpr uint32_t kKnownFlags = 63;
using Words = std::array<int32_t, kWordCount>;
static_assert(HC_LAYOUT_KNOB_COUNT == 8);
static_assert(std::atomic<int32_t>::is_always_lock_free);
static_assert(std::atomic<uint64_t>::is_always_lock_free);
static_assert(std::endian::native == std::endian::little);

struct Snapshot {
    bool available = false;
    uint32_t flags = 0;
    int64_t epoch = 0;
    int64_t revision = 0;
    int64_t valid_until = 0;
    home_layout::Config config{};
};

inline home_layout::Config neutral_config() {
    home_layout::Config config{};
    config.cell_x = 4;
    config.cell_y = 6;
    return config;
}

inline int64_t long_at(const Words &words, size_t index) {
    const auto bits = static_cast<uint64_t>(static_cast<uint32_t>(words[index]))
        | (static_cast<uint64_t>(static_cast<uint32_t>(words[index + 1])) << 32);
    return std::bit_cast<int64_t>(bits);
}

inline Words unavailable_words() {
    Words words{};
    words[0] = kAck;
    words[12] = HC_LAYOUT_KNOB_COUNT;
    words[29] = 17;
    return words;
}

inline bool valid_flags(int32_t flags) {
    return flags >= 0 && (static_cast<uint32_t>(flags) & ~kKnownFlags) == 0
        && ((flags & kAllowed) != 0 || flags == 0)
        && ((flags & 8) == 0 || (flags & 16) != 0);
}

// Fixed HCS1 Parcel layout: 3 ints, 3 longs, grid/count, 8 knob pairs,
// tweak count, 17 tweak ints. No optional tail or legacy-format fallback.
// The server's BOOTTIME deadline is retained; receipt never renews its lease.
inline bool decode(const Words &words, int64_t now, Snapshot &result) {
    if (now <= 0 || words[0] != kAck || (words[1] != 0 && words[1] != 1)
        || !valid_flags(words[2]) || words[12] != HC_LAYOUT_KNOB_COUNT
        || words[29] != 17) return false;
    Snapshot candidate;
    candidate.available = words[1] == 1;
    candidate.flags = static_cast<uint32_t>(words[2]);
    candidate.epoch = long_at(words, 3);
    candidate.revision = long_at(words, 5);
    candidate.valid_until = long_at(words, 7);
    candidate.config = neutral_config();
    const bool positive_identity = candidate.epoch > 0 && candidate.revision > 0;
    const bool empty_identity = candidate.epoch == 0 && candidate.revision == 0;
    if (!candidate.available || candidate.flags == 0) {
        if (candidate.flags != 0 || candidate.valid_until != 0
            || (candidate.available ? !positive_identity : !(positive_identity || empty_identity)))
            return false;
        for (size_t index = 9; index < words.size(); ++index) {
            if (index != 12 && index != 29 && words[index] != 0) return false;
        }
        result = candidate;
        return true;
    }
    if (!positive_identity || candidate.valid_until <= now
        || candidate.valid_until - now > kMaxLeaseNanos) return false;
    const auto flag = [](int32_t value) { return value == 0 || value == 1; };
    const auto within = [](int32_t value, int32_t low, int32_t high) {
        return value >= low && value <= high;
    };
    if (!flag(words[9]) || !within(words[10], 3, 9) || !within(words[11], 4, 13))
        return false;
    candidate.config.grid_enabled = words[9] != 0;
    candidate.config.cell_x = words[10];
    candidate.config.cell_y = words[11];
    for (size_t index = 0; index < HC_LAYOUT_KNOB_COUNT; ++index) {
        const auto enabled = words[13 + index * 2];
        const auto delta = words[14 + index * 2];
        if (!flag(enabled) || !within(delta, -2400, 2400)) return false;
        // A disabled slider's stored value must not become an active startup delta.
        candidate.config.knobs[index] = {enabled != 0, enabled != 0 ? delta : 0};
    }
    const int32_t *tweaks = words.data() + 30;
    if (!within(tweaks[0], 1, 16) || !flag(tweaks[1])
        || !within(tweaks[2], 2, 16) || !within(tweaks[3], 2, 16) || !flag(tweaks[4])
        || !within(tweaks[5], 2, 16) || !within(tweaks[6], 2, 16) || !flag(tweaks[7])
        || !within(tweaks[8], 0, 255) || !flag(tweaks[9]) || !flag(tweaks[10])
        || !flag(tweaks[11]) || !flag(tweaks[12]) || !within(tweaks[13], 30, 200)
        || !flag(tweaks[14]) || !within(tweaks[15], 30, 200) || !flag(tweaks[16]))
        return false;
    candidate.config.tweaks = {
        tweaks[1] != 0, tweaks[0], tweaks[4] != 0, tweaks[2], tweaks[3],
        tweaks[7] != 0, tweaks[5], tweaks[6], tweaks[9] != 0, tweaks[8],
        tweaks[10] != 0, tweaks[11] != 0, tweaks[12] != 0, tweaks[13],
        tweaks[14] != 0, tweaks[15], tweaks[16] != 0};
    result = candidate;
    return true;
}

// All payload words are atomic: retrying a seqlock around ordinary Config
// memory would still constitute a C++ data race. SC operations make the
// ordering argument explicit: reading a new payload word orders the writer's
// odd sequence before the reader's final sequence read. Readers never take the
// writer mutex and make at most three attempts, including during revocation.
class Cache {
public:
    bool publish(const Words &words, int64_t now) {
        Snapshot checked;
        bool valid = decode(words, now, checked);
        std::lock_guard lock(writer_mutex_);
        if (valid && checked.epoch > 0) {
            // Epoch is an opaque generation token, not an ordered timestamp.
            // A temporary outage does not erase the current epoch's high-water
            // mark and thereby permit an older in-flight publication to return.
            if (checked.epoch == last_epoch_ && checked.revision < last_revision_)
                valid = false;
            if (valid) {
                last_epoch_ = checked.epoch;
                last_revision_ = checked.revision;
            }
        }
        store_locked(valid ? words : unavailable_words());
        return valid;
    }

    void revoke() {
        std::lock_guard lock(writer_mutex_);
        store_locked(unavailable_words());
    }

    bool load_snapshot(int64_t now, Snapshot &result) const {
        Words words{};
        for (int attempt = 0; attempt < 3; ++attempt) {
            const auto before = sequence_.load();
            if ((before & 1) != 0) continue;
            for (size_t index = 0; index < words.size(); ++index)
                words[index] = words_[index].load();
            if (sequence_.load() == before) return decode(words, now, result);
        }
        return false;
    }

    bool authorization(int64_t now, uint32_t &flags) const {
        if (now <= 0) return false;
        for (int attempt = 0; attempt < 3; ++attempt) {
            const auto before = sequence_.load();
            if ((before & 1) != 0) continue;
            const auto candidate = words_[2].load();
            const auto low = static_cast<uint32_t>(words_[7].load());
            const auto high = static_cast<uint32_t>(words_[8].load());
            const auto deadline = std::bit_cast<int64_t>(static_cast<uint64_t>(low)
                | (static_cast<uint64_t>(high) << 32));
            if (sequence_.load() != before) continue;
            if ((candidate & kAllowed) == 0 || deadline <= now
                || deadline - now > kMaxLeaseNanos) return false;
            flags = static_cast<uint32_t>(candidate);
            return true;
        }
        return false;
    }

private:
    void store_locked(const Words &words) {
        sequence_.fetch_add(1);
        for (size_t index = 0; index < words.size(); ++index)
            words_[index].store(words[index]);
        sequence_.fetch_add(1);
    }

    std::mutex writer_mutex_;
    int64_t last_epoch_ = 0;
    int64_t last_revision_ = 0;
    std::atomic<uint64_t> sequence_{0};
    std::array<std::atomic<int32_t>, kWordCount> words_{};
};
} // namespace hyos_snapshot
