/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once

#include <array>
#include <atomic>
#include <bit>
#include <cstdint>
#include <mutex>

namespace weather_snapshot {
constexpr int32_t kTransaction = 0x00485753;
constexpr int32_t kAck = 0x48575331;
constexpr size_t kWordCount = 12;
constexpr int32_t kByteCount = 48;
constexpr int64_t kMaxLeaseNanos = 5'000'000'000LL;
using Words = std::array<int32_t, kWordCount>;
static_assert(std::endian::native == std::endian::little);
static_assert(std::atomic<int32_t>::is_always_lock_free);
static_assert(std::atomic<uint64_t>::is_always_lock_free);

struct Snapshot {
    bool available = false;
    bool allowed = false;
    int64_t epoch = 0;
    int64_t revision = 0;
    int64_t valid_until = 0;
    int32_t device_level = 3; // UI values: 0 low, 1 middle, 2 high, 3 default.
    int32_t card_type = 0;
    bool unlock_blur = false;
};

inline Words unavailable_words() {
    Words value{};
    value[0] = kAck;
    value[9] = 3;
    return value;
}

inline int64_t long_at(const Words &words, size_t index) {
    return std::bit_cast<int64_t>(static_cast<uint64_t>(static_cast<uint32_t>(words[index]))
        | (static_cast<uint64_t>(static_cast<uint32_t>(words[index + 1])) << 32));
}

// HWS1 is independent of Home HCS1. The complete Java reply is exactly 48
// bytes; the transport must check its size before supplying these twelve words.
inline bool decode(const Words &words, int64_t now, Snapshot &result) {
    if (now <= 0 || words[0] != kAck || (words[1] != 0 && words[1] != 1)
        || (words[2] != 0 && words[2] != 1)) return false;
    Snapshot candidate;
    candidate.available = words[1] == 1;
    candidate.allowed = words[2] == 1;
    candidate.epoch = long_at(words, 3);
    candidate.revision = long_at(words, 5);
    candidate.valid_until = long_at(words, 7);
    const bool positive = candidate.epoch > 0 && candidate.revision > 0;
    const bool empty = candidate.epoch == 0 && candidate.revision == 0;
    if (!candidate.available || !candidate.allowed) {
        if (candidate.allowed || candidate.valid_until != 0
            || (candidate.available ? !positive : !(positive || empty))
            || words[9] != 3 || words[10] != 0 || words[11] != 0) return false;
    } else {
        if (!positive || candidate.valid_until <= now
            || candidate.valid_until - now > kMaxLeaseNanos
            || words[9] < 0 || words[9] > 3
            || (words[10] != 0 && words[10] != 2 && words[10] != 3)
            || (words[11] != 0 && words[11] != 1)) return false;
        candidate.device_level = words[9];
        candidate.card_type = words[10];
        candidate.unlock_blur = words[11] == 1;
    }
    result = candidate;
    return true;
}

// Single transport poller; all words are atomic to avoid a C++ data race.
// Readers use a bounded SC sequence check and never enter the writer mutex.
// BOOTTIME is checked on consumption; receiving a frame cannot renew a lease.
class Cache {
public:
    bool publish(const Words &words, int64_t now) {
        Snapshot candidate;
        bool valid = decode(words, now, candidate);
        std::lock_guard lock(writer_mutex_);
        if (valid && candidate.epoch > 0) {
            if (candidate.epoch == last_epoch_ && candidate.revision < last_revision_)
                valid = false;
            if (valid) {
                last_epoch_ = candidate.epoch;
                last_revision_ = candidate.revision;
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

private:
    void store_locked(const Words &words) {
        sequence_.fetch_add(1);
        for (size_t index = 0; index < words.size(); ++index) words_[index].store(words[index]);
        sequence_.fetch_add(1);
    }

    std::mutex writer_mutex_;
    int64_t last_epoch_ = 0;
    int64_t last_revision_ = 0;
    std::atomic<uint64_t> sequence_{0};
    std::array<std::atomic<int32_t>, kWordCount> words_{};
};
} // namespace weather_snapshot
