/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * NativeHookRuntime resolver contract.
 *
 * A resolver answers exactly one question: *where is the target, and why do
 * you believe that*. It never installs, uninstalls or mprotects anything -
 * that is the runtime/backend's job (nativehook/hook_bank.h,
 * got_hook_backend.h). Splitting the two is what lets one target's resolver
 * be replaced without touching the lifecycle and vice versa.
 *
 * Failure discipline: zero candidates is a failure; one candidate may proceed
 * to validation; several candidates fail unless the concrete resolver brings
 * further disambiguating evidence. `candidates.front()` style selection is a
 * review-rejected pattern.
 */
#pragma once

#include "arm64_decode.h"

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

namespace nhk {

enum class TargetKind {
    kInline,  // Patch the instructions at `address` (inline/trampoline hook).
    kGotSlot, // Replace the pointer stored at `got_rva` (caller-specific PLT/GOT hook).
};

/** One resolved hook point plus the evidence that justifies it. */
struct ResolvedTarget {
    TargetKind kind = TargetKind::kInline;
    // Image-relative virtual address of the instruction stream (kInline) or
    // of the GOT slot (kGotSlot). RVAs survive generation remapping; backends
    // translate through NativeImage::runtime().
    uint64_t rva = 0;
    // For kInline: the original words captured from the same snapshot the
    // resolution was made on. A backend may only install when live memory
    // still matches these words.
    std::vector<uint32_t> original_words;
    // For kGotSlot: symbol name and original pointer (filled by the caller
    // after reading the live slot through a fault-reporting read).
    std::string symbol;
};

/**
 * Verifiable justification for a [ResolvedTarget]. Evidence is deliberately
 * dumb data: it can be logged, unit-tested and diffed without a device.
 */
struct ResolverEvidence {
    size_t candidate_count = 0;
    // Named imports the target provably calls (symbol names resolved through
    // the image's dynamic tables), e.g. a known libc/Rust FFI import.
    std::vector<std::string> import_anchors;
    // Addresses (RVA) of call sites inside the target, in program order, for
    // call-ordering evidence.
    std::vector<uint64_t> call_sites;
    // Free-form stage identifier for diagnostics ("shape", "unique", ...).
    std::string stage;
    // What ruled the other candidates out, for the log. Informational only:
    // it does NOT make a multi-candidate result acceptable, because a string
    // (however structured) is not a proof.
    std::vector<std::string> disambiguators;
};

/**
 * Contract helper: does this evidence justify proceeding?
 *
 * Only exactly one candidate qualifies. A resolver that starts from several
 * candidates must converge to one **by itself** - narrowing the search with
 * further evidence it can actually verify - and hand the runtime the single
 * result. `candidate_count == 0` is a failure, and `>= 2` is a failure no
 * matter what the resolver writes in `stage` or `disambiguators`: a diagnostic
 * note is not a proof, and this is what stops a future consumer from
 * installing on an unresolved ambiguity.
 */
inline bool evidence_acceptable(const ResolverEvidence &evidence) {
    return evidence.candidate_count == 1;
}

} // namespace nhk
