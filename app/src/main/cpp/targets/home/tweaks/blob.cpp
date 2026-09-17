// SPDX-License-Identifier: Apache-2.0
#include "blob.h"

#include <fcntl.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

namespace hometweaks {
namespace {

inline uint32_t ReadU32(const uint8_t* p) {
    return static_cast<uint32_t>(p[0]) | (static_cast<uint32_t>(p[1]) << 8) |
           (static_cast<uint32_t>(p[2]) << 16) | (static_cast<uint32_t>(p[3]) << 24);
}

inline uint32_t ClampU32(uint32_t v, uint32_t lo, uint32_t hi) {
    if (v < lo) return lo;
    if (v > hi) return hi;
    return v;
}

void NormalizeGrid(uint32_t lo, uint32_t hi, uint32_t defMajor, uint32_t defMinor,
                   uint32_t* major, uint32_t* minor) {
    uint32_t a = ClampU32(*major, lo, hi);
    uint32_t b = ClampU32(*minor, lo, hi);
    if (a < b) {
        const uint32_t t = a;
        a = b;
        b = t;
    }
    if (a == b) {
        a = defMajor;
        b = defMinor;
    }
    *major = a;
    *minor = b;
}

}

bool ParseConfigBlob(const uint8_t* data, size_t size, Config* out) {
    if (data == nullptr || out == nullptr) return false;
    if (size < 20) return false;
    if (memcmp(data, "HTWK", 4) != 0) return false;
    const uint32_t format = ReadU32(data + 4);
    if (format != kBlobFormatV2 && format != kBlobFormatV3 && format != kBlobFormatV4 &&
        format != kBlobFormatV5 && format != kBlobFormatV6 && format != kBlobFormatV7) {
        return false;
    }

    const uint32_t flags = ReadU32(data + 8);
    const uint32_t count = ReadU32(data + 12);
    if (count > 32) return false;
    size_t need = 16u + count * 4u + 4u;
    if (format >= kBlobFormatV3) need += 5u * 4u;
    if (format >= kBlobFormatV4) need += 1u * 4u;
    if (format >= kBlobFormatV5) need += 1u * 4u;
    if (format == kBlobFormatV6) need += 1u * 4u;
    if (size < need) return false;

    out->flags = flags;
    out->enabled.clear();
    out->enabled.reserve(count);
    for (uint32_t i = 0; i < count; ++i) {
        out->enabled.push_back(ReadU32(data + 16 + i * 4));
    }
    const size_t base = 16u + count * 4u;
    out->folderCols = ClampU32(ReadU32(data + base), kFolderColsMin, kFolderColsMax);

    out->padMajor = kDefaultPadMajor;
    out->padMinor = kDefaultPadMinor;
    out->phoneCols = kDefaultPhoneCols;
    out->phoneRows = kPhoneRowsAuto;
    out->foldMajor = kDefaultFoldMajor;
    out->foldMinor = kDefaultFoldMinor;
    out->iconScaleCode = kIconScaleCodeDefault;

    if (format >= kBlobFormatV3) {
        uint32_t padMajor = ReadU32(data + base + 4);
        uint32_t padMinor = ReadU32(data + base + 8);
        NormalizeGrid(kPadGridMin, kPadGridMax, kDefaultPadMajor, kDefaultPadMinor,
                      &padMajor, &padMinor);
        out->padMajor = padMajor;
        out->padMinor = padMinor;
        out->phoneCols = ClampU32(ReadU32(data + base + 12), kPhoneColsMin, kPhoneColsMax);
        uint32_t foldMajor = ReadU32(data + base + 16);
        uint32_t foldMinor = ReadU32(data + base + 20);
        NormalizeGrid(kPadGridMin, kPadGridMax, kDefaultFoldMajor, kDefaultFoldMinor,
                      &foldMajor, &foldMinor);
        out->foldMajor = foldMajor;
        out->foldMinor = foldMinor;
    }
    if (format >= kBlobFormatV4) {
        const uint32_t rows = ReadU32(data + base + 24);
        if (rows == kPhoneRowsAuto || (rows >= kPhoneRowsMin && rows <= kPhoneRowsMax)) {
            out->phoneRows = rows;
        } else {
            out->phoneRows = kPhoneRowsAuto;
        }
    }
    if (format >= kBlobFormatV5) {
        const uint32_t code = ReadU32(data + base + 28);
        out->iconScaleCode = IconScaleCodeValid(code) ? code : kIconScaleCodeDefault;
    }
    return true;
}

double IconScaleValueFromCode(uint32_t code) {
    const uint32_t b = (code >> 6) & 1u;
    const uint32_t exp = ((1u - b) << 10) | ((b != 0u ? 255u : 0u) << 2) | ((code >> 4) & 3u);
    const uint64_t bits = (static_cast<uint64_t>((code >> 7) & 1u) << 63) |
                          (static_cast<uint64_t>(exp) << 52) |
                          (static_cast<uint64_t>(code & 15u) << 48);
    double v = 0.0;
    memcpy(&v, &bits, sizeof(v));
    return v;
}

size_t ConfigCandidatePaths(const char** out, size_t maxCount) {
    static const char* kPaths[] = {
            kLocalConfigPath,
            "/data/local/tmp/hometweaks.bin",
            "/data/adb/hometweaks.bin",
            "/sdcard/Android/media/com.miui.home.tweaks/hometweaks.bin",
            "/storage/emulated/0/Android/media/com.miui.home.tweaks/hometweaks.bin",
            "/sdcard/Download/HomeTweaks/hometweaks.bin",
            "/storage/emulated/0/Download/HomeTweaks/hometweaks.bin",
            "/sdcard/hometweaks.bin",
    };
    const size_t total = sizeof(kPaths) / sizeof(kPaths[0]);
    const size_t n = total < maxCount ? total : maxCount;
    for (size_t i = 0; i < n; ++i) out[i] = kPaths[i];
    return n;
}

size_t SitePackPaths(const char** out, size_t maxCount) {
    static const char* kPaths[] = {
            "/data/user/0/com.miui.home/files/hometweaks.sites",
            "/data/local/tmp/hometweaks.sites",
            "/data/adb/hometweaks.sites",
            "/sdcard/Android/media/com.miui.home.tweaks/hometweaks.sites",
            "/storage/emulated/0/Android/media/com.miui.home.tweaks/hometweaks.sites",
    };
    const size_t total = sizeof(kPaths) / sizeof(kPaths[0]);
    const size_t n = total < maxCount ? total : maxCount;
    for (size_t i = 0; i < n; ++i) out[i] = kPaths[i];
    return n;
}

constexpr size_t kFastPathCount = kFastConfigPaths;

/*
 * stat()-only election over the candidate paths, mirroring the "newest wins" rule of
 * LoadConfigBlobLimited so the caller can compare fingerprints without reading anything. The
 * timestamp is assembled exactly the way ReadWholeFile assembles it, so the two are comparable
 * field by field.
 */
bool NewestConfigCandidate(char* usedPath, size_t pathCap, uint64_t* outMtime, size_t maxPaths) {
    const char* paths[16];
    const size_t total = ConfigCandidatePaths(paths, 16);
    const size_t count = total < maxPaths ? total : maxPaths;

    const char* bestPath = nullptr;
    uint64_t bestMtime = 0;
    for (size_t i = 0; i < count; ++i) {
        struct stat st {};
        if (stat(paths[i], &st) != 0 || !S_ISREG(st.st_mode)) continue;
        const uint64_t mtime = static_cast<uint64_t>(st.st_mtime) * 1000000ull +
                               static_cast<uint64_t>(st.st_mtim.tv_nsec / 1000);
        if (bestPath != nullptr && mtime <= bestMtime) continue;
        bestPath = paths[i];
        bestMtime = mtime;
    }
    if (bestPath == nullptr) return false;

    if (usedPath != nullptr && pathCap > 0) {
        const size_t len = strlen(bestPath);
        const size_t n = len < pathCap - 1 ? len : pathCap - 1;
        memcpy(usedPath, bestPath, n);
        usedPath[n] = '\0';
    }
    if (outMtime != nullptr) *outMtime = bestMtime;
    return true;
}

bool LoadConfigBlobLimited(Config* out, std::vector<uint8_t>* bytesOut, char* usedPath,
                           size_t pathCap, uint64_t* outMtime, size_t maxPaths) {
    const char* paths[16];
    const size_t total = ConfigCandidatePaths(paths, 16);
    const size_t count = total < maxPaths ? total : maxPaths;

    Config best;
    std::vector<uint8_t> bestBytes;
    uint64_t bestMtime = 0;
    const char* bestPath = nullptr;
    bool have = false;

    std::vector<uint8_t> data;
    for (size_t i = 0; i < count; ++i) {
        uint64_t mtime = 0;
        data.clear();
        Config cfg;
        if (!ReadWholeFile(paths[i], &data, &mtime)) continue;
        if (!ParseConfigBlob(data.data(), data.size(), &cfg)) {
            LOGW("配置 %s 解析失败，跳过", paths[i]);
            continue;
        }
        if (!have || mtime > bestMtime) {
            have = true;
            bestMtime = mtime;
            best = cfg;
            bestBytes = data;
            bestPath = paths[i];
        }
    }
    if (!have) return false;

    *out = best;
    if (bytesOut != nullptr) *bytesOut = bestBytes;
    if (usedPath != nullptr && pathCap > 0 && bestPath != nullptr) {
        const size_t len = strlen(bestPath);
        const size_t n = len < pathCap - 1 ? len : pathCap - 1;
        memcpy(usedPath, bestPath, n);
        usedPath[n] = '\0';
    }
    if (outMtime != nullptr) *outMtime = bestMtime;
    return true;
}

bool ReadWholeFile(const char* path, std::vector<uint8_t>* out, uint64_t* outMtime) {
    const int fd = open(path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return false;
    struct stat st {};
    bool ok = false;
    if (fstat(fd, &st) == 0 && S_ISREG(st.st_mode) && st.st_size >= 20 &&
        static_cast<uint64_t>(st.st_size) <= kMaxBlobBytes) {
        out->resize(static_cast<size_t>(st.st_size));
        size_t done = 0;
        while (done < out->size()) {
            const ssize_t n = read(fd, out->data() + done, out->size() - done);
            if (n <= 0) break;
            done += static_cast<size_t>(n);
        }
        ok = done == out->size();
        if (ok && outMtime != nullptr) {
            *outMtime = static_cast<uint64_t>(st.st_mtime) * 1000000ull +
                        static_cast<uint64_t>(st.st_mtim.tv_nsec / 1000);
        }
    }
    close(fd);
    if (!ok) out->clear();
    return ok;
}

bool LoadConfigBlob(Config* out, std::vector<uint8_t>* bytesOut, char* usedPath, size_t pathCap,
                    uint64_t* outMtime) {
    return LoadConfigBlobLimited(out, bytesOut, usedPath, pathCap, outMtime, kAllConfigPaths);
}

bool LoadConfigBlobFast(Config* out, std::vector<uint8_t>* bytesOut, char* usedPath,
                        size_t pathCap, uint64_t* outMtime) {
    return LoadConfigBlobLimited(out, bytesOut, usedPath, pathCap, outMtime, kFastPathCount);
}

namespace {

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

}

bool WriteWholeFileAtomic(const char* path, const uint8_t* data, size_t size) {
    if (path == nullptr || data == nullptr || size == 0) return false;
    MakeParentDirs(path);
    char tmp[600];
    const size_t plen = strlen(path);
    if (plen + 5 >= sizeof(tmp)) return false;
    memcpy(tmp, path, plen);
    memcpy(tmp + plen, ".tmp", 5);

    const int fd = open(tmp, O_WRONLY | O_CREAT | O_TRUNC | O_CLOEXEC, 0644);
    if (fd < 0) return false;
    size_t done = 0;
    while (done < size) {
        const ssize_t n = write(fd, data + done, size - done);
        if (n <= 0) break;
        done += static_cast<size_t>(n);
    }
    if (done == size && fchmod(fd, 0644) != 0) {
    }
    close(fd);
    if (done != size) {
        unlink(tmp);
        return false;
    }
    if (rename(tmp, path) != 0) {
        unlink(tmp);
        return false;
    }
    return true;
}

bool MirrorConfigToPath(const char* path, const uint8_t* data, size_t size,
                        uint64_t sourceMtime) {
    if (path == nullptr || data == nullptr || size == 0) return false;
    std::vector<uint8_t> existing;
    uint64_t mtime = 0;
    if (ReadWholeFile(path, &existing, &mtime) && existing.size() == size &&
        memcmp(existing.data(), data, size) == 0) {
        return true;
    }
    if (!WriteWholeFileAtomic(path, data, size)) return false;

    if (sourceMtime != 0) {
        struct timespec ts[2];
        ts[0].tv_sec = static_cast<time_t>(sourceMtime / 1000000ull);
        ts[0].tv_nsec = static_cast<long>((sourceMtime % 1000000ull) * 1000ull);
        ts[1] = ts[0];
        utimensat(AT_FDCWD, path, ts, 0);
    }
    return true;
}

}
