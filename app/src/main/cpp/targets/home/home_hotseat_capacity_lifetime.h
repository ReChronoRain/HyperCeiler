/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "nativehook/source_word_patch.h"

namespace home_layout {
enum class CapacitySyncStatus { ready, invalid_bank, page_guard_failed, transaction_failed };
struct CapacitySyncResult {
    CapacitySyncStatus status = CapacitySyncStatus::invalid_bank;
    unsigned restored = 0;
    bool ready() const { return status == CapacitySyncStatus::ready; }
};

// Same bank for startup, preference changes and health repair. Register every
// page BEFORE publishing any word. Healthy passes perform bounded reads only;
// mapping/protection checks and retained rollback journals belong to NHK writes.
template<size_t N, class Protect, class Factory>
CapacitySyncResult maintain_capacity_words(std::array<nhk::SourceWordPatch,N>& words,
    bool enabled, bool& pages_protected, Protect protect, Factory factory) {
    for (size_t i=0;i<N;++i) {
        if (!nhk::valid_source_word(words[i])) return {};
        for (size_t j=0;j<i;++j) if (words[i].address==words[j].address) return {};
    }
    // Recovery obligations outlive registration failures and setting changes.
    if (!nhk::settle_source_words(words,factory))
        return {CapacitySyncStatus::transaction_failed};
    if (enabled && !pages_protected) {
        for (const auto& word:words)
            if (!protect(word.address,4)) return {CapacitySyncStatus::page_guard_failed};
        pages_protected=true;
    }
    unsigned lost=0;
    for (const auto& word:words) {
        if (!enabled || !word.owned || word.ownedWord!=word.replacement || word.pending()) continue;
        auto ops=factory(word); uint32_t live=0;
        if (ops.Read(word.address,live) && live==word.original) ++lost;
    }
    if (!nhk::apply_source_words(words,enabled,factory))
        return {CapacitySyncStatus::transaction_failed};
    return {CapacitySyncStatus::ready,lost};
}
} // namespace home_layout
