/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include <algorithm>
#include <array>
#include <atomic>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <elf.h>
#include <fcntl.h>
#include <span>
#include <string>
#include <sys/mman.h>
#include <sys/wait.h>
#include <unistd.h>
#include <time.h>

#ifndef HC_CARD_RUNTIME_SOURCE
#define HC_CARD_RUNTIME_SOURCE "targets/weather/weather_card_runtime.cpp"
#endif
#include HC_CARD_RUNTIME_SOURCE

using namespace weather_card_runtime;
static uintptr_t dart_base = 0, flutter_base = 0;
static size_t host_page = 0;
static int64_t fake_now = 1'000'000'000'000LL;
static weather_snapshot::Snapshot snapshot{true, true, 7, 11, fake_now + 3'000'000'000LL, 3, 2, false};
static bool fresh_available = true, clock_denied = false;
static unsigned failures = 0;
static const char *case_name = "setup";
enum class Fault { None, SecondWrite, FirstRestoreOnce, FirstRestoreAlways,
                   CancelFirstWrite, ForeignSecond, ForeignTail, NoArena, WrongHint, CancelPublication,
                   LifetimeAbsent, LifetimeBusy, LifetimeLost, LifetimeLostAfterFirst };
static Fault fault = Fault::None;
static unsigned fault_hits = 0, near_calls = 0;
static bool first_writable = false;
static bool lifetime_protected = false;
static unsigned lifetime_claims = 0, lifetime_releases = 0;

// Explicit component boundary: the actual lifetime module has its own tests.
// This adapter lets the real code transaction observe refusal/takeover states;
// it does not prove a real early GOT installation or reclaim exclusion.
namespace weather_card_lifetime {
bool ready() noexcept { return fault != Fault::LifetimeAbsent; }
bool has_residue() noexcept { return false; }
bool protect(uintptr_t dart, uintptr_t flutter, const std::array<uintptr_t, 2> &sites) noexcept {
    ++lifetime_claims;
    if (fault == Fault::LifetimeBusy || dart != dart_base || flutter != flutter_base
        || sites != std::array<uintptr_t, 2>{dart + 0x3a8408, dart + 0x3aece0}) return false;
    lifetime_protected = true;
    return true;
}
bool intact() noexcept {
    return lifetime_protected && fault != Fault::LifetimeLost
        && !(fault == Fault::LifetimeLostAfterFirst && weather_card_runtime::status().owned_mask != 0);
}
bool release_pages() noexcept { ++lifetime_releases; lifetime_protected = false; return true; }
}

static void check(bool okay, const char *what) {
    if (!okay) { ++failures; std::fprintf(stderr, "FAIL %s: %s\n", case_name, what); }
}
static uintptr_t site(unsigned index) { return dart_base + (index ? 0x3aece0 : 0x3a8408); }
static uintptr_t pg(uintptr_t value) { return value & ~(uintptr_t(host_page) - 1); }
static uint32_t word(unsigned index) { return *reinterpret_cast<const uint32_t *>(site(index)); }
static void alter(uintptr_t address, uint32_t value);

extern "C" int __real_mprotect(void *, size_t, int);
extern "C" void *__real_mmap(void *, size_t, int, int, int, off_t);
extern "C" int __wrap_clock_gettime(clockid_t clock, timespec *value) {
    if (clock != CLOCK_BOOTTIME || clock_denied) { errno = EINVAL; return -1; }
    value->tv_sec = fake_now / 1'000'000'000LL;
    value->tv_nsec = fake_now % 1'000'000'000LL;
    return 0;
}
extern "C" void *__wrap_mmap(void *address, size_t length, int prot, int flags, int fd, off_t offset) {
    if ((flags & MAP_FIXED_NOREPLACE) != 0) {
        ++near_calls;
        if (fault == Fault::NoArena) { errno = EEXIST; return MAP_FAILED; }
        if (fault == Fault::WrongHint)
            return __real_mmap(nullptr, length, prot, flags & ~MAP_FIXED_NOREPLACE, fd, offset);
    }
    return __real_mmap(address, length, prot, flags, fd, offset);
}
extern "C" int __wrap_mprotect(void *address, size_t length, int prot) {
    const uintptr_t at = reinterpret_cast<uintptr_t>(address);
    const bool first = at == pg(site(0)), second = at == pg(site(1));
    const bool writing = prot == (PROT_READ | PROT_WRITE | PROT_EXEC);
    if (first && writing) first_writable = true;
    if (fault == Fault::SecondWrite && second && writing) { ++fault_hits; errno = EPERM; return -1; }
    if ((fault == Fault::FirstRestoreOnce || fault == Fault::FirstRestoreAlways)
        && first && first_writable && prot == (PROT_READ | PROT_EXEC)
        && (fault_hits == 0 || fault == Fault::FirstRestoreAlways)) {
        ++fault_hits; errno = EPERM; return -1;
    }
    if (fault == Fault::CancelFirstWrite && first && writing && fault_hits++ == 0) cancel();
    const int result = __real_mprotect(address, length, prot);
    if (result == 0 && fault == Fault::ForeignSecond && second && writing && fault_hits++ == 0)
        *reinterpret_cast<uint32_t *>(site(1)) = 0xd503201f;
    if (result == 0 && fault == Fault::ForeignTail && first && first_writable
        && prot == (PROT_READ | PROT_EXEC) && fault_hits++ == 0) alter(site(0) + 4, 0xd503201f);
    return result;
}
static void alter(uintptr_t address, uint32_t value) {
    check(__real_mprotect(reinterpret_cast<void *>(pg(address)), host_page, PROT_READ | PROT_WRITE | PROT_EXEC) == 0,
          "fixture writable");
    *reinterpret_cast<uint32_t *>(address) = value;
    check(__real_mprotect(reinterpret_cast<void *>(pg(address)), host_page, PROT_READ | PROT_EXEC) == 0,
          "fixture RX");
}
static bool fresh(weather_snapshot::Snapshot &out) {
    if (fault == Fault::CancelPublication && status().phase == Phase::Installed && fault_hits++ == 0) cancel();
    out = snapshot;
    return fresh_available;
}
static bool start() { return prepare(fresh) && install_before_snapshot(dart_base, flutter_base); }
static void original() {
    check(word(0) == 0xaa1603e0 && word(1) == 0xaa1603e0, "both original first words");
    check(protection(pg(site(0)), host_page) == (PROT_READ | PROT_EXEC)
       && protection(pg(site(1)), host_page) == (PROT_READ | PROT_EXEC), "both original RX protections");
}
static void installed() {
    check(start(), "complete production transaction installed");
    check(status().phase == Phase::Installed && status().owned_mask == 3, "installed both sites");
    check((word(0) & 0xfc000000) == 0x14000000 && (word(1) & 0xfc000000) == 0x14000000, "two B instructions");
    check(word(0) == g_branches[0] && word(1) == g_branches[1], "exact owned branches");
    check(g_lease.read() == (uint64_t(snapshot.valid_until) << 2 | 2), "unaltered authoritative deadline and raw light");
    check(arena_intact(), "RX arena contains exact production templates and lease literals");
    check(image_state(3), "full production guards accept owned image");
    check(lifetime_protected && lifetime_claims == 1, "page lifetime reserved before patch publication");
}
static void positive() { installed(); }
static void switch_modes() {
    installed(); snapshot.card_type = 3; ++snapshot.revision; service();
    check((g_lease.read() & 3) == 3, "subsequent poll selects dark");
    snapshot.card_type = 0; ++snapshot.revision; service();
    check(g_lease.read() == 0, "default disables future overrides");
    snapshot.card_type = 2; ++snapshot.revision; service();
    check((g_lease.read() & 3) == 2 && status().owned_mask == 3, "enabled again through installed inert gateway");
    check(image_state(3), "mode changes leave both code patches unchanged");
}
static void unavailable() { installed(); fresh_available = false; service(); check(g_lease.read() == 0, "unavailable transport clears lease"); }
static void expiry() { installed(); fake_now = snapshot.valid_until; service(); check(g_lease.read() == 0, "expiry cannot be renewed from receipt"); }
static void bad_clock() { installed(); clock_denied = true; service(); check(g_lease.read() == 0, "clock error disables publication"); }
static void terminal() {
    installed(); cancel(); snapshot.card_type = 3; ++snapshot.revision; service();
    check(g_lease.read() == weather_card_control::kTerminal && status().phase == Phase::Missed, "terminal cannot reopen");
    check(!prepare(fresh) && !install_before_snapshot(dart_base, flutter_base), "no second installation");
    check(image_state(3) && arena_intact(), "cancel keeps in-flight code and storage alive");
}
static void lost_code() {
    installed(); alter(site(1), 0xaa1603e0); service();
    check(g_lease.terminal() && status().phase == Phase::Missed, "lost patch permanently disables");
    check(word(1) == 0xaa1603e0 && word(0) == g_branches[0], "maintenance does not repair code");
}
static void changed_arena() {
    installed(); alter(status().arena + 64, 0xd503201f); service();
    check(g_lease.terminal(), "foreign arena bytes permanently disable");
}
static void permission_change() {
    installed(); check(__real_mprotect(reinterpret_cast<void *>(pg(site(1))), host_page, PROT_READ | PROT_WRITE | PROT_EXEC) == 0, "fixture changes protection");
    service(); check(g_lease.terminal(), "unexpected live protection disables");
    check(protection(pg(site(1)), host_page) == (PROT_READ | PROT_WRITE | PROT_EXEC), "maintenance does not overwrite foreign permissions");
}
static void bad_initial_guard() {
    alter(site(0) + 12, 0xd503201f); check(!start(), "bad complete body refused");
    check(status().arena == 0 && status().owned_mask == 0 && g_lease.terminal(), "guard failure before allocation or code write");
}
static void no_authorization() { snapshot.allowed = false; check(!start(), "initial denial refuses install"); original(); }
static void no_near_arena() { fault = Fault::NoArena; check(!start(), "bounded no-free-near-page refusal"); check(near_calls == 8, "eight finite placement attempts"); original(); }
static void wrong_hint() { fault = Fault::WrongHint; check(!start(), "unknown flag wrong mapping refused"); check(near_calls == 8 && status().arena == 0, "all wrong mappings released"); original(); }
static void rollback_second() { fault = Fault::SecondWrite; check(!start(), "second write failure refused"); original(); check(status().phase == Phase::Rejected && status().owned_mask == 0, "clean rollback classified"); }
static void lifetime_absent() {
    fault = Fault::LifetimeAbsent; check(!start(), "missing early observer refuses preparation");
    check(status().error == Error::LifetimeRejected && status().arena == 0 && lifetime_claims == 0,
          "no late observer fallback or arena allocation"); original();
}
static void lifetime_busy() {
    fault = Fault::LifetimeBusy; check(!start(), "in-flight unprotected reclaim refuses installation");
    check(lifetime_claims == 1 && !lifetime_protected && g_lease.terminal(), "one bounded claim without code writes"); original();
}
static void lifetime_lost() {
    installed(); fault = Fault::LifetimeLost; service();
    check(g_lease.terminal() && status().phase == Phase::Missed, "observer takeover revokes future writes");
    check(image_state(3), "maintenance leaves live code inert without repair");
}
static void lifetime_lost_after_first() {
    fault = Fault::LifetimeLostAfterFirst; check(!start(), "observer lost during install refuses commit");
    original(); check(status().owned_mask == 0 && g_lease.terminal(), "first code patch rolled back");
    check(!lifetime_protected && lifetime_releases > 0, "verified original code permits page release");
}
static void rollback_restore() { fault = Fault::FirstRestoreOnce; check(!start(), "first permission restore failure refused"); original(); check(status().phase == Phase::Rejected && g_lease.terminal(), "clean rollback after protection failure"); }
static void residue_restore() {
    fault = Fault::FirstRestoreAlways; check(!start(), "repeated protection failure refused");
    check(status().phase == Phase::Residue && status().error == Error::RollbackFailed, "unresolved permission remains residue");
    check(word(0) == 0xaa1603e0 && g_lease.terminal(), "original word alone is not rollback success");
    cancel(); check(status().phase == Phase::Residue, "cancellation preserves residue");
}
static void cancel_install() { fault = Fault::CancelFirstWrite; check(!start(), "cancellation while writable blocks commit"); original(); check(g_lease.terminal(), "no authorization after cancelled install"); }
static void cancel_publication() { fault = Fault::CancelPublication; check(!start(), "cancel at publication rejects stale publisher"); original(); check(g_lease.terminal(), "terminal not changed by late CAS"); }
static void foreign_word() {
    fault = Fault::ForeignSecond; check(!start(), "foreign second word blocks CAS");
    check(word(0) == 0xaa1603e0 && word(1) == 0xd503201f, "rollback restores ours and preserves foreign word");
    check(status().phase == Phase::Residue && g_lease.terminal(), "foreign image not described as clean");
}
static void foreign_tail() {
    fault = Fault::ForeignTail; check(!start(), "full-body readback catches untouched-tail mutation");
    check(word(0) == 0xaa1603e0 && *reinterpret_cast<uint32_t *>(site(0) + 4) == 0xd503201f, "foreign tail preserved");
    check(status().phase == Phase::Residue && g_lease.terminal(), "full-body rollback proof required");
}
static void bounded_busy() { installed(); g_writer.test_and_set(); snapshot.card_type = 0; service(); check(status().phase == Phase::Installed, "busy service returns without waiting"); g_writer.clear(); service(); check(g_lease.read() == 0, "next worker pass handles update"); }
static void parser() {
    const std::string valid = "1000-3000 r-xp 00000000 00:00 0 /a path with spaces";
    check(mapping_line(valid.data(), valid.size(), 0x1000, 0x2000) == 5, "complete private RX span");
    check(mapping_line(valid.data(), valid.size(), 0x1000, 0x2001) == -1, "cross-map span refused");
    for (const char *bad : {"1000-3000 r-xs 0", "1000-3000 rwxpX", "1000-3000 r?xp 0", "10000000000000000-3000 r-xp 0", "1000-1000 r-xp 0", "1000-3000 r-xp"})
        check(mapping_line(bad, std::strlen(bad), 0x1000, 4) == -1, "malformed/shared mapping refused");
}
static void policy() {
    alignas(8) uint64_t data = 0; weather_card_control::Lease lease(data);
    check(lease.publish(0, snapshot, fake_now), "coherent allowed initial projection");
    const auto old = lease.read(); lease.cancel();
    check(!lease.publish(old, snapshot, fake_now), "old publisher loses cancellation race");
    check(!lease.publish(0, snapshot, fake_now), "initial zero publisher cannot reopen cancelled state");
    check(!lease.publish(weather_card_control::kTerminal, snapshot, fake_now), "terminal token cannot be reused by publisher");
    check(!lease.clear(old) && lease.terminal(), "stale clear preserves terminal token");
    for (int type : {-1, 0, 1, 4, INT32_MAX}) { auto value = snapshot; value.card_type = type; check(weather_card_control::project(value, fake_now) == 0, "invalid/default raw mode denied"); }
    auto value = snapshot; value.valid_until = fake_now; check(weather_card_control::project(value, fake_now) == 0, "deadline equality denied");
    value.valid_until = fake_now + weather_snapshot::kMaxLeaseNanos + 1; check(weather_card_control::project(value, fake_now) == 0, "oversized lease denied");
    value.valid_until = INT64_MAX; check(weather_card_control::project(value, INT64_MAX - 3) == 0, "deadline cannot overflow 62 bits");
    uint32_t branch_word = 0;
    check(weather_card_control::branch(0x10000000, 0x17fffffc, branch_word) && branch_word == 0x15ffffff, "maximum forward B");
    check(weather_card_control::branch(0x10000000, 0x08000000, branch_word) && branch_word == 0x16000000, "maximum backward B");
    check(!weather_card_control::branch(0x10000000, 0x18000000, branch_word), "forward B overflow refused");
    check(!weather_card_control::branch(0x10000000, 0x07fffffc, branch_word), "backward B overflow refused");
    check(!weather_card_control::branch(0x10000000, 0x10000000, branch_word), "self branch refused");
    check(!weather_card_control::branch(0x10000000, UINTPTR_MAX - 3, branch_word), "address truncation refused");
}
static uintptr_t map_elf(const char *path) {
    const int fd = open(path, O_RDONLY | O_CLOEXEC); if (fd < 0) std::abort();
    Elf64_Ehdr eh{}; if (pread(fd, &eh, sizeof(eh), 0) != sizeof(eh) || eh.e_machine != EM_AARCH64 || eh.e_phnum > 64) std::abort();
    std::array<Elf64_Phdr, 64> ph{};
    const size_t bytes = eh.e_phnum * sizeof(Elf64_Phdr);
    if (pread(fd, ph.data(), bytes, eh.e_phoff) != static_cast<ssize_t>(bytes)) std::abort();
    size_t extent = 0;
    for (unsigned i = 0; i < eh.e_phnum; ++i) if (ph[i].p_type == PT_LOAD) extent = std::max(extent, size_t(ph[i].p_vaddr + ph[i].p_memsz));
    extent = (extent + host_page - 1) & ~(host_page - 1);
    void *region = __real_mmap(nullptr, extent, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (region == MAP_FAILED) std::abort();
    const auto base = reinterpret_cast<uintptr_t>(region);
    for (unsigned i = 0; i < eh.e_phnum; ++i) {
        const auto &p = ph[i]; if (p.p_type != PT_LOAD || p.p_filesz == 0) continue;
        if (pread(fd, reinterpret_cast<void *>(base + p.p_vaddr), p.p_filesz, p.p_offset) != static_cast<ssize_t>(p.p_filesz)) std::abort();
    }
    close(fd);
    if (__real_mprotect(region, extent, PROT_READ) != 0) std::abort();
    for (unsigned i = 0; i < eh.e_phnum; ++i) {
        const auto &p = ph[i]; if (p.p_type != PT_LOAD || (p.p_flags & PF_X) == 0) continue;
        const uintptr_t start = pg(base + p.p_vaddr);
        const uintptr_t end = (base + p.p_vaddr + p.p_memsz + host_page - 1) & ~(host_page - 1);
        if (__real_mprotect(reinterpret_cast<void *>(start), end - start, PROT_READ | PROT_EXEC) != 0) std::abort();
    }
    return base;
}
int main(int argc, char **argv) {
    if (argc != 3) return 2;
    host_page = sysconf(_SC_PAGESIZE); dart_base = map_elf(argv[1]); flutter_base = map_elf(argv[2]);
    struct Case { const char *name; void (*run)(); };
    const Case cases[] = {
        {"positive", positive}, {"mode updates", switch_modes}, {"unavailable", unavailable}, {"expiry", expiry},
        {"clock failure", bad_clock}, {"terminal", terminal}, {"lost code", lost_code}, {"changed arena", changed_arena},
        {"permission change", permission_change}, {"initial full guard", bad_initial_guard}, {"initial denial", no_authorization},
        {"near placement", no_near_arena}, {"ignored mapping flag", wrong_hint}, {"second site rollback", rollback_second},
        {"permission rollback", rollback_restore}, {"permission residue", residue_restore}, {"install cancellation", cancel_install},
        {"publication cancellation", cancel_publication}, {"foreign word", foreign_word}, {"foreign tail", foreign_tail},
        {"bounded busy", bounded_busy}, {"maps parser", parser}, {"lease policy", policy},
        {"lifetime absent", lifetime_absent}, {"lifetime busy", lifetime_busy},
        {"lifetime lost", lifetime_lost}, {"lifetime install takeover", lifetime_lost_after_first}};
    unsigned failed = 0;
    for (const auto &test : cases) {
        const pid_t child = fork();
        if (child == 0) { case_name = test.name; test.run(); std::fflush(nullptr); _exit(failures ? 1 : 0); }
        if (child < 0) return 2;
        int status_code = 0; waitpid(child, &status_code, 0);
        if (!WIFEXITED(status_code) || WEXITSTATUS(status_code) != 0) {
            ++failed; std::fprintf(stderr, "CASE_FAILED %s status=%d\n", test.name, status_code);
        }
    }
    std::printf("RESULT cases=%zu failed=%u\n", std::size(cases), failed);
    return failed ? 1 : 0;
}
