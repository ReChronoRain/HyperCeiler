// SPDX-License-Identifier: Apache-2.0
#include "image.h"

#include "common.h"

// The launcher's Dart image is a stored entry inside its own APK; the helper below is this
// project's already-verified zip entry locator (it validates the stored method and the bounds).
#include "nativehook/native_image.h"

#include <elf.h>
#include <fcntl.h>
#include <link.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

namespace hometweaks {
namespace {

struct SearchRequest {
    const char* basename;
    Image result;
    bool found;
    int matches;
};

int CollectCallback(struct dl_phdr_info* info, size_t, void* opaque) {
    auto* req = static_cast<SearchRequest*>(opaque);
    if (info == nullptr || info->dlpi_name == nullptr) return 0;
    const char* slash = strrchr(info->dlpi_name, '/');
    const char* base = slash == nullptr ? info->dlpi_name : slash + 1;
    if (strcmp(base, req->basename) != 0) return 0;
    ++req->matches;
    if (req->found) return 0;
    Image& image = req->result;
    memset(&image, 0, sizeof(image));
    image.base = static_cast<uintptr_t>(info->dlpi_addr);
    image.fromApkEntry = false;
    const size_t pathLen = strlen(info->dlpi_name);
    if (pathLen >= sizeof(image.path)) return 0;
    memcpy(image.path, info->dlpi_name, pathLen + 1);
    /*
     * The linker reports a synthetic "…base.apk!/lib/arm64-v8a/libapp.so" for an image that is
     * mapped straight out of the APK. That path cannot be opened, and every later read - section
     * headers, .gnu_debugdata, and the original words of a patch site - has to be taken from the
     * APK at the entry's own offset. Recording the split here is what makes those reads land on the
     * right bytes; leaving it unset is why the symbol table looked unavailable.
     */
    if (const char* bang = strchr(image.path, '!'); bang != nullptr && bang[1] != '\0') {
        const std::string apkPath(image.path, static_cast<size_t>(bang - image.path));
        // The entry inside the archive is named without the separator's own leading slash
        // ("lib/arm64-v8a/libapp.so"), while the synthetic path writes it as "!/lib/...".
        const char* entry = bang + 1;
        while (*entry == '/') ++entry;
        if (const auto stored = nhk::zip_stored_entry(apkPath, entry)) {
            if (apkPath.size() < sizeof(image.path)) {
                memcpy(image.path, apkPath.c_str(), apkPath.size() + 1);
                image.apkEntryOffset = stored->first;
                image.fromApkEntry = true;
                LOGI("镜像来自 APK 内嵌条目：%s 起始 %#llx 大小 %llu", entry,
                     static_cast<unsigned long long>(stored->first),
                     static_cast<unsigned long long>(stored->second));
            }
        } else {
            LOGW("APK 内嵌条目 %s 在 %s 里找不到（符号表与补丁原值将无法读取）", entry,
                 apkPath.c_str());
        }
    }
    for (size_t i = 0; i < info->dlpi_phnum && image.segmentCount < 16; ++i) {
        const ElfW(Phdr)& ph = info->dlpi_phdr[i];
        if (ph.p_type != PT_LOAD || ph.p_memsz == 0) continue;
        image.segments[image.segmentCount++] = {
                image.base + static_cast<uintptr_t>(ph.p_vaddr),
                image.base + static_cast<uintptr_t>(ph.p_vaddr + ph.p_memsz),
                ph.p_flags};
    }
    req->found = image.segmentCount > 0;
    return 0;
}

struct ApkMapLine {
    uintptr_t begin;
    uintptr_t end;
    uint64_t fileOff;
    char path[256];
};

bool ParseMapLine(const char* line, ApkMapLine* out) {
    unsigned long long b = 0, e = 0, off = 0, ino = 0;
    unsigned int maj = 0, min = 0;
    char perms[8] = {0};
    int consumed = 0;
    if (sscanf(line, "%llx-%llx %7s %llx %x:%x %llu %n", &b, &e, perms, &off, &maj, &min,
               &ino, &consumed) < 7) {
        return false;
    }
    out->begin = static_cast<uintptr_t>(b);
    out->end = static_cast<uintptr_t>(e);
    out->fileOff = static_cast<uint64_t>(off);
    out->path[0] = '\0';
    const char* p = line + consumed;
    while (*p == ' ' || *p == '\t') ++p;
    if (*p != '\0' && *p != '\n') {
        size_t n = 0;
        while (p[n] != '\0' && p[n] != '\n' && n < sizeof(out->path) - 1) {
            out->path[n] = p[n];
            ++n;
        }
        out->path[n] = '\0';
    }
    return true;
}

bool ZipEntryNameBefore(const char* apkPath, uint64_t dataOff, char* nameOut, size_t cap) {
    if (apkPath == nullptr || dataOff < 64) return false;
    constexpr size_t kWindow = 2048;
    const uint64_t start = dataOff > kWindow ? dataOff - kWindow : 0;
    const size_t len = static_cast<size_t>(dataOff - start);
    if (len < 30) return false;

    static uint8_t win[kWindow];
    const int fd = open(apkPath, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return false;
    const ssize_t got = pread(fd, win, len, static_cast<off_t>(start));
    close(fd);
    if (got != static_cast<ssize_t>(len)) return false;

    for (size_t i = len; i >= 4; --i) {
        const size_t h = i - 4;
        if (win[h] != 0x50 || win[h + 1] != 0x4B || win[h + 2] != 0x03 || win[h + 3] != 0x04) {
            continue;
        }
        if (h + 30 > len) continue;
        const uint16_t nameLen =
                static_cast<uint16_t>(win[h + 26] | (static_cast<uint32_t>(win[h + 27]) << 8));
        const uint16_t extraLen =
                static_cast<uint16_t>(win[h + 28] | (static_cast<uint32_t>(win[h + 29]) << 8));
        if (static_cast<uint64_t>(h) + start + 30u + nameLen + extraLen != dataOff) continue;
        if (nameLen == 0 || nameLen >= cap || h + 30 + nameLen > len) continue;
        memcpy(nameOut, win + h + 30, nameLen);
        nameOut[nameLen] = '\0';
        return true;
    }
    return false;
}

bool ImageFromElfAt(uintptr_t base, const char* path, Image* out) {
    if (base == 0 || path == nullptr || out == nullptr) return false;
    memset(out, 0, sizeof(*out));
    out->base = base;
    const size_t pl = strlen(path);
    if (pl >= sizeof(out->path)) return false;
    memcpy(out->path, path, pl + 1);
    out->fromApkEntry = true;

    const Elf64_Ehdr* eh = reinterpret_cast<const Elf64_Ehdr*>(base);
    if (memcmp(eh->e_ident, ELFMAG, SELFMAG) != 0) return false;
    if (eh->e_ident[EI_CLASS] != ELFCLASS64) return false;
    if (eh->e_ident[EI_DATA] != ELFDATA2LSB) return false;
    if (eh->e_phnum == 0 || eh->e_phnum > 64) return false;

    const Elf64_Phdr* ph = reinterpret_cast<const Elf64_Phdr*>(base + eh->e_phoff);
    for (int i = 0; i < eh->e_phnum && out->segmentCount < 16; ++i) {
        if (ph[i].p_type != PT_LOAD || ph[i].p_memsz == 0) continue;
        out->segments[out->segmentCount++] = {
                base + static_cast<uintptr_t>(ph[i].p_vaddr),
                base + static_cast<uintptr_t>(ph[i].p_vaddr + ph[i].p_memsz),
                static_cast<uint32_t>(ph[i].p_flags)};
    }
    return out->segmentCount > 0;
}

size_t ReadMaps(char* buf, size_t cap) {
    const int fd = open("/proc/self/maps", O_RDONLY | O_CLOEXEC);
    if (fd < 0) return 0;
    size_t total = 0;
    while (total < cap - 1) {
        const ssize_t n = read(fd, buf + total, cap - 1 - total);
        if (n <= 0) break;
        total += static_cast<size_t>(n);
    }
    close(fd);
    buf[total] = '\0';
    return total;
}

}

bool FindImageByName(const char* basename, Image* out) {
    if (basename == nullptr || out == nullptr) return false;
    SearchRequest req{basename, {}, false, 0};
    dl_iterate_phdr(CollectCallback, &req);
    if (!req.found) return false;
    *out = req.result;
    return true;
}

bool FindImageFromApkMaps(const char* apkMarker, const char* libName, Image* out) {
    if (apkMarker == nullptr || libName == nullptr || out == nullptr) return false;

    constexpr size_t kMapsCap = 512 * 1024;
    constexpr size_t kMaxLines = 512;
    constexpr size_t kMaxOffsets = 64;

    static char* buf = nullptr;
    static ApkMapLine lines[kMaxLines];
    if (buf == nullptr) {
        buf = static_cast<char*>(malloc(kMapsCap));
        if (buf == nullptr) return false;
    }
    if (ReadMaps(buf, kMapsCap) == 0) return false;

    size_t lineCount = 0;
    uint64_t offsets[kMaxOffsets];
    size_t offsetCount = 0;
    char apkPath[256] = {0};

    char* p = buf;
    while (p != nullptr && *p != '\0' && lineCount < kMaxLines) {
        char* nl = strchr(p, '\n');
        if (nl != nullptr) *nl = '\0';
        ApkMapLine ml{};
        if (ParseMapLine(p, &ml) && ml.path[0] != '\0' &&
            strstr(ml.path, apkMarker) != nullptr) {
            lines[lineCount++] = ml;
            if (apkPath[0] == '\0' && strlen(ml.path) < sizeof(apkPath)) {
                memcpy(apkPath, ml.path, strlen(ml.path) + 1);
            }
            bool seen = false;
            for (size_t i = 0; i < offsetCount; ++i) {
                if (offsets[i] == ml.fileOff) {
                    seen = true;
                    break;
                }
            }
            if (!seen && offsetCount < kMaxOffsets) offsets[offsetCount++] = ml.fileOff;
        }
        if (nl == nullptr) break;
        p = nl + 1;
    }
    if (lineCount == 0 || apkPath[0] == '\0' || offsetCount == 0) return false;

    for (size_t i = 0; i < offsetCount; ++i) {
        size_t best = i;
        for (size_t j = i + 1; j < offsetCount; ++j) {
            if (offsets[j] < offsets[best]) best = j;
        }
        if (best != i) {
            const uint64_t t = offsets[i];
            offsets[i] = offsets[best];
            offsets[best] = t;
        }

        char name[256] = {0};
        if (!ZipEntryNameBefore(apkPath, offsets[i], name, sizeof(name))) continue;
        const char* slash = strrchr(name, '/');
        const char* entryBase = slash == nullptr ? name : slash + 1;
        if (strcmp(entryBase, libName) != 0) continue;

        for (size_t j = 0; j < lineCount; ++j) {
            if (lines[j].fileOff != offsets[i]) continue;
            if (ImageFromElfAt(lines[j].begin, lines[j].path, out)) {
                out->apkEntryOffset = offsets[i];
                return true;
            }
        }
    }
    return false;
}

bool RangeInImage(const Image& image, uintptr_t address, size_t length) {
    if (length == 0) return false;
    const uintptr_t end = address + length;
    if (end < address) return false;
    for (size_t i = 0; i < image.segmentCount; ++i) {
        const Segment& s = image.segments[i];
        if (address >= s.begin && end <= s.end) return true;
    }
    return false;
}

bool ReadImageFile(const Image& image, uint64_t offset, void* out, size_t length) {
    if (out == nullptr || length == 0 || image.path[0] == '\0') return false;
    const uint64_t fileOffset =
            image.fromApkEntry ? image.apkEntryOffset + offset : offset;
    const int fd = open(image.path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return false;
    uint8_t* dst = static_cast<uint8_t*>(out);
    size_t done = 0;
    bool ok = true;
    while (done < length) {
        const ssize_t n = pread(fd, dst + done, length - done,
                                static_cast<off_t>(fileOffset + done));
        if (n <= 0) {
            ok = false;
            break;
        }
        done += static_cast<size_t>(n);
    }
    close(fd);
    return ok;
}

namespace {

struct DumpRequest {
    int maxEntries;
    int seen;
};

int DumpCallback(struct dl_phdr_info* info, size_t, void* opaque) {
    auto* req = static_cast<DumpRequest*>(opaque);
    if (info == nullptr) return 0;
    if (req->seen >= req->maxEntries) return 1;
    ++req->seen;
    const char* name = info->dlpi_name != nullptr ? info->dlpi_name : "(空)";
    int loads = 0;
    for (size_t i = 0; i < info->dlpi_phnum; ++i) {
        if (info->dlpi_phdr[i].p_type == PT_LOAD) ++loads;
    }
    LOGI("  [phdr] base=%#lx PT_LOAD=%d name=%s",
         static_cast<unsigned long>(info->dlpi_addr), loads, name);
    return 0;
}

}

void DumpLoadedLibraries(int maxEntries) {
    DumpRequest req{maxEntries, 0};
    LOGI("dl_iterate_phdr 可见的库（最多 %d 条）：", maxEntries);
    dl_iterate_phdr(DumpCallback, &req);
    LOGI("dl_iterate_phdr 遍历结束，共 %d 条", req.seen);
}

void DumpApkMaps(int maxEntries) {
    constexpr size_t kMapsCap = 512 * 1024;
    static char* buf = nullptr;
    if (buf == nullptr) {
        buf = static_cast<char*>(malloc(kMapsCap));
        if (buf == nullptr) return;
    }
    const size_t total = ReadMaps(buf, kMapsCap);
    if (total == 0) {
        LOGW("/proc/self/maps 读不到");
        return;
    }
    LOGI("/proc/self/maps 里含 .apk 的映射（最多 %d 条）：", maxEntries);
    int shown = 0;
    char* p = buf;
    while (p != nullptr && *p != '\0' && shown < maxEntries) {
        char* nl = strchr(p, '\n');
        if (nl != nullptr) *nl = '\0';
        if (strstr(p, ".apk") != nullptr) {
            LOGI("  [maps] %s", p);
            ++shown;
        }
        if (nl == nullptr) break;
        p = nl + 1;
    }
    LOGI("maps 中 .apk 映射共显示 %d 条", shown);
}

void DumpImage(const Image& image) {
    LOGI("镜像 base=%#lx 段数=%zu path=%s", static_cast<unsigned long>(image.base),
         image.segmentCount, image.path);
    for (size_t i = 0; i < image.segmentCount; ++i) {
        const Segment& s = image.segments[i];
        LOGI("  段%zu %#lx - %#lx flags=%#x 大小=%#lx", i,
             static_cast<unsigned long>(s.begin), static_cast<unsigned long>(s.end), s.flags,
             static_cast<unsigned long>(s.end - s.begin));
    }
}

}
