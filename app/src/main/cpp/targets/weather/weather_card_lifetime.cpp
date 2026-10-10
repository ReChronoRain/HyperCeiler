/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "weather_card_lifetime.h"
#include "weather_card_lifetime_guard.generated.h"
#include <atomic>
#include <cerrno>
#include <climits>
#include <cstddef>
#include <cstring>
#include <fcntl.h>
#include <span>
#include <type_traits>
#include <sys/mman.h>
#include <sys/syscall.h>
#include <sys/uio.h>
#include <unistd.h>

#ifdef WEATHER_CARD_LIFETIME_TEST
namespace weather_card_lifetime_test {
long call(long, uintptr_t, uintptr_t, uintptr_t, uintptr_t, uintptr_t, uintptr_t) noexcept;
long page_size() noexcept;
int forward(uintptr_t, void *, size_t, int) noexcept;
void before_slot_cas() noexcept;
}
#endif

namespace weather_card_lifetime {
namespace {
namespace guard = weather_card_lifetime_guard;
constexpr char kFlutterPath[] = "/system_ext/lib64/libhyper_os_flutter.so";
enum class Capture : unsigned { Empty, Reading, Published };
enum class Phase : unsigned { Cold, Declined, Ready, Residue };
struct Image {
    uintptr_t flutter = 0, libc = 0, original = 0, slot = 0;
    size_t page_size = 0;
    long parent_pid = 0;
};
struct Pages {
    uintptr_t dart = 0;
    std::array<uintptr_t, 2> sites{}, pages{};
};
Image g_image;
Pages g_pages;
std::atomic<Capture> g_capture{Capture::Empty};
std::atomic<Phase> g_phase{Phase::Cold};
std::atomic<bool> g_closed{false};
std::atomic<uintptr_t> g_original{0};
std::atomic<long> g_child_pid{0};
// Bit zero publishes the immutable descriptor. Each active wrapper owns +2.
std::atomic<uint64_t> g_readers{0};
std::atomic_flag g_protect_attempt = ATOMIC_FLAG_INIT;
static_assert(std::atomic<uint64_t>::is_always_lock_free);
static_assert(std::atomic<uintptr_t>::is_always_lock_free);
struct Errno { const int saved = errno; ~Errno() { errno = saved; } };

long call(long number, uintptr_t a = 0, uintptr_t b = 0, uintptr_t c = 0,
          uintptr_t d = 0, uintptr_t e = 0, uintptr_t f = 0) noexcept {
#ifdef WEATHER_CARD_LIFETIME_TEST
    return weather_card_lifetime_test::call(number, a, b, c, d, e, f);
#else
    return syscall(number, a, b, c, d, e, f);
#endif
}
long pid() noexcept { return call(SYS_getpid); }
long tid() noexcept { return call(SYS_gettid); }
bool range(uintptr_t address, size_t length) noexcept {
    return address != 0 && length != 0 && address <= UINTPTR_MAX - length;
}
// Every caller supplies a view of its real destination object. The kernel
// validates the remote range; a short/failed transfer never counts as a match.
bool read_bytes(uintptr_t address, std::span<std::byte> output) noexcept {
    if (output.data() == nullptr || !range(address, output.size())
        || output.size() > static_cast<size_t>(LONG_MAX)) return false;
    iovec local{output.data(), output.size()}, remote{reinterpret_cast<void *>(address), output.size()};
    return call(SYS_process_vm_readv, pid(), reinterpret_cast<uintptr_t>(&local), 1,
                reinterpret_cast<uintptr_t>(&remote), 1, 0) == static_cast<long>(output.size());
}
template<class T> bool read_object(uintptr_t address, T &out) noexcept {
    static_assert(std::is_trivially_copyable_v<T>);
    return read_bytes(address, std::as_writable_bytes(std::span<T, 1>(&out, 1)));
}
template<size_t N> bool guards(uintptr_t base, const std::array<guard::Guard, N> &ranges) noexcept {
    std::array<uint8_t, 256> actual{};
    for (const auto &item : ranges) {
        if (base > UINTPTR_MAX - item.va || !range(base + item.va, item.size)) return false;
        for (size_t at = 0; at < item.size;) {
            const size_t length = item.size - at < actual.size() ? item.size - at : actual.size();
            if (!read_bytes(base + item.va + at, std::as_writable_bytes(std::span(actual).first(length)))
                || std::memcmp(actual.data(), item.data + at, length) != 0) return false;
            at += length;
        }
    }
    return true;
}

bool number(const char *text, size_t size, size_t &at, unsigned radix, uintptr_t &out) noexcept {
    const size_t begin = at;
    uintptr_t value = 0;
    while (at < size) {
        const unsigned char c = text[at];
        const unsigned digit = c >= '0' && c <= '9' ? c - '0'
            : c >= 'a' && c <= 'f' ? c - 'a' + 10 : 16;
        if (digit >= radix) break;
        if (value > (UINTPTR_MAX - digit) / radix) return false;
        value = value * radix + digit;
        ++at;
    }
    out = value;
    return at != begin;
}
struct Mapping {
    uintptr_t begin = 0, end = 0, offset = 0;
    int protection = 0;
    const char *path = nullptr;
    size_t path_size = 0;
};
bool parse_map(const char *text, size_t size, Mapping &m) noexcept {
    size_t at = 0;
    auto take = [&](char c) { return at < size && text[at++] == c; };
    if (!number(text, size, at, 16, m.begin) || !take('-')
        || !number(text, size, at, 16, m.end) || !take(' ') || m.begin >= m.end
        || size - at < 5) return false;
    const char *p = text + at;
    if ((p[0] != 'r' && p[0] != '-') || (p[1] != 'w' && p[1] != '-')
        || (p[2] != 'x' && p[2] != '-') || (p[3] != 'p' && p[3] != 's') || p[4] != ' ') return false;
    m.protection = (p[0] == 'r' ? PROT_READ : 0) | (p[1] == 'w' ? PROT_WRITE : 0)
        | (p[2] == 'x' ? PROT_EXEC : 0);
    // Shared mappings can be parsed, but cannot serve as any owned image page.
    if (p[3] == 's') m.protection |= 0x100;
    at += 5;
    uintptr_t ignored = 0;
    if (!number(text, size, at, 16, m.offset) || !take(' ')
        || !number(text, size, at, 16, ignored) || !take(':')
        || !number(text, size, at, 16, ignored) || !take(' ')
        || !number(text, size, at, 10, ignored)) return false;
    if (at < size && text[at] != ' ') return false;
    while (at < size && text[at] == ' ') ++at;
    m.path = text + at; m.path_size = size - at;
    return true;
}
// Entire bounded file must parse and end at EOF; an early match is insufficient.
template<class Visitor> bool maps(Visitor visitor) noexcept {
    const long fd = call(SYS_openat, AT_FDCWD, reinterpret_cast<uintptr_t>("/proc/self/maps"), O_RDONLY | O_CLOEXEC);
    if (fd < 0) return false;
    char buffer[4096], line[1024];
    size_t used = 0;
    bool okay = true, eof = false;
    for (unsigned block = 0; block < 256 && okay; ++block) {
        const long count = call(SYS_read, fd, reinterpret_cast<uintptr_t>(buffer), sizeof(buffer));
        if (count == 0) { eof = true; break; }
        if (count < 0 || count > static_cast<long>(sizeof(buffer))) { okay = false; break; }
        for (long i = 0; i < count; ++i) {
            if (buffer[i] == '\n') {
                Mapping item;
                if (!parse_map(line, used, item) || !visitor(item)) { okay = false; break; }
                used = 0;
            } else if (used == sizeof(line) || buffer[i] == '\0') { okay = false; break; }
            else line[used++] = buffer[i];
        }
    }
    const long closed = call(SYS_close, fd);
    return okay && eof && used == 0 && closed == 0;
}
int protection(uintptr_t address, size_t length) noexcept {
    if (!range(address, length)) return -1;
    int found = -1;
    unsigned matches = 0;
    if (!maps([&](const Mapping &m) {
            if (m.begin <= address && address + length <= m.end) { found = m.protection; ++matches; }
            return true;
        }) || matches != 1) return -1;
    return found;
}
uintptr_t page(uintptr_t address) noexcept { return address & ~(uintptr_t(g_image.page_size) - 1); }
bool images() noexcept {
    return guards(g_image.flutter, guard::kFlutterGuards) && guards(g_image.libc, guard::kLibcGuards)
        && protection(g_image.original, 24) == (PROT_READ | PROT_EXEC)
        && protection(g_image.libc + 0x7bcb4, 28) == (PROT_READ | PROT_EXEC);
}
bool slot_is(uintptr_t expected) noexcept {
    uintptr_t actual = 0;
    return read_object(g_image.slot, actual) && actual == expected;
}

bool single_thread() noexcept {
    const long current = pid();
    if (current <= 0 || current != tid()) return false;
    const long fd = call(SYS_openat, AT_FDCWD, reinterpret_cast<uintptr_t>("/proc/self/task"), O_RDONLY | O_CLOEXEC | O_DIRECTORY);
    if (fd < 0) return false;
    alignas(8) char buffer[4096];
    unsigned tasks = 0, dots = 0;
    bool okay = true, eof = false;
    // linux_dirent64: ino64, off64, reclen16, type8, nul-terminated name.
    for (unsigned block = 0; block < 8 && okay; ++block) {
        const long count = call(SYS_getdents64, fd, reinterpret_cast<uintptr_t>(buffer), sizeof(buffer));
        if (count == 0) { eof = true; break; }
        if (count < 0 || count > static_cast<long>(sizeof(buffer))) { okay = false; break; }
        for (size_t at = 0; at < static_cast<size_t>(count);) {
            if (static_cast<size_t>(count) - at < 20) { okay = false; break; }
            uint16_t length = 0;
            std::memcpy(&length, buffer + at + 16, sizeof(length));
            if (length < 20 || length % 8 != 0 || length > static_cast<size_t>(count) - at || buffer[at + 18] != 4) { okay = false; break; }
            const char *name = buffer + at + 19;
            const void *nul = std::memchr(name, 0, length - 19);
            if (nul == nullptr) { okay = false; break; }
            const size_t size = static_cast<const char *>(nul) - name;
            if (size == 1 && name[0] == '.') { if (dots & 1) okay = false; dots |= 1; }
            else if (size == 2 && name[0] == '.' && name[1] == '.') { if (dots & 2) okay = false; dots |= 2; }
            else {
                uintptr_t task = 0; size_t cursor = 0;
                if (size == 0 || name[0] == '0' || !number(name, size, cursor, 10, task)
                    || cursor != size || task != static_cast<uintptr_t>(current) || ++tasks != 1) okay = false;
            }
            if (!okay) break;
            at += length;
        }
    }
    const long closed = call(SYS_close, fd);
    return okay && eof && tasks == 1 && dots == 3 && closed == 0 && pid() == current && tid() == current;
}
int forward(void *address, size_t length, int advice) noexcept {
    const uintptr_t original = g_original.load(std::memory_order_acquire);
    if (!original) { errno = ENOSYS; return -1; }
#ifdef WEATHER_CARD_LIFETIME_TEST
    return weather_card_lifetime_test::forward(original, address, length, advice);
#else
    return reinterpret_cast<int (*)(void *, size_t, int)>(original)(address, length, advice);
#endif
}
int observed_madvise(void *address, size_t length, int advice) noexcept {
    const uint64_t previous = g_readers.fetch_add(2, std::memory_order_acq_rel);
    struct Reader { ~Reader() { g_readers.fetch_sub(2, std::memory_order_release); } } reader;
    const auto begin = reinterpret_cast<uintptr_t>(address);
    if (!(previous & 1) || advice != MADV_DONTNEED || length == 0
        || begin % g_image.page_size != 0 || begin > UINTPTR_MAX - length)
        return forward(address, length, advice);
    const uintptr_t end = begin + length;
    if (end > UINTPTR_MAX - (g_image.page_size - 1)) return forward(address, length, advice);
    // No extra syscalls or loader state in this path. At most three original calls.
    const int saved_errno = errno;
    uintptr_t cursor = begin;
    for (unsigned index = 0; index < 2; ++index) {
        const uintptr_t skip = g_pages.pages[index];
        if (index != 0 && skip == g_pages.pages[0]) continue;
        if (skip >= end || skip + g_image.page_size <= cursor) continue;
        if (skip > cursor && forward(reinterpret_cast<void *>(cursor), skip - cursor, advice) != 0) return -1;
        cursor = skip + g_image.page_size;
        if (cursor >= end) { errno = saved_errno; return 0; }
    }
    if (cursor < end && forward(reinterpret_cast<void *>(cursor), end - cursor, advice) != 0) return -1;
    errno = saved_errno;
    return 0;
}
uintptr_t wrapper() noexcept { return reinterpret_cast<uintptr_t>(&observed_madvise); }
bool change_protection(int wanted) noexcept {
    return call(SYS_mprotect, page(g_image.slot), g_image.page_size, wanted) == 0;
}
bool exchange_slot(uintptr_t expected, uintptr_t desired) noexcept {
#ifdef WEATHER_CARD_LIFETIME_TEST
    weather_card_lifetime_test::before_slot_cas();
#endif
    return __atomic_compare_exchange_n(reinterpret_cast<uintptr_t *>(g_image.slot), &expected,
                                        desired, false, __ATOMIC_ACQ_REL, __ATOMIC_ACQUIRE);
}
bool owned_observer() noexcept {
    return pid() == g_child_pid.load(std::memory_order_acquire)
        && slot_is(wrapper()) && protection(page(g_image.slot), g_image.page_size) == PROT_READ && images();
}
void install_observer() noexcept {
    g_original.store(g_image.original, std::memory_order_release);
    if (!change_protection(PROT_READ | PROT_WRITE)) {
        if (!slot_is(g_image.original) || protection(page(g_image.slot), g_image.page_size) != PROT_READ)
            g_phase.store(Phase::Residue, std::memory_order_release);
        return;
    }
    const bool installed = exchange_slot(g_image.original, wrapper());
    if (installed && change_protection(PROT_READ) && owned_observer()) {
        g_phase.store(Phase::Ready, std::memory_order_release);
        return;
    }
    // Undo only our pointer. A foreign pointer is never overwritten.
    bool restored = slot_is(g_image.original);
    if (slot_is(wrapper()) && change_protection(PROT_READ | PROT_WRITE))
        restored = exchange_slot(wrapper(), g_image.original);
    const bool permission_restored = change_protection(PROT_READ);
    if (!restored || !permission_restored || !slot_is(g_image.original)
        || protection(page(g_image.slot), g_image.page_size) != PROT_READ)
        g_phase.store(Phase::Residue, std::memory_order_release);
}
} // namespace

void observe_parent_load(const char *path) noexcept {
    Errno keep;
    if (path == nullptr || std::strcmp(path, kFlutterPath) != 0 || g_closed.load(std::memory_order_acquire)) return;
    Capture expected = Capture::Empty;
    if (!g_capture.compare_exchange_strong(expected, Capture::Reading, std::memory_order_acq_rel)) return;
    Image candidate;
    unsigned matches = 0;
    bool valid = maps([&](const Mapping &m) {
        if (m.offset == 0 && m.path_size == sizeof(kFlutterPath) - 1
            && std::memcmp(m.path, kFlutterPath, sizeof(kFlutterPath) - 1) == 0) {
            candidate.flutter = m.begin; ++matches;
            return m.protection == PROT_READ;
        }
        return true;
    });
#ifdef WEATHER_CARD_LIFETIME_TEST
    const long page_size = weather_card_lifetime_test::page_size();
#else
    const long page_size = sysconf(_SC_PAGESIZE);
#endif
    candidate.page_size = static_cast<size_t>(page_size);
    candidate.parent_pid = pid();
    valid = valid && matches == 1 && (page_size == 4096 || page_size == 16384)
        && candidate.parent_pid > 0 && candidate.flutter % candidate.page_size == 0
        && candidate.flutter <= UINTPTR_MAX - guard::kMadviseSlotVa;
    if (valid) {
        candidate.slot = candidate.flutter + guard::kMadviseSlotVa;
        valid = candidate.slot % alignof(uintptr_t) == 0 && read_object(candidate.slot, candidate.original)
            && candidate.original > guard::kMadviseVa;
    }
    if (valid) {
        candidate.libc = candidate.original - guard::kMadviseVa;
        valid = candidate.libc % candidate.page_size == 0
            && guards(candidate.flutter, guard::kFlutterGuards) && guards(candidate.libc, guard::kLibcGuards)
            && protection(candidate.original, 24) == (PROT_READ | PROT_EXEC)
            && protection(candidate.libc + 0x7bcb4, 28) == (PROT_READ | PROT_EXEC)
            && protection(candidate.slot & ~(uintptr_t(candidate.page_size) - 1), candidate.page_size) == PROT_READ;
    }
    if (valid && !g_closed.load(std::memory_order_acquire)) {
        g_image = candidate;
        g_capture.store(Capture::Published, std::memory_order_release);
    }
    // A failed first observation is terminal. No late scanning fallback.
}
void after_specialization(bool weather) noexcept {
    Errno keep;
    if (g_closed.exchange(true, std::memory_order_acq_rel)) return;
    g_phase.store(Phase::Declined, std::memory_order_release);
    if (!weather || g_capture.load(std::memory_order_acquire) != Capture::Published) return;
    const long current = pid();
    if (current <= 0 || current == g_image.parent_pid || call(SYS_getppid) != g_image.parent_pid
        || !single_thread() || !images() || !slot_is(g_image.original)
        || protection(page(g_image.slot), g_image.page_size) != PROT_READ) return;
    g_child_pid.store(current, std::memory_order_release);
    install_observer();
}
bool ready() noexcept {
    Errno keep;
    return g_phase.load(std::memory_order_acquire) == Phase::Ready && owned_observer();
}
bool has_residue() noexcept { return g_phase.load(std::memory_order_acquire) == Phase::Residue; }
bool protect(uintptr_t dart, uintptr_t flutter, const std::array<uintptr_t, 2> &sites) noexcept {
    Errno keep;
    if (g_protect_attempt.test_and_set(std::memory_order_acq_rel) || !ready() || flutter != g_image.flutter
        || dart == 0 || dart % g_image.page_size != 0) return false;
    Pages descriptor; descriptor.dart = dart; descriptor.sites = sites;
    for (unsigned index = 0; index < 2; ++index) {
        uint32_t original = 0;
        if (dart > UINTPTR_MAX - guard::kSites[index] || sites[index] != dart + guard::kSites[index]
            || !read_object(sites[index], original) || original != guard::kOriginalWord) return false;
        descriptor.pages[index] = page(sites[index]);
        if (!range(descriptor.pages[index], g_image.page_size)
            || protection(descriptor.pages[index], g_image.page_size) != (PROT_READ | PROT_EXEC)) return false;
    }
    g_pages = descriptor;
    uint64_t expected = 0;
    // Acquire the last pass-through reader's release before the caller may
    // write code; release publishes our immutable page descriptor to wrappers.
    return g_readers.compare_exchange_strong(expected, 1, std::memory_order_acq_rel, std::memory_order_acquire);
}
bool intact() noexcept {
    Errno keep;
    if (!(g_readers.load(std::memory_order_acquire) & 1) || !ready()) return false;
    for (uintptr_t address : g_pages.pages)
        if (protection(address, g_image.page_size) != (PROT_READ | PROT_EXEC)) return false;
    return true;
}
bool release_pages() noexcept {
    Errno keep;
    if (!(g_readers.load(std::memory_order_acquire) & 1)) return false;
    for (uintptr_t site : g_pages.sites) {
        uint32_t word = 0;
        if (!read_object(site, word) || word != guard::kOriginalWord) return false;
    }
    uint64_t expected = 1;
    return g_readers.compare_exchange_strong(expected, 0, std::memory_order_acq_rel);
}
} // namespace weather_card_lifetime
