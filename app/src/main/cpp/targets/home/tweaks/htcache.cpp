// SPDX-License-Identifier: Apache-2.0
#include "htcache.h"
#include "common.h"
#include "nativehook/memory_io.h"
#include <elf.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>
#include <algorithm>
#include <array>
#include <atomic>
#include <cstdio>
#include <limits>
#include <span>
#include <vector>

namespace hometweaks {
namespace {
constexpr char kCacheName[] = "hometweaks-sites.bin";
constexpr char kCacheMagic[4] = {'H', 'T', 'S', '2'};
constexpr uint32_t kCacheFormat = 2;
constexpr size_t kCacheMaxBytes = 8u * 1024u;
constexpr size_t kHeaderBytes = 4 + 4 + 4 + 8 + 4;
constexpr uint64_t kMaxIdentityBytes = 512u * 1024u * 1024u;
constexpr size_t kIdentityChunk = 64u * 1024u;

void AppendU32(std::vector<uint8_t>* out, uint32_t v) {
    out->push_back(static_cast<uint8_t>(v & 0xFFu));
    out->push_back(static_cast<uint8_t>((v >> 8) & 0xFFu));
    out->push_back(static_cast<uint8_t>((v >> 16) & 0xFFu));
    out->push_back(static_cast<uint8_t>((v >> 24) & 0xFFu));
}

void AppendU64(std::vector<uint8_t>* out, uint64_t v) {
    for (int i = 0; i < 8; ++i) {
        out->push_back(static_cast<uint8_t>((v >> (8 * i)) & 0xFFu));
    }
}

uint32_t ReadU32(const uint8_t* p) {
    return static_cast<uint32_t>(p[0]) | (static_cast<uint32_t>(p[1]) << 8) |
           (static_cast<uint32_t>(p[2]) << 16) | (static_cast<uint32_t>(p[3]) << 24);
}

uint64_t ReadU64(const uint8_t* p) {
    uint64_t v = 0;
    for (int i = 0; i < 8; ++i) v |= static_cast<uint64_t>(p[i]) << (8 * i);
    return v;
}

uint32_t Fnv32(const uint8_t* p, size_t n) {
    uint32_t h = 2166136261u;
    for (size_t i = 0; i < n; ++i) {
        h ^= p[i];
        h *= 16777619u;
    }
    return h;
}

void Mix(uint64_t* h, const uint8_t* p, size_t n) {
    for (size_t i = 0; i < n; ++i) {
        *h ^= p[i];
        *h *= 1099511628211ull;
    }
}


bool SameFileState(const struct stat& a, const struct stat& b) {
    return a.st_dev == b.st_dev && a.st_ino == b.st_ino && a.st_size == b.st_size
        && a.st_mode == b.st_mode && a.st_uid == b.st_uid && a.st_gid == b.st_gid
        && a.st_mtim.tv_sec == b.st_mtim.tv_sec && a.st_mtim.tv_nsec == b.st_mtim.tv_nsec
        && a.st_ctim.tv_sec == b.st_ctim.tv_sec && a.st_ctim.tv_nsec == b.st_ctim.tv_nsec;
}
bool ReadAt(int fd, uint64_t offset, uint8_t* out, size_t length) {
    const uint64_t max = uint64_t(std::numeric_limits<off_t>::max());
    if (!out || !length || offset > max || uint64_t(length) > max - offset) return false;
    size_t done = 0;
    while (done < length) {
        const ssize_t n = pread(fd, out + done, length - done, off_t(offset + done));
        if (n < 0 && errno == EINTR) continue;
        if (n <= 0 || size_t(n) > length - done) return false;
        done += size_t(n);
    }
    return true;
}
bool PrivateDirectory(int fd) {
    struct stat st{};
    return fstat(fd, &st) == 0 && S_ISDIR(st.st_mode) && st.st_uid == geteuid()
        && !(st.st_mode & 0002) && (!(st.st_mode & 0020) || st.st_gid == getegid());
}
int OpenCacheDirectory(bool create) {
    // Cache is advisory. Never accept or publish patch locations in shared tmp/media.
    // A pinned directory FD owns open/unlink/rename; the final component cannot be a link.
    char path[160];
    const unsigned user = unsigned(geteuid()) / 100000u;
    const int n = snprintf(path, sizeof(path), "/data/user/%u/com.miui.home/files", user);
    if (n <= 0 || size_t(n) >= sizeof(path)) return -1;
    int fd = open(path, O_RDONLY | O_DIRECTORY | O_NOFOLLOW | O_CLOEXEC | O_NONBLOCK);
    if (fd < 0 && errno == ENOENT && create) {
        char* slash = strrchr(path, '/');
        if (!slash) return -1;
        *slash = '\0';
        const int parent = open(path, O_RDONLY | O_DIRECTORY | O_NOFOLLOW | O_CLOEXEC | O_NONBLOCK);
        if (parent < 0) return -1;
        if (PrivateDirectory(parent) && (mkdirat(parent, "files", 0700) == 0 || errno == EEXIST))
            fd = openat(parent, "files", O_RDONLY | O_DIRECTORY | O_NOFOLLOW | O_CLOEXEC | O_NONBLOCK);
        if (close(parent) != 0) { if (fd >= 0) close(fd); return -1; }
    }
    if (fd >= 0 && !PrivateDirectory(fd)) { close(fd); return -1; }
    return fd;
}
bool ReadWhole(int dir, uint8_t* buf, size_t cap, size_t* outLen, const char* name = kCacheName) {
    if (!buf || !cap || !outLen) return false;
    const int fd = openat(dir, name, O_RDONLY | O_NOFOLLOW | O_CLOEXEC | O_NONBLOCK);
    if (fd < 0) return false;
    struct stat before{}, after{}; bool ok = false;
    if (fstat(fd, &before) == 0 && S_ISREG(before.st_mode) && before.st_uid == geteuid()
        && !(before.st_mode & 0077) && before.st_size > 0 && uint64_t(before.st_size) <= cap) {
        const size_t want = size_t(before.st_size);
        uint8_t extra = 0;
        ok = ReadAt(fd, 0, buf, want);
        if (ok) {
            ssize_t n;
            do { n = pread(fd, &extra, 1, off_t(want)); } while (n < 0 && errno == EINTR);
            ok = n == 0 && fstat(fd, &after) == 0 && SameFileState(before, after);
        }
        if (ok) *outLen = want;
    }
    // Never retry close(EINTR): the descriptor may already have been released.
    return close(fd) == 0 && ok;
}
bool WriteWholeAtomic(int dir, const uint8_t* data, size_t len, const char* name = kCacheName) {
    if (!data || !len || len > kCacheMaxBytes) return false;
    static std::atomic<uint64_t> serial{0};
    char tmp[96]; int fd = -1;
    for (unsigned attempt = 0; attempt < 32; ++attempt) {
        const auto id = serial.fetch_add(1, std::memory_order_relaxed);
        const int n = snprintf(tmp, sizeof(tmp), ".hometweaks-sites.%lu.%llu.tmp",
            static_cast<unsigned long>(getpid()), static_cast<unsigned long long>(id));
        if (n <= 0 || size_t(n) >= sizeof(tmp)) return false;
        fd = openat(dir, tmp, O_WRONLY | O_CREAT | O_EXCL | O_NOFOLLOW | O_CLOEXEC | O_NONBLOCK, 0600);
        if (fd >= 0) break;
        if (errno != EEXIST) return false;
    }
    if (fd < 0) return false;
    bool ok = fchmod(fd, 0600) == 0;
    size_t done = 0;
    while (ok && done < len) {
        const ssize_t n = write(fd, data + done, len - done);
        if (n < 0 && errno == EINTR) continue;
        if (n <= 0 || size_t(n) > len - done) { ok = false; break; }
        done += size_t(n);
    }
    struct stat st{};
    ok = ok && fstat(fd, &st) == 0 && S_ISREG(st.st_mode) && st.st_uid == geteuid()
        && !(st.st_mode & 0077) && uint64_t(st.st_size) == len;
    if (ok) { int result; do { result = fsync(fd); } while (result < 0 && errno == EINTR); ok = result == 0; }
    if (close(fd) != 0) ok = false;
    if (ok) ok = renameat(dir, tmp, dir, name) == 0;
    if (!ok) { unlinkat(dir, tmp, 0); return false; }
    int result; do { result = fsync(dir); } while (result < 0 && errno == EINTR);
    // A directory-sync failure may follow a visible rename; report persistence as unconfirmed.
    return result == 0;
}
bool CacheSitesValid(const LocatedSites& sites) {
    if (!sites.AnyOk() || sites.noClearCount > kMaxNoClearSites || sites.foldGuardCount > kMaxFoldGuards
        || sites.iconSiteCount > kMaxIconSites) return false;
    for (size_t i = 0; i < kMaxIconFuncs; ++i)
        if (sites.iconFuncSiteBeg[i] > sites.iconSiteCount
            || sites.iconFuncSiteCount[i] > sites.iconSiteCount - sites.iconFuncSiteBeg[i]) return false;
    return true;
}
} // namespace

uint64_t ImageIdentity(const Image& image) {
    if (!CodeView(image).ExecutableRangesOk() || !image.path[0]
        || !memchr(image.path, '\0', sizeof(image.path)) || !image.sourceInode
        || !image.fileViewBytes || image.fileViewBytes > kMaxIdentityBytes
        || (!image.fromApkEntry && image.apkEntryOffset != 0)) return 0;
    const uint64_t origin = image.fromApkEntry ? image.apkEntryOffset : 0;
    const uint64_t max = uint64_t(std::numeric_limits<off_t>::max());
    if (origin > max || image.fileViewBytes > max - origin) return 0;
    const int fd = open(image.path, O_RDONLY | O_NOFOLLOW | O_CLOEXEC | O_NONBLOCK);
    if (fd < 0) return 0;
    const auto compute = [&]() -> uint64_t {
        struct stat before{}, after{};
        if (fstat(fd, &before) != 0 || !S_ISREG(before.st_mode) || before.st_size < 0
            || uint64_t(before.st_dev) != image.sourceDevice || uint64_t(before.st_ino) != image.sourceInode
            || origin + image.fileViewBytes > uint64_t(before.st_size)
            || (!image.fromApkEntry && image.fileViewBytes != uint64_t(before.st_size))) return 0;
        Elf64_Ehdr header{}, live{};
        if (image.fileViewBytes < sizeof(header) || !ReadAt(fd, origin, reinterpret_cast<uint8_t*>(&header), sizeof(header))
            || memcmp(header.e_ident, ELFMAG, SELFMAG) || header.e_ident[EI_CLASS] != ELFCLASS64
            || header.e_ident[EI_DATA] != ELFDATA2LSB || header.e_ident[EI_VERSION] != EV_CURRENT
            || header.e_type != ET_DYN || header.e_machine != EM_AARCH64 || header.e_version != EV_CURRENT
            || header.e_ehsize != sizeof(header) || header.e_phentsize != sizeof(Elf64_Phdr)
            || !header.e_phnum || header.e_phnum > 64 || header.e_phoff < sizeof(header) || header.e_phoff > 4096
            || !RangeInImage(image, image.base, sizeof(live))
            || !nhk::safe_read(image.base, std::as_writable_bytes(std::span(&live, 1)))
            || memcmp(&header, &live, sizeof(header))) return 0;
        const size_t bytes = size_t(header.e_phnum) * sizeof(Elf64_Phdr);
        if (header.e_phoff > image.fileViewBytes || bytes > image.fileViewBytes - header.e_phoff
            || header.e_phoff > UINTPTR_MAX - image.base) return 0;
        std::vector<uint8_t> table(bytes), liveTable(bytes);
        if (!ReadAt(fd, origin + header.e_phoff, table.data(), bytes)
            || !RangeInImage(image, image.base + header.e_phoff, bytes)
            || !nhk::safe_read(image.base + header.e_phoff, std::as_writable_bytes(std::span(liveTable)))
            || table != liveTable) return 0;
        std::vector<Elf64_Phdr> loads;
        for (size_t i = 0; i < header.e_phnum; ++i) {
            Elf64_Phdr p{}; memcpy(&p, table.data() + i * sizeof(p), sizeof(p));
            if (p.p_type != PT_LOAD || !p.p_memsz) continue;
            if (loads.size() >= 16 || !(p.p_flags & PF_R) || (p.p_flags & (PF_W | PF_X)) == (PF_W | PF_X)
                || p.p_filesz > p.p_memsz || p.p_offset > image.fileViewBytes
                || p.p_filesz > image.fileViewBytes - p.p_offset
                || p.p_vaddr > UINT32_MAX || p.p_memsz > UINT32_MAX - p.p_vaddr
                || p.p_vaddr + p.p_memsz > UINTPTR_MAX - image.base
                || (p.p_align > 1 && ((p.p_align & (p.p_align - 1))
                    || p.p_offset % p.p_align != p.p_vaddr % p.p_align))) return 0;
            for (const auto& prev : loads)
                if (p.p_vaddr < prev.p_vaddr + prev.p_memsz && prev.p_vaddr < p.p_vaddr + p.p_memsz) return 0;
            loads.push_back(p);
        }
        if (loads.empty()) return 0;
        for (size_t i = 0; i < image.segmentCount; ++i) {
            const auto& seg = image.segments[i]; bool owned = false;
            if (seg.begin < image.base || seg.end <= seg.begin) return 0;
            for (const auto& p : loads)
                if (seg.begin - image.base >= p.p_vaddr && seg.end - image.base <= p.p_vaddr + p.p_memsz
                    && bool(seg.flags & PF_X) == bool(p.p_flags & PF_X) && !(seg.flags & ~p.p_flags)) owned = true;
            if (!owned) return 0;
        }
        // Full immutable file-view content, not three 64-byte samples. A single FD
        // pins the inode; before/after stat detects normal in-place replacement.
        uint64_t h = 14695981039346656037ull;
        const uint8_t domain[] = {'H','T','S','2','I','D'};
        Mix(&h, domain, sizeof(domain));
        std::array<uint8_t, kIdentityChunk> chunk{};
        for (uint64_t done = 0; done < image.fileViewBytes;) {
            const size_t want = size_t(std::min<uint64_t>(chunk.size(), image.fileViewBytes - done));
            if (!ReadAt(fd, origin + done, chunk.data(), want)) return 0;
            Mix(&h, chunk.data(), want); done += want;
        }
        if (fstat(fd, &after) != 0 || !SameFileState(before, after)) return 0;
        return h;
    };
    const uint64_t result = compute();
    return close(fd) == 0 ? result : 0;
}

bool LoadSitesCache(uint64_t imageId, LocatedSites* out) {
    if (!imageId || !out) return false;
    const int dir = OpenCacheDirectory(false); if (dir < 0) return false;
    std::array<uint8_t, kCacheMaxBytes> buf{}; size_t len = 0;
    bool ok = ReadWhole(dir, buf.data(), buf.size(), &len);
    if (close(dir) != 0) ok = false;
    if (!ok || len < kHeaderBytes || memcmp(buf.data(), kCacheMagic, 4)
        || ReadU32(buf.data() + 4) != kCacheFormat || ReadU64(buf.data() + 12) != imageId) return false;
    const uint32_t payloadLen = ReadU32(buf.data() + 20);
    if (payloadLen != len - kHeaderBytes) return false;
    const uint8_t* payload = buf.data() + kHeaderBytes;
    if (Fnv32(payload, payloadLen) != ReadU32(buf.data() + 8)) return false;
    LocatedSites sites{};
    if (!ParseSites(payload, payloadLen, &sites) || !CacheSitesValid(sites)) return false;
    std::vector<uint8_t> canonical;
    if (!SerializeSites(sites, &canonical) || canonical.size() != payloadLen
        || memcmp(canonical.data(), payload, payloadLen)) return false;
    *out = sites; return true;
}
bool SaveSitesCache(uint64_t imageId, const LocatedSites& sites) {
    if (!imageId || !CacheSitesValid(sites)) return false;
    std::vector<uint8_t> payload;
    if (!SerializeSites(sites, &payload) || payload.size() > kCacheMaxBytes - kHeaderBytes) return false;
    std::vector<uint8_t> file; file.reserve(payload.size() + kHeaderBytes);
    file.insert(file.end(), kCacheMagic, kCacheMagic + 4);
    AppendU32(&file, kCacheFormat); AppendU32(&file, Fnv32(payload.data(), payload.size()));
    AppendU64(&file, imageId); AppendU32(&file, uint32_t(payload.size()));
    file.insert(file.end(), payload.begin(), payload.end());
    const int dir = OpenCacheDirectory(true); if (dir < 0) return false;
    bool ok = WriteWholeAtomic(dir, file.data(), file.size());
    if (close(dir) != 0) ok = false;
    if (ok) LOGI("站点缓存已写入私有目录（%zu 字节，指纹 %016llx）", file.size(),
        static_cast<unsigned long long>(imageId));
    return ok;
}
bool LoadSymbolCache(uint64_t imageId, std::vector<uint8_t>* out) {
    if (!imageId || !out) return false;
    const int dir = OpenCacheDirectory(false); if (dir < 0) return false;
    std::array<uint8_t, kCacheMaxBytes> buf{}; size_t len = 0;
    bool ok = ReadWhole(dir, buf.data(), buf.size(), &len, "hometweaks-symbols.bin");
    if (close(dir) != 0) ok = false;
    if (!ok || len < kHeaderBytes || memcmp(buf.data(), "HSY1", 4)
        || ReadU32(buf.data() + 4) != 1 || ReadU64(buf.data() + 12) != imageId
        || ReadU32(buf.data() + 20) != len - kHeaderBytes
        || Fnv32(buf.data() + kHeaderBytes, len - kHeaderBytes) != ReadU32(buf.data() + 8)) return false;
    out->assign(buf.begin() + kHeaderBytes, buf.begin() + len); return true;
}
bool SaveSymbolCache(uint64_t imageId, const std::vector<uint8_t>& payload) {
    if (!imageId || payload.empty() || payload.size() > kCacheMaxBytes - kHeaderBytes) return false;
    std::vector<uint8_t> file{'H','S','Y','1'};
    AppendU32(&file, 1); AppendU32(&file, Fnv32(payload.data(), payload.size()));
    AppendU64(&file, imageId); AppendU32(&file, uint32_t(payload.size()));
    file.insert(file.end(), payload.begin(), payload.end());
    const int dir = OpenCacheDirectory(true); if (dir < 0) return false;
    bool ok = WriteWholeAtomic(dir, file.data(), file.size(), "hometweaks-symbols.bin");
    if (close(dir) != 0) ok = false;
    return ok;
}
void DropSitesCache() {
    const int dir = OpenCacheDirectory(false); if (dir < 0) return;
    if (unlinkat(dir, kCacheName, 0) == 0) LOGI("已删除私有目录的失效站点缓存");
    close(dir);
}
}
