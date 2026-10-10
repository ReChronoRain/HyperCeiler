/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "weather_card_runtime.h"
#include "weather_card_binding.h"
#include "weather_card_control.h"
#include "weather_card_gateway.h"
#include "weather_card_lifetime.h"
#include <array>
#include <atomic>
#include <cerrno>
#include <cstring>
#include <fcntl.h>
#include <limits>
#include <span>
#include <sys/mman.h>
#include <sys/syscall.h>
#include <sys/uio.h>
#include <time.h>
#include <unistd.h>

namespace weather_card_runtime {
namespace {
constexpr size_t kGatewayStride = 304; // both templates and their literals stay 8-byte aligned
constexpr size_t kArenaBytes = kGatewayStride + weather_card_gateway::kTemplateSize;
constexpr int kCodeProtection = PROT_READ | PROT_EXEC;
alignas(8) uint64_t g_lease_word = 0;
weather_card_control::Lease g_lease(g_lease_word);
std::atomic<Phase> g_phase{Phase::Cold};
std::atomic<Error> g_error{Error::None};
std::atomic<unsigned> g_owned{0};
std::atomic<uintptr_t> g_arena{0};
std::atomic_flag g_writer = ATOMIC_FLAG_INIT;
ReadFresh g_read_fresh = nullptr;
size_t g_page_size = 0;
weather_card::Binding g_binding{};
weather_card::BranchWords g_branches{};
alignas(8) std::array<std::byte, kArenaBytes> g_templates{};
std::array<bool, 2> g_protection_pending{};

struct Writer {
    const bool owns = !g_writer.test_and_set(std::memory_order_acquire);
    ~Writer() { if (owns) g_writer.clear(std::memory_order_release); }
};

int64_t now() noexcept {
    timespec value{};
    if (clock_gettime(CLOCK_BOOTTIME, &value) != 0 || value.tv_sec < 0
        || value.tv_nsec < 0 || value.tv_nsec >= 1'000'000'000L
        || value.tv_sec > (std::numeric_limits<int64_t>::max() - 999'999'999) / 1'000'000'000)
        return 0;
    return static_cast<int64_t>(value.tv_sec) * 1'000'000'000LL + value.tv_nsec;
}

bool read_memory(uintptr_t address, std::span<std::byte> output) noexcept {
    if (address == 0 || output.empty() || address > UINTPTR_MAX - output.size()) return false;
    iovec local{output.data(), output.size()}, remote{reinterpret_cast<void *>(address), output.size()};
    return syscall(SYS_process_vm_readv, getpid(), &local, 1, &remote, 1, 0)
        == static_cast<ssize_t>(output.size());
}

bool read_word(uintptr_t address, uint32_t &word) noexcept {
    return read_memory(address, std::as_writable_bytes(std::span(&word, 1)));
}

bool hex(const char *line, size_t size, size_t &cursor, uintptr_t &result) noexcept {
    const size_t start = cursor;
    uintptr_t number = 0;
    while (cursor < size) {
        const unsigned char c = line[cursor];
        const unsigned digit = c >= '0' && c <= '9' ? c - '0'
            : c >= 'a' && c <= 'f' ? c - 'a' + 10
            : c >= 'A' && c <= 'F' ? c - 'A' + 10 : 16;
        if (digit == 16) break;
        if (number > (UINTPTR_MAX - digit) / 16) return false;
        number = number * 16 + digit;
        ++cursor;
    }
    if (cursor == start) return false;
    result = number;
    return true;
}

int mapping_line(const char *line, size_t size, uintptr_t address, size_t length) noexcept {
    size_t cursor = 0;
    uintptr_t begin = 0, end = 0;
    if (address == 0 || length == 0 || address > UINTPTR_MAX - length
        || !hex(line, size, cursor, begin) || cursor >= size || line[cursor++] != '-'
        || !hex(line, size, cursor, end) || cursor >= size || line[cursor++] != ' '
        || end <= begin || address < begin || address + length > end || cursor + 4 >= size)
        return -1;
    const char *permission = line + cursor;
    if ((permission[0] != 'r' && permission[0] != '-')
        || (permission[1] != 'w' && permission[1] != '-')
        || (permission[2] != 'x' && permission[2] != '-')
        || permission[3] != 'p' || permission[4] != ' ') return -1;
    return (permission[0] == 'r' ? PROT_READ : 0)
        | (permission[1] == 'w' ? PROT_WRITE : 0)
        | (permission[2] == 'x' ? PROT_EXEC : 0);
}

// A bounded /proc read uses fixed buffers and raw syscalls. No heap, maps scan
// through iostreams, loader lookup, retry loop, Binder or worker creation occurs
// at the snapshot boundary. An incomplete/oversized maps file simply declines.
int protection(uintptr_t address, size_t length) noexcept {
    const int fd = static_cast<int>(syscall(SYS_openat, AT_FDCWD, "/proc/self/maps", O_RDONLY | O_CLOEXEC, 0));
    if (fd < 0) return -1;
    char buffer[4096], prefix[96];
    size_t used = 0;
    int result = -1;
    bool complete = false;
    for (unsigned chunk = 0; chunk < 64 && !complete; ++chunk) {
        const long count = syscall(SYS_read, fd, buffer, sizeof(buffer));
        if (count <= 0) break;
        for (long i = 0; i < count; ++i) {
            if (buffer[i] == '\n') {
                result = mapping_line(prefix, used, address, length);
                used = 0;
                if (result >= 0) { complete = true; break; }
            } else if (used < sizeof(prefix)) prefix[used++] = buffer[i];
        }
    }
    syscall(SYS_close, fd);
    return complete ? result : -1;
}

uintptr_t page(uintptr_t address) noexcept { return address & ~(uintptr_t(g_page_size) - 1); }

bool current(weather_snapshot::Snapshot &snapshot) noexcept {
    return !g_lease.terminal() && g_read_fresh != nullptr && g_read_fresh(snapshot)
        && weather_card_control::project(snapshot, now()) != 0;
}

bool same_authorization(const weather_snapshot::Snapshot &initial) noexcept {
    weather_snapshot::Snapshot latest;
    return current(latest) && latest.epoch == initial.epoch && latest.revision == initial.revision
        && latest.card_type == initial.card_type && latest.valid_until == initial.valid_until;
}

bool arena_intact() noexcept {
    const auto arena = g_arena.load(std::memory_order_acquire);
    if (arena == 0 || protection(arena, g_page_size) != kCodeProtection) return false;
    std::array<std::byte, kArenaBytes> actual{};
    return read_memory(arena, actual) && actual == g_templates;
}

bool image_state(unsigned mask) noexcept {
    return weather_card::verify_state(g_binding, read_memory, g_branches, mask);
}

bool allocate_arena() noexcept {
    constexpr std::array<int64_t, 8> offsets = {
        0x04000000, -0x04000000, 0x06000000, -0x06000000,
        0x02000000, -0x02000000, 0x07000000, -0x07000000};
    for (const int64_t offset : offsets) {
        uintptr_t candidate = page(g_binding.dart_base);
        if (offset > 0) {
            if (candidate > UINTPTR_MAX - static_cast<uintptr_t>(offset)) continue;
            candidate += static_cast<uintptr_t>(offset);
        } else {
            if (candidate <= static_cast<uintptr_t>(-offset)) continue;
            candidate -= static_cast<uintptr_t>(-offset);
        }
        if (candidate > UINTPTR_MAX - g_page_size) continue;
        weather_card::BranchWords branches{};
        if (!weather_card_control::branch(g_binding.epilogue[0], candidate, branches[0])
            || !weather_card_control::branch(g_binding.epilogue[1], candidate + kGatewayStride, branches[1]))
            continue;
        void *mapped = mmap(reinterpret_cast<void *>(candidate), g_page_size, PROT_READ | PROT_WRITE,
            MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED_NOREPLACE, -1, 0);
        if (mapped == MAP_FAILED) continue;
        // Old kernels may ignore an unknown flag and treat this as a hint. A
        // different result is never adopted and MAP_FIXED is never a fallback.
        if (reinterpret_cast<uintptr_t>(mapped) != candidate) { munmap(mapped, g_page_size); continue; }
        g_arena.store(candidate, std::memory_order_release);
        g_branches = branches;
        std::memcpy(mapped, g_templates.data(), g_templates.size());
        __builtin___clear_cache(static_cast<char *>(mapped), static_cast<char *>(mapped) + g_templates.size());
        if (mprotect(mapped, g_page_size, kCodeProtection) != 0 || !arena_intact()) return false;
        return true;
    }
    return false;
}

struct WriteResult { bool changed = false; bool restored = false; };
WriteResult exchange(unsigned index, uint32_t expected, uint32_t desired) noexcept {
    const uintptr_t address = g_binding.epilogue[index];
    const auto begin = page(address);
    const int observed = protection(begin, g_page_size);
    if (observed != kCodeProtection
        && !(g_protection_pending[index] && observed == (kCodeProtection | PROT_WRITE))) return {};
    if (mprotect(reinterpret_cast<void *>(begin), g_page_size, kCodeProtection | PROT_WRITE) != 0)
        return {};
    g_protection_pending[index] = true;
    const bool changed = __atomic_compare_exchange_n(reinterpret_cast<uint32_t *>(address),
        &expected, desired, false, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
    if (changed) __builtin___clear_cache(reinterpret_cast<char *>(address), reinterpret_cast<char *>(address + 4));
    const bool restored = mprotect(reinterpret_cast<void *>(begin), g_page_size, kCodeProtection) == 0
        && protection(begin, g_page_size) == kCodeProtection;
    if (restored) g_protection_pending[index] = false;
    return {changed, restored};
}

bool rollback() noexcept {
    g_lease.cancel();
    bool okay = true;
    unsigned mask = g_owned.load(std::memory_order_relaxed);
    for (unsigned count = 2; count != 0; --count) {
        const unsigned index = count - 1;
        uint32_t word = 0;
        if (!read_word(g_binding.epilogue[index], word)) { okay = false; continue; }
        if (word == g_branches[index] && (mask & (1u << index)) != 0) {
            const auto undone = exchange(index, g_branches[index], weather_card_guard::kOriginalEpilogueWord);
            if (!undone.changed || !undone.restored) okay = false;
            if (!read_word(g_binding.epilogue[index], word)) { okay = false; continue; }
        }
        if (word == weather_card_guard::kOriginalEpilogueWord) mask &= ~(1u << index);
        else okay = false; // Never replace a foreign word, even after our failed CAS.
        if (g_protection_pending[index]) {
            const auto begin = page(g_binding.epilogue[index]);
            if (mprotect(reinterpret_cast<void *>(begin), g_page_size, kCodeProtection) == 0
                && protection(begin, g_page_size) == kCodeProtection) g_protection_pending[index] = false;
            else okay = false;
        }
    }
    g_owned.store(mask, std::memory_order_release);
    return okay && mask == 0 && image_state(0);
}

bool fail(Error error, bool attempted) noexcept {
    g_lease.cancel();
    const bool restored = !attempted || rollback();
    // Keep the observer and its original forward target for process lifetime.
    // Only a verified return to original code permits dropping the two-page
    // policy; an in-flight reader may make this non-waiting release refuse.
    if (restored && g_owned.load(std::memory_order_acquire) == 0 && g_binding.dart_base != 0
        && image_state(0)) weather_card_lifetime::release_pages();
    g_error.store(restored ? error : Error::RollbackFailed, std::memory_order_release);
    g_phase.store(restored ? (error == Error::Cancelled ? Phase::Missed : Phase::Rejected)
                          : Phase::Residue, std::memory_order_release);
    // Keep any allocated arena and the lease storage for process lifetime. An
    // in-flight gateway may still hold either even after a clean rollback.
    return false;
}

bool publish_current() noexcept {
    const uint64_t expected = g_lease.load();
    weather_snapshot::Snapshot snapshot;
    if (!current(snapshot)) return g_lease.clear(expected);
    return g_lease.publish(expected, snapshot, now());
}
} // namespace

bool prepare(ReadFresh read_fresh) noexcept {
    Writer writer;
    if (!writer.owns || g_phase.load(std::memory_order_acquire) != Phase::Cold || g_lease.terminal()) return false;
    if (!weather_card_lifetime::ready()) return fail(Error::LifetimeRejected, false);
    const long page_size = sysconf(_SC_PAGESIZE);
    if (read_fresh == nullptr || page_size < static_cast<long>(kArenaBytes) || page_size > 65536
        || (page_size & (page_size - 1)) != 0) return fail(Error::NotPrepared, false);
    g_page_size = static_cast<size_t>(page_size);
    g_read_fresh = read_fresh;
    const auto lease = reinterpret_cast<uintptr_t>(&g_lease_word);
    if (!weather_card_gateway::copy_template(weather_card_gateway::Kind::Recompute,
            std::span(g_templates).first(weather_card_gateway::kTemplateSize), lease)
        || !weather_card_gateway::copy_template(weather_card_gateway::Kind::Constructor,
            std::span(g_templates).subspan(kGatewayStride, weather_card_gateway::kTemplateSize), lease))
        return fail(Error::TemplateRejected, false);
    auto cold = Phase::Cold;
    return !g_lease.terminal()
        && g_phase.compare_exchange_strong(cold, Phase::Prepared, std::memory_order_acq_rel);
}

bool install_before_snapshot(uintptr_t dart_base, uintptr_t flutter_base) noexcept {
    Writer writer;
    if (!writer.owns) { cancel(); return false; }
    auto prepared = Phase::Prepared;
    if (!g_phase.compare_exchange_strong(prepared, Phase::Installing, std::memory_order_acq_rel)) {
        cancel(); return false;
    }
    weather_snapshot::Snapshot initial;
    if (!current(initial)) return fail(Error::NotAuthorized, false);
    if (!weather_card::resolve(dart_base, flutter_base, read_memory, g_binding))
        return fail(Error::ImageMismatch, false);
    if (!same_authorization(initial)) return fail(Error::Cancelled, false);
    if (!allocate_arena()) return fail(Error::NoArena, false);
    // The early observer counts pre-publication forwards until they return.
    // This one-shot protection handshake refuses any such in-flight call;
    // no code is patched unless both exact pages are protected first.
    if (!weather_card_lifetime::protect(dart_base, flutter_base, g_binding.epilogue))
        return fail(Error::LifetimeRejected, false);
    if (!image_state(0) || !arena_intact() || !weather_card_lifetime::intact())
        return fail(Error::ImageMismatch, false);
    unsigned mask = 0;
    for (unsigned index = 0; index < 2; ++index) {
        if (!same_authorization(initial)) return fail(Error::Cancelled, mask != 0);
        if (!weather_card_lifetime::intact()) return fail(Error::LifetimeRejected, mask != 0);
        if (protection(page(g_binding.epilogue[index]), g_page_size) != kCodeProtection)
            return fail(Error::ProtectionRejected, mask != 0);
        const auto changed = exchange(index, weather_card_guard::kOriginalEpilogueWord, g_branches[index]);
        if (changed.changed) {
            mask |= 1u << index;
            g_owned.store(mask, std::memory_order_release);
        }
        if (!changed.changed || !changed.restored) return fail(Error::WriteFailed, true);
        if (!image_state(mask) || !arena_intact() || !weather_card_lifetime::intact())
            return fail(Error::ReadbackFailed, true);
    }
    if (!same_authorization(initial)) return fail(Error::Cancelled, true);
    auto installing = Phase::Installing;
    if (g_lease.terminal()
        || !g_phase.compare_exchange_strong(installing, Phase::Installed, std::memory_order_acq_rel))
        return fail(Error::Cancelled, true);
    if (!publish_current()) return fail(Error::Cancelled, true);
    return true;
}

void service() noexcept {
    if (g_phase.load(std::memory_order_acquire) != Phase::Installed || g_lease.terminal()) return;
    Writer writer;
    if (!writer.owns) return;
    if (g_phase.load(std::memory_order_acquire) != Phase::Installed || g_lease.terminal()) return;
    if (!image_state(3) || !arena_intact() || !weather_card_lifetime::intact()
        || protection(page(g_binding.epilogue[0]), g_page_size) != kCodeProtection
        || protection(page(g_binding.epilogue[1]), g_page_size) != kCodeProtection) {
        g_error.store(Error::ForeignCode, std::memory_order_release);
        cancel(); // Never repair live Dart code from the polling thread.
        return;
    }
    publish_current();
}

void cancel() noexcept {
    g_lease.cancel();
    // Retain Residue: cancellation does not prove a failed restoration healed.
    Phase phase = g_phase.load(std::memory_order_acquire);
    for (unsigned attempt = 0; attempt < 4 && phase != Phase::Residue && phase != Phase::Missed; ++attempt)
        if (g_phase.compare_exchange_strong(phase, Phase::Missed, std::memory_order_acq_rel)) break;
}

Status status() noexcept {
    return {g_phase.load(std::memory_order_acquire), g_error.load(std::memory_order_acquire),
            g_owned.load(std::memory_order_acquire), g_arena.load(std::memory_order_acquire)};
}
} // namespace weather_card_runtime
