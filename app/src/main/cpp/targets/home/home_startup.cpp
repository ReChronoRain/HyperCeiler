/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "home_startup.h"
#include "home_startup_plan.generated.h"
#include <atomic>
#include <array>
#include <cstring>
#include <limits>
#include <sys/mman.h>
#include <sys/uio.h>
#include <unistd.h>

extern "C" {
// Even = denied. Every revocation advances the epoch and clears bit 0 in one
// atomic operation. A writer can only activate the exact epoch it acquired.
alignas(8) uint64_t hc_layout_startup_epoch_gate = 0;
}
#ifdef HOME_STARTUP_TEST
extern bool home_startup_test_read(uintptr_t, void *, size_t) noexcept;
extern bool home_startup_test_write(uintptr_t, const void *, size_t) noexcept;
extern int home_startup_test_protect(uintptr_t, size_t, int) noexcept;
#endif
namespace home_startup {
namespace {
// Phone, Pad and Fold grids also feed the Rust GridConfig/model. Do not create
// a Dart/Rust disagreement while that first-use deadline remains unproven.
// Keep their generated Dart plans as evidence but decline the composite grids.
constexpr uint64_t kUnsupported = PhoneGridDart | PhoneGridRust | PadGrid | FoldGrid | RecentsAnimationRust;
constexpr size_t kMaxWords = 64;
constexpr size_t kHookCount = std::size(plan::kHooks);
static_assert(kHookCount == 15);
struct WordState {
    uint32_t va = 0, original = 0, current = 0, desired = 0, attempted = 0;
};
struct HookState {
    std::array<uint32_t, 4> original{}, patch{};
    bool registered = false, patch_known = false;
};
struct State {
    uintptr_t base = 0;
    HookFunction hook = nullptr;
    UnhookFunction unhook = nullptr;
    WordState words[kMaxWords]{};
    size_t word_count = 0;
    struct Page { uintptr_t begin = 0; bool protection_pending = false; } pages[kMaxWords]{};
    size_t page_count = 0;
    HookState hooks[kHookCount]{};
    uint64_t eligible = 0;
    bool attempted = false, installed = false, terminal = false, ever_activated = false;
    uint64_t serviced_epoch = 0;
    Result result{};
};
State g_state;
std::atomic_flag g_writer = ATOMIC_FLAG_INIT;
std::atomic<pid_t> g_prepared_pid{0};
size_t g_page_size = 0;

struct Writer {
    bool owns = !g_writer.test_and_set(std::memory_order_acquire);
    ~Writer() { if (owns) g_writer.clear(std::memory_order_release); }
};
uint64_t epoch() noexcept { return __atomic_load_n(&hc_layout_startup_epoch_gate, __ATOMIC_ACQUIRE); }
bool live(uint64_t expected, AuthorizationCheck check, void *context) noexcept {
    const auto current = epoch();
    return expected < UINT64_MAX - 1 && current < UINT64_MAX - 1
        && (current & ~uint64_t{1}) == (expected & ~uint64_t{1})
        && check != nullptr && check(context);
}
bool activate(uint64_t expected) noexcept {
    if (expected >= UINT64_MAX - 1) return false;
    const uint64_t desired = expected | 1;
    return __atomic_compare_exchange_n(&hc_layout_startup_epoch_gate, &expected, desired,
        false, __ATOMIC_RELEASE, __ATOMIC_ACQUIRE);
}
bool read_memory(uintptr_t address, void *out, size_t size) noexcept {
    if (!address || !size || address > UINTPTR_MAX - size) return false;
#ifdef HOME_STARTUP_TEST
    return home_startup_test_read(address, out, size);
#else
    iovec local{out, size}, remote{reinterpret_cast<void *>(address), size};
    // One syscall only: no EINTR retry loop, signal handler or /proc fallback.
    return process_vm_readv(getpid(), &local, 1, &remote, 1, 0) == static_cast<ssize_t>(size);
#endif
}
int protect_memory(uintptr_t begin, size_t length, int protection) noexcept {
#ifdef HOME_STARTUP_TEST
    return home_startup_test_protect(begin, length, protection);
#else
    return mprotect(reinterpret_cast<void *>(begin), length, protection);
#endif
}
bool restore_protections() noexcept {
    bool okay = true;
    for (size_t i = 0; i < g_state.page_count; ++i) {
        auto &page = g_state.pages[i];
        if (!page.protection_pending) continue;
        if (protect_memory(page.begin, g_page_size, PROT_READ | PROT_EXEC) == 0)
            page.protection_pending = false;
        else okay = false;
    }
    return okay;
}
bool write_memory(uintptr_t address, const void *data, size_t size) noexcept {
    // Every direct recipe changes exactly one aligned AArch64 instruction.
    // A 32-bit atomic store is not a transaction across a multi-site group.
    if (!address || size != 4 || (address & 3) || address > UINTPTR_MAX - size || !g_page_size) return false;
    const uintptr_t begin = address & ~(uintptr_t(g_page_size) - 1);
    State::Page *page = nullptr;
    for (size_t i = 0; i < g_state.page_count; ++i)
        if (g_state.pages[i].begin == begin) { page = &g_state.pages[i]; break; }
    if (!page) {
        if (g_state.page_count == kMaxWords) return false;
        page = &g_state.pages[g_state.page_count++]; page->begin = begin;
    }
    if (protect_memory(begin, g_page_size, PROT_READ | PROT_WRITE | PROT_EXEC)) return false;
    page->protection_pending = true;
#ifdef HOME_STARTUP_TEST
    const bool wrote = home_startup_test_write(address, data, size);
#else
    uint32_t word = 0; std::memcpy(&word, data, sizeof(word));
    __atomic_store_n(reinterpret_cast<uint32_t *>(address), word, __ATOMIC_RELAXED);
    __builtin___clear_cache(reinterpret_cast<char *>(address), reinterpret_cast<char *>(address + size));
    const bool wrote = true;
#endif
    const bool restored = protect_memory(begin, g_page_size, PROT_READ | PROT_EXEC) == 0;
    if (restored) page->protection_pending = false;
    return wrote && restored;
}
bool address(uint32_t va, size_t size, uintptr_t &out, bool executable = true) noexcept {
    if (!g_state.base || g_state.base > UINTPTR_MAX - va
        || g_state.base + va > UINTPTR_MAX - size) return false;
    for (const auto &segment : plan::kSegments) {
        if (va >= segment.va && uint64_t(va) + size <= uint64_t(segment.va) + segment.file_size
            && (segment.flags & 4) && (!executable || (segment.flags & 1))) {
            out = g_state.base + va; return true;
        }
    }
    return false;
}
bool compare(uint32_t va, const uint8_t *expected, size_t size, bool executable = true) noexcept {
    uint8_t buffer[512];
    uintptr_t at = 0;
    if (!address(va, size, at, executable)) return false;
    for (size_t done = 0; done < size;) {
        const size_t count = size - done < sizeof(buffer) ? size - done : sizeof(buffer);
        if (!read_memory(at + done, buffer, count) || std::memcmp(buffer, expected + done, count)) return false;
        done += count;
    }
    return true;
}
bool guards() noexcept {
    if (!compare(0, plan::kIdentity, sizeof(plan::kIdentity), false)) return false;
    for (const auto &guard : plan::kGuards)
        if (!compare(guard.va, plan::kGuardBytes + guard.offset, guard.size)) return false;
    return true;
}
bool detour_guards() noexcept {
    // Enforce our 16-byte bank contract after every backend call instead of
    // assuming the external hook API promises a maximum overwrite length.
    // All other guarded bytes, including adjacent folder splices, must remain
    // pristine. A longer/foreign patch is rejected and handed back to unhook.
    uint8_t expected[512], observed[512];
    for (const auto &guard : plan::kGuards) for (size_t done = 0; done < guard.size;) {
        const size_t count = guard.size - done < sizeof(expected) ? guard.size - done : sizeof(expected);
        const uint32_t begin = guard.va + done, end = begin + count;
        std::memcpy(expected, plan::kGuardBytes + guard.offset + done, count);
        for (size_t i = 0; i < kHookCount; ++i) {
            const auto &state = g_state.hooks[i];
            const uint32_t hook_begin = plan::kHooks[i].va, hook_end = hook_begin + 16;
            if (!state.registered || !state.patch_known || hook_begin >= end || hook_end <= begin) continue;
            const uint32_t left = hook_begin > begin ? hook_begin : begin;
            const uint32_t right = hook_end < end ? hook_end : end;
            std::memcpy(expected + left - begin,
                reinterpret_cast<const uint8_t *>(state.patch.data()) + left - hook_begin, right - left);
        }
        uintptr_t at = 0;
        if (!address(begin, count, at) || !read_memory(at, observed, count)
            || std::memcmp(observed, expected, count)) return false;
        done += count;
    }
    return true;
}
bool add_word(const plan::Word &word) noexcept {
    for (size_t i = 0; i < g_state.word_count; ++i)
        if (g_state.words[i].va == word.va) return g_state.words[i].original == word.original;
    if (g_state.word_count == kMaxWords || (word.va & 3)) return false;
    g_state.words[g_state.word_count++] = {word.va, word.original, word.original,
        word.original, word.original};
    return true;
}
uint64_t requested(const home_layout::Config &config) noexcept {
    uint64_t mask = 0;
    if (config.grid_enabled) mask |= PhoneGridDart | PhoneGridRust;
    const auto &t = config.tweaks;
    if (t.folder_enabled) mask |= FolderColumns;
    if (t.pad_enabled) mask |= PadGrid;
    if (t.fold_enabled) mask |= FoldGrid;
    if (t.icon_scale_enabled) mask |= IconScale;
    if (t.recents_no_clear) mask |= RemoveClear;
    if (t.recents_hide_clear && !t.recents_no_clear) mask |= HideClear;
    if (config.knobs[0].enabled) mask |= HotseatMargin;
    if (config.knobs[1].enabled) mask |= FolderSpacing;
    if (config.knobs[2].enabled || config.knobs[3].enabled || config.knobs[4].enabled)
        mask |= Workspace | FolderGeometry;
    if (config.knobs[5].enabled) mask |= CapsuleMargin;
    if (config.knobs[6].enabled || config.knobs[7].enabled) mask |= IndicatorPolicy;
    if (t.hotseat_unlimited) mask |= HotseatCapacity;
    if (t.animation_open_enabled) mask |= OpenAnimation;
    if (t.animation_recents_enabled) mask |= RecentsAnimationRust;
    return mask;
}
bool valid_config(const home_layout::Config &config) noexcept {
    for (const auto &knob : config.knobs)
        if (knob.delta_dp < -2400 || knob.delta_dp > 2400 || (!knob.enabled && knob.delta_dp != 0)) return false;
    if (config.knobs[6].enabled && (config.knobs[6].delta_dp < 0 || config.knobs[6].delta_dp > 2)) return false;
    const auto &t = config.tweaks;
    if (t.animation_open_rate_percent < 30 || t.animation_open_rate_percent > 200
        || t.animation_recents_rate_percent < 30 || t.animation_recents_rate_percent > 200) return false;
    return true;
}
bool set_desired(const plan::Word &word) noexcept {
    for (size_t i = 0; i < g_state.word_count; ++i) {
        auto &state = g_state.words[i];
        if (state.va != word.va) continue;
        if (state.original != word.original || (state.desired != state.original
            && state.desired != word.replacement)) return false;
        state.desired = word.replacement; return true;
    }
    return false;
}
bool make_plan(const home_layout::Config &config, uint64_t mask) noexcept {
    for (size_t i = 0; i < g_state.word_count; ++i) g_state.words[i].desired = g_state.words[i].original;
    const auto &t = config.tweaks;
    const uint32_t keys[] = {
        (uint32_t(config.cell_x) << 8) | uint32_t(config.cell_y), uint32_t(t.folder_cols),
        (uint32_t(t.pad_major) << 8) | uint32_t(t.pad_minor),
        (uint32_t(t.fold_major) << 8) | uint32_t(t.fold_minor), uint32_t(t.icon_scale_code), 1, 1,
    };
    for (unsigned family = 0; family < 7; ++family) {
        if (!(mask & (uint64_t{1} << family))) continue;
        const plan::Variant *found = nullptr;
        for (const auto &variant : plan::kVariants)
            if (variant.family == family && variant.key == keys[family]) { found = &variant; break; }
        if (found == nullptr) return false;
        for (size_t i = 0; i < found->count; ++i)
            if (!set_desired(plan::kWords[found->begin + i])) return false;
    }
    if (mask & HotseatCapacity)
        for (const auto &word : plan::kCapacity) if (!set_desired(word)) return false;
    return true;
}
bool read_word(WordState &word, uint32_t &value) noexcept {
    uintptr_t at = 0;
    return address(word.va, 4, at) && read_memory(at, &value, 4);
}
// The persistent journal includes attempts whose final protection restore or
// readback failed. Never erase a record on an unverified restoration.
struct Permit { uint64_t generation; AuthorizationCheck check; void *context; };
Error write_words(bool restore, const Permit *permit = nullptr) noexcept {
    Error error = Error::None;
    for (size_t i = 0; i < g_state.word_count; ++i) {
        auto &word = g_state.words[i]; uint32_t current = 0;
        if (!read_word(word, current)) { error = Error::ReadbackFailed; if (!restore) return error; continue; }
        if (current != word.current && current != word.original && current != word.attempted) {
            error = Error::ForeignCode; if (!restore) return error; continue;
        }
        word.current = current;
        const uint32_t desired = restore ? word.original : word.desired;
        if (current == desired) continue;
        if (!restore && permit && !live(permit->generation, permit->check, permit->context))
            return Error::Revoked;
        uintptr_t at = 0;
        if (!address(word.va, 4, at)) { error = Error::InvalidRequest; if (!restore) return error; continue; }
        word.attempted = desired;
        const bool wrote = write_memory(at, &desired, 4);
        const bool read = read_word(word, current);
        if (read && (current == word.current || current == word.original || current == desired))
            word.current = current;
        else if (read) {
            // Do not adopt an unexplained readback into the ownership journal:
            // doing so would authorize a later rollback over somebody else's code.
            error = Error::ForeignCode;
            if (!restore) return error;
            continue;
        }
        if (!wrote || !read || current != desired) {
            error = !wrote ? Error::WriteFailed : Error::ReadbackFailed;
            if (!restore) return error;
        }
    }
    if (!restore_protections()) error = Error::WriteFailed;
    return error;
}
bool rollback_hooks() noexcept {
    bool okay = true;
    for (size_t i = kHookCount; i-- > 0;) {
        auto &state = g_state.hooks[i]; if (!state.registered) continue;
        const auto &spec = plan::kHooks[i]; uintptr_t at = 0;
        std::array<uint32_t, 4> observed{};
        if (!address(spec.va, sizeof(observed), at) || !read_memory(at, observed.data(), sizeof(observed))
            || (observed != state.original && (!state.patch_known || observed != state.patch))) {
            okay = false; continue;
        }
        // The backend owns allocation/unwind records. Do not free its original
        // by erasing a pointer or writing a prologue behind a failed unhook.
        const int removed = g_state.unhook ? g_state.unhook(reinterpret_cast<void *>(at)) : -1;
        if (removed != 0 || !read_memory(at, observed.data(), sizeof(observed)) || observed != state.original) {
            okay = false; continue;
        }
        state.registered = false; state.patch_known = false;
        const auto binding = detail::binding(spec.index);
        if (binding.original) *binding.original = nullptr;
    }
    return okay;
}
Error install_hooks(uint64_t mask, const Permit &permit) noexcept {
    for (size_t i = 0; i < kHookCount; ++i) {
        const auto &spec = plan::kHooks[i]; if (!(mask & spec.feature)) continue;
        if (!live(permit.generation, permit.check, permit.context)) return Error::Revoked;
        auto &state = g_state.hooks[i]; const auto binding = detail::binding(spec.index);
        uintptr_t at = 0;
        if (!binding.replacement || !binding.original || *binding.original
            || !address(spec.va, 16, at) || !read_memory(at, state.original.data(), 16)) return Error::InvalidRequest;
        const int rc = g_state.hook(reinterpret_cast<void *>(at), binding.replacement, binding.original);
        const bool read = read_memory(at, state.patch.data(), 16);
        state.patch_known = read && state.patch != state.original;
        // Even a failed backend might have allocated or changed its target.
        // Retain ownership until a verified unhook restores the original.
        state.registered = rc == 0 || *binding.original != nullptr || !read || state.patch_known;
        if (rc != 0) return Error::HookFailed;
        if (*binding.original == nullptr) return Error::MissingContinuation;
        if (!read || !state.patch_known) return Error::ReadbackFailed;
        if (!detour_guards()) return Error::ForeignCode;
    }
    return Error::None;
}
Error verify_hooks() noexcept {
    for (size_t i = 0; i < kHookCount; ++i) {
        const auto &state = g_state.hooks[i];
        if (!state.registered) continue;
        const auto binding = detail::binding(plan::kHooks[i].index);
        uintptr_t at = 0;
        std::array<uint32_t, 4> observed{};
        if (!binding.original || !*binding.original) return Error::MissingContinuation;
        if (!address(plan::kHooks[i].va, 16, at) || !read_memory(at, observed.data(), 16))
            return Error::ReadbackFailed;
        // Do not reinstall a lost 16-byte detour after snapshot consumption.
        // A page refill or foreign edit is reported and gates are closed. This
        // preserves the one accepted installation window instead of silently
        // racing live Dart execution with a late multi-instruction memcpy.
        if (!state.patch_known || observed != state.patch) return Error::ForeignCode;
    }
    return Error::None;
}
Result failure(Error error, uint64_t mask, bool unhook) noexcept {
    request_revoke();
    const bool restored = write_words(true) == Error::None;
    // An opened gateway may still have callers in flight after revocation.
    // Keep its backend-owned continuation resident; closing the epoch gate
    // does not establish a quiescent point at which unhooking is safe.
    unhook = unhook && !g_state.ever_activated;
    const bool hooks = !unhook || rollback_hooks();
    // The backend's restoration cannot be inferred from its return code or
    // the first 16 bytes alone. Read the complete pinned guard regions again;
    // never overwrite an unexplained tail on behalf of another patch owner.
    const bool pristine = !unhook || (hooks && guards());
    if (!restored || !hooks || !pristine) { error = Error::RollbackFailed; g_state.terminal = true; }
    g_state.result = {g_state.terminal ? Outcome::TerminalFailure : Outcome::Declined, mask, 0, mask, error};
    return g_state.result;
}
Result busy() noexcept { return {Outcome::Declined, 0, 0, 0, Error::Busy}; }
} // namespace

bool prepare_for_child() noexcept {
    const pid_t self = getpid();
    if (g_prepared_pid.load(std::memory_order_acquire) == self) return true;
    // The controller must call this before exposing any child writer. Do not
    // reset a lock inherited from an active parent: the Hyos module starts no
    // workers or target installations in the spawner.
    Writer writer; if (!writer.owns) return false;
    g_state = State{};
    __atomic_store_n(&hc_layout_startup_epoch_gate, uint64_t{0}, __ATOMIC_RELEASE);
    detail::clear_consumers();
    const long page = sysconf(_SC_PAGESIZE);
    if (page <= 0 || page > 65536 || (page & (page - 1))) return false;
    g_page_size = static_cast<size_t>(page);
    for (const auto &word : plan::kWords) if (!add_word(word)) return false;
    for (const auto &word : plan::kCapacity) if (!add_word(word)) return false;
    g_prepared_pid.store(self, std::memory_order_release);
    return true;
}
void request_revoke() noexcept {
    uint64_t previous = epoch();
    for (;;) {
        // Saturation is terminal-denied; never recycle a generation on wrap.
        const uint64_t next = previous >= UINT64_MAX - 2 ? UINT64_MAX - 1
            : (previous & ~uint64_t{1}) + 2;
        if (__atomic_compare_exchange_n(&hc_layout_startup_epoch_gate, &previous, next,
                false, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) break;
    }
    detail::clear_consumers();
}
Result execute(const Request &request) noexcept {
    if (g_prepared_pid.load(std::memory_order_acquire) != getpid())
        return {Outcome::Declined, 0, 0, 0, Error::NotPrepared};
    Writer writer; if (!writer.owns) return busy();
    if (g_state.attempted) return {Outcome::Missed, 0, 0, 0, Error::LateConfiguration};
    g_state.attempted = true;
    if (!request.config || !request.dart_load_bias || !request.hook || !request.unhook
        || !request.still_authorized) return {Outcome::Declined, 0, 0, 0, Error::InvalidRequest};
    const auto config = *request.config;
    const uint64_t mask = requested(config), generation = epoch();
    if (!valid_config(config) || !make_plan(config, mask & ~kUnsupported))
        return {Outcome::Declined, mask, 0, mask, Error::UnsupportedConfiguration};
    if (!live(generation, request.still_authorized, request.context))
        return {Outcome::Declined, mask, 0, mask, Error::NotAuthorized};
    g_state.base = request.dart_load_bias; g_state.hook = request.hook; g_state.unhook = request.unhook;
    if (!guards()) return {Outcome::Declined, mask, 0, mask, Error::GuardMismatch};
    detail::initialize_consumers(g_state.base, plan::kIndicatorBranches[0], plan::kIndicatorBranches[1],
        plan::kIndicatorBranches[2], plan::kIndicatorBranches[3], plan::kFolderResume);
    if (!live(generation, request.still_authorized, request.context)) return failure(Error::Revoked, mask, false);
    const Permit permit{generation, request.still_authorized, request.context};
    const auto installed = install_hooks(mask & ~kUnsupported, permit);
    if (installed != Error::None) return failure(installed, mask, true);
    const auto written = write_words(false, &permit);
    if (written != Error::None) return failure(written, mask, true);
    if (!live(generation, request.still_authorized, request.context)) return failure(Error::Revoked, mask, true);
    detail::publish_consumers(config, mask & ~kUnsupported);
    if (!live(generation, request.still_authorized, request.context)) return failure(Error::Revoked, mask, true);
    if (!activate(generation)) return failure(Error::Revoked, mask, true);
    g_state.ever_activated = true;
    if (!live(generation, request.still_authorized, request.context)) return failure(Error::Revoked, mask, true);
    g_state.eligible = mask & ~kUnsupported; g_state.installed = true;
    g_state.serviced_epoch = generation & ~uint64_t{1};
    g_state.result = {Outcome::Applied, mask, mask & ~kUnsupported, mask & kUnsupported,
        (mask & kUnsupported) ? Error::EarlierRustDeadline : Error::None};
    return g_state.result;
}
Result service_revoke() noexcept {
    Writer writer; if (!writer.owns) return busy();
    const uint64_t generation = epoch();
    if ((generation & 1) || g_state.base == 0) return g_state.result;
    detail::clear_consumers();
    if (write_words(true) != Error::None) {
        g_state.terminal = true;
        g_state.result = {Outcome::TerminalFailure, g_state.result.requested, 0,
            g_state.result.requested, Error::RollbackFailed};
    } else {
        g_state.serviced_epoch = generation;
        g_state.result.applied = 0; g_state.result.declined = g_state.result.requested;
        g_state.result.outcome = g_state.terminal ? Outcome::TerminalFailure : Outcome::Declined;
        g_state.result.error = g_state.terminal ? Error::RollbackFailed : Error::Revoked;
    }
    return g_state.result;
}
Result maintenance(const home_layout::Config *value, bool authorized,
    AuthorizationCheck check, void *context) noexcept {
    if (!authorized || value == nullptr || check == nullptr) {
        request_revoke(); return service_revoke();
    }
    Writer writer; if (!writer.owns) return busy();
    const auto config = *value; const uint64_t mask = requested(config), generation = epoch();
    if (!g_state.installed || g_state.terminal)
        return {g_state.terminal ? Outcome::TerminalFailure : Outcome::Missed, mask, 0, mask,
            g_state.terminal ? Error::RollbackFailed : Error::LateConfiguration};
    const uint64_t supported = mask & g_state.eligible;
    if (!live(generation, check, context)) return failure(Error::Revoked, mask, false);
    if (!valid_config(config) || !make_plan(config, supported)) return failure(Error::UnsupportedConfiguration, mask, false);
    // First-use constants are never newly changed after snapshot consumption.
    // Disabling is a best-effort serialized restoration, not an atomic rewind
    // of in-flight Dart functions or values already cached in Dart objects.
    for (size_t i = 0; i < g_state.word_count; ++i) {
        const auto &word = g_state.words[i];
        if (word.desired != word.current && word.desired != word.original)
            return failure(Error::LateConfiguration, mask, false);
    }
    const auto healthy = verify_hooks();
    if (healthy != Error::None) return failure(healthy, mask, false);
    // Close authorization while writing a group, preserving the epoch. Revoke
    // races advance it, so neither this owner nor an older one can reopen it.
    uint64_t expected = generation;
    if (!__atomic_compare_exchange_n(&hc_layout_startup_epoch_gate, &expected, generation & ~uint64_t{1},
            false, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE)) return failure(Error::Revoked, mask, false);
    detail::clear_consumers();
    const Permit permit{generation, check, context};
    const auto written = write_words(false, &permit);
    if (written != Error::None) return failure(written, mask, false);
    if (!live(generation, check, context)) return failure(Error::Revoked, mask, false);
    detail::publish_consumers(config, supported);
    if (!live(generation, check, context)) return failure(Error::Revoked, mask, false);
    if (!activate(generation & ~uint64_t{1})) return failure(Error::Revoked, mask, false);
    if (!live(generation, check, context)) return failure(Error::Revoked, mask, false);
    const uint64_t declined = mask & ~supported;
    g_state.result = {Outcome::Applied, mask, supported, declined,
        (declined & ~kUnsupported) != 0 ? Error::LateConfiguration
            : declined != 0 ? Error::EarlierRustDeadline : Error::None};
    return g_state.result;
}
} // namespace home_startup
