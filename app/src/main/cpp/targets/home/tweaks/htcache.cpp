// SPDX-License-Identifier: Apache-2.0
#include "htcache.h"
#include "common.h"

#include <elf.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

#include <vector>

namespace hometweaks {
namespace {

const char* const kCachePaths[] = {
        "/data/user/0/com.miui.home/files/hometweaks-sites.bin",
        "/data/local/tmp/hometweaks-sites.bin",
        "/storage/emulated/0/Android/media/com.miui.home.tweaks/hometweaks-sites.bin",
};
constexpr size_t kCachePathCount = sizeof(kCachePaths) / sizeof(kCachePaths[0]);

constexpr char kCacheMagic[4] = {'H', 'T', 'S', '1'};
constexpr uint32_t kCacheFormat = 1;
constexpr size_t kCacheMaxBytes = 8u * 1024u;
constexpr size_t kHeaderBytes = 4 + 4 + 4 + 8 + 4;

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

void MakeParentDirs(const char* path) {
    char buf[512];
    const size_t len = strlen(path);
    if (len == 0 || len >= sizeof(buf)) return;
    memcpy(buf, path, len + 1);
    char* slash = strrchr(buf, '/');
    if (slash == nullptr || slash == buf) return;
    *slash = '\0';
    for (char* p = buf + 1; *p != '\0'; ++p) {
        if (*p != '/') continue;
        *p = '\0';
        mkdir(buf, 0771);
        *p = '/';
    }
    mkdir(buf, 0771);
}

bool ReadWhole(const char* path, uint8_t* buf, size_t cap, size_t* outLen) {
    const int fd = open(path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return false;
    struct stat st {};
    bool ok = false;
    if (fstat(fd, &st) == 0 && S_ISREG(st.st_mode) && st.st_size > 0 &&
        static_cast<uint64_t>(st.st_size) <= cap) {
        size_t done = 0;
        const size_t want = static_cast<size_t>(st.st_size);
        while (done < want) {
            const ssize_t n = read(fd, buf + done, want - done);
            if (n <= 0) break;
            done += static_cast<size_t>(n);
        }
        ok = done == want;
        if (ok && outLen != nullptr) *outLen = want;
    }
    close(fd);
    return ok;
}

bool WriteWholeAtomic(const char* path, const uint8_t* data, size_t len) {
    MakeParentDirs(path);
    char tmp[600];
    const size_t plen = strlen(path);
    if (plen + 5 >= sizeof(tmp)) return false;
    memcpy(tmp, path, plen);
    memcpy(tmp + plen, ".tmp", 5);

    const int fd = open(tmp, O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0644);
    if (fd < 0) return false;
    size_t done = 0;
    while (done < len) {
        const ssize_t n = write(fd, data + done, len - done);
        if (n <= 0) break;
        done += static_cast<size_t>(n);
    }
    if (done == len) {
        if (fchmod(fd, 0644) != 0) {  }
        fsync(fd);
    }
    close(fd);
    if (done != len) {
        unlink(tmp);
        return false;
    }
    if (rename(tmp, path) != 0) {
        unlink(tmp);
        return false;
    }
    return true;
}

}

uint64_t ImageIdentity(const Image& image) {
    uint64_t h = 1469598103934665603ull;

    if (RangeInImage(image, image.base, sizeof(Elf64_Ehdr))) {
        const uint8_t* base = reinterpret_cast<const uint8_t*>(image.base);
        Mix(&h, base, sizeof(Elf64_Ehdr));
        const Elf64_Ehdr* eh = reinterpret_cast<const Elf64_Ehdr*>(base);
        const uint64_t phoff = eh->e_phoff;
        const uint32_t phnum = eh->e_phnum;
        const uint16_t phentsize = eh->e_phentsize;
        if (phnum > 0 && phnum <= 64 && phentsize == sizeof(Elf64_Phdr)) {
            const uintptr_t at = image.base + static_cast<uintptr_t>(phoff);
            const size_t bytes = static_cast<size_t>(phnum) * sizeof(Elf64_Phdr);
            if (RangeInImage(image, at, bytes)) {
                Mix(&h, reinterpret_cast<const uint8_t*>(at), bytes);
            }
        }
    }

    constexpr size_t kSample = 64;
    for (size_t s = 0; s < image.segmentCount; ++s) {
        const Segment& seg = image.segments[s];
        if ((seg.flags & 0x1u) == 0) continue;
        const uintptr_t bytes = seg.end - seg.begin;
        if (bytes < kSample * 4) continue;
        const uintptr_t offsets[3] = {0, (bytes - kSample) / 2, bytes - kSample};
        for (int i = 0; i < 3; ++i) {
            Mix(&h, reinterpret_cast<const uint8_t*>(seg.begin + offsets[i]), kSample);
        }
    }
    return h;
}

bool LoadSitesCache(uint64_t imageId, LocatedSites* out) {
    if (out == nullptr) return false;
    static uint8_t buf[kCacheMaxBytes];
    for (size_t i = 0; i < kCachePathCount; ++i) {
        size_t len = 0;
        if (!ReadWhole(kCachePaths[i], buf, sizeof(buf), &len)) continue;
        if (len < kHeaderBytes) continue;
        if (memcmp(buf, kCacheMagic, 4) != 0) continue;
        const uint32_t format = ReadU32(buf + 4);
        if (format != kCacheFormat) continue;
        const uint32_t crc = ReadU32(buf + 8);
        const uint64_t cachedId = ReadU64(buf + 12);
        const uint32_t payloadLen = ReadU32(buf + 20);
        if (payloadLen != len - kHeaderBytes) continue;
        const uint8_t* payload = buf + kHeaderBytes;
        if (Fnv32(payload, payloadLen) != crc) continue;
        if (cachedId != imageId) {
            LOGI("站点缓存 %s 属于另一个桌面版本（指纹 %016llx ≠ %016llx），忽略并重扫",
                 kCachePaths[i], static_cast<unsigned long long>(cachedId),
                 static_cast<unsigned long long>(imageId));
            continue;
        }
        LocatedSites sites;
        if (!ParseSites(payload, payloadLen, &sites)) continue;
        *out = sites;
        return true;
    }
    return false;
}

bool SaveSitesCache(uint64_t imageId, const LocatedSites& sites) {
    std::vector<uint8_t> payload;
    if (!SerializeSites(sites, &payload)) return false;
    if (payload.size() + kHeaderBytes > kCacheMaxBytes) return false;

    std::vector<uint8_t> file;
    file.reserve(payload.size() + kHeaderBytes);
    file.insert(file.end(), kCacheMagic, kCacheMagic + 4);
    AppendU32(&file, kCacheFormat);
    AppendU32(&file, Fnv32(payload.data(), payload.size()));
    AppendU64(&file, imageId);
    AppendU32(&file, static_cast<uint32_t>(payload.size()));
    file.insert(file.end(), payload.begin(), payload.end());

    for (size_t i = 0; i < kCachePathCount; ++i) {
        if (WriteWholeAtomic(kCachePaths[i], file.data(), file.size())) {
            LOGI("站点缓存已写入 %s（%zu 字节，指纹 %016llx）", kCachePaths[i], file.size(),
                 static_cast<unsigned long long>(imageId));
            return true;
        }
    }
    LOGW("站点缓存写入失败（所有候选路径都不可写），下次桌面启动仍需重新扫描");
    return false;
}

void DropSitesCache() {
    for (size_t i = 0; i < kCachePathCount; ++i) {
        if (unlink(kCachePaths[i]) == 0) {
            LOGI("已删除失效的站点缓存 %s", kCachePaths[i]);
        }
    }
}

}
