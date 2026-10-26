/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "code_word_write.h"
#include "native_image.h"
#include <array>

namespace nhk {
// Persistent ownership and two independent recovery obligations, retained on failure.
// A bank is not an atomic multi-instruction publication or a mapping-generation lease.
struct SourceWordPatch {
    uintptr_t address = 0;
    uint32_t original = 0, replacement = 0;
    CodeSource source{};
    uint32_t ownedWord = 0;
    bool owned = false;
    WordWriteJournal recovery{};
    uint32_t rollbackWord = 0;
    bool rollbackPending = false;
    bool pending() const { return rollbackPending || recovery.pending(); }
};

inline bool valid_source_word(const SourceWordPatch& word) {
    return word.address && !(word.address & 3u) && word.address <= UINTPTR_MAX - 4
        && word.source.inode && word.source.file_offset <= UINT64_MAX - 4
        && word.original != word.replacement;
}

template<class Ops> bool source_word_clean(Ops& ops, const SourceWordPatch& site, uint32_t wanted) {
    const long size = ops.PageSize();
    if (!valid_source_word(site) || size < 4 || (uint64_t(size) & (uint64_t(size)-1))) return false;
    const uintptr_t page = site.address & ~(uintptr_t(size)-1);
    if (page > UINTPTR_MAX-uintptr_t(size)) return false;
    CodeWriteGuard lock;
    if (!lock) return false;
    CodePage before{},after{}; uint32_t current = 0;
    return ops.Page(page,size_t(size),before) && before.protection == 5 && before.privateMap
        && ops.Accept(before) && ops.Read(site.address,current) && current == wanted
        && ops.Page(page,size_t(size),after) && SameCodePage(before,after)
        && after.protection == 5 && after.privateMap && ops.Accept(after);
}

template<class Ops> bool settle_source_word(SourceWordPatch& site, Ops& ops) {
    if (!site.pending()) return true;
    CodeWriteGuard lock;
    if (!lock) return false;
    if (!valid_source_word(site) || !RecoverCodeWord(ops,site.recovery)) return false;
    if (!site.rollbackPending) return true;
    uint32_t current = 0;
    if (!ops.Read(site.address,current)) return false;
    if (current != site.rollbackWord) {
        // A foreign instruction is never overwritten by a bank's rollback.
        if (!site.owned || current != site.ownedWord) return false;
        const auto result = WriteCodeWord(ops,site.address,current,site.rollbackWord,site.rollbackWord);
        site.recovery = result.recovery;
        if (site.recovery.pending()) return false;
    }
    // Original bytes alone do not prove RX recovery. Verify both before retiring the intent.
    if (!source_word_clean(ops,site,site.rollbackWord)) return false;
    site.ownedWord = site.rollbackWord; site.owned = true;
    site.rollbackPending = false; return true;
}

template<size_t N, class Factory> bool settle_source_words(std::array<SourceWordPatch,N>& sites,
    Factory factory) {
    bool settled = true;
    for (auto& site : sites) {
        if (!site.pending()) continue;
        auto ops = factory(site);
        if (!settle_source_word(site,ops)) settled = false;
    }
    return settled;
}

template<size_t N, class Factory> bool apply_source_words(std::array<SourceWordPatch,N>& sites,
    bool enabled, Factory factory) {
    // Whole-bank shape preflight precedes any IO, recovery or mutation.
    for (size_t i = 0; i < N; ++i) {
        if (!valid_source_word(sites[i])) return false;
        for (size_t j = 0; j < i; ++j) if (sites[i].address == sites[j].address) return false;
    }
    if (!settle_source_words(sites,factory)) return false;
    std::array<uint32_t,N> before{};
    for (size_t i = 0; i < N; ++i) {
        auto ops = factory(sites[i]);
        if (!ops.Read(sites[i].address,before[i]) || (before[i] != sites[i].original
            && (!sites[i].owned || before[i] != sites[i].ownedWord))) return false;
    }
    for (size_t i = 0; i < N; ++i) {
        auto& site = sites[i]; const uint32_t wanted = enabled ? site.replacement : site.original;
        if (before[i] == wanted) continue;
        // Record the previous bank state BEFORE even a potentially failing syscall.
        site.rollbackWord = before[i]; site.rollbackPending = true;
        auto ops = factory(site);
        const auto result = WriteCodeWord(ops,site.address,before[i],wanted,before[i]);
        site.recovery = result.recovery;
        if (!result.complete) {
            (void)settle_source_words(sites,factory); return false;
        }
        site.ownedWord = wanted; site.owned = true;
    }
    for (auto& site : sites) {
        auto ops = factory(site); uint32_t current = 0;
        if (!ops.Read(site.address,current) || current != (enabled ? site.replacement : site.original)) {
            (void)settle_source_words(sites,factory); return false;
        }
    }
    for (auto& site : sites) site.rollbackPending = false;
    return true;
}
template<size_t N> bool source_words_owned(const std::array<SourceWordPatch,N>& sites) {
    for (const auto& site : sites) if (site.owned || site.pending()) return true;
    return false;
}
} // namespace nhk
