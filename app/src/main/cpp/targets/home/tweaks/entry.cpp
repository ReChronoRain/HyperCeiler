// SPDX-License-Identifier: Apache-2.0
#include "blob.h"
#include "common.h"
#include "ht_plan.h"
#include "htcache.h"
#include "image.h"
#include "scanner.h"
#include "symtab.h"
#include "nativehook/code_word_write.h"
#include "nativehook/memory_io.h"
#include <array>
#include <sys/sysmacros.h>

#include <dlfcn.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/system_properties.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/stat.h>
#include <time.h>
#include <unistd.h>

#include <atomic>
#include <cerrno>
#include <chrono>
#include <condition_variable>
#include <mutex>
#include <string>
#include <vector>

namespace hometweaks {
namespace {

constexpr const char* kModuleVersion = "2.3";

/*
 * Debug property switch. Read once on first use and cached: __system_property_get is a cheap
 * shared-memory read but it is not free, and this is asked on a path that runs on every pass.
 */
constexpr const char* kPropConfigBlob = "debug.hyperceiler.hometweaks.blob";

bool DebugPropertyEnabled(const char* name) {
    char value[PROP_VALUE_MAX] = {};
    return __system_property_get(name, value) > 0 && value[0] == '1';
}

const NativeAPIEntries* g_entries = nullptr;
std::mutex g_wakeMutex;
std::condition_variable g_wakeCv;
bool g_wakeFlag = false;

std::atomic<uint32_t> g_loadedCount{0};
/* The desktop is forked from the spawner, so several entry points ask for a start; only the first
   one may create the monitoring thread. */
std::atomic<bool> g_tweaksStarted{false};

struct AppliedPatch {
    uintptr_t address;
    uint32_t oldWord;
    uint32_t newWord;
    bool reverting = false; // A failed restore must never be repaired back to the old override.
    nhk::WordWriteJournal recovery{};
};

struct PatchStats {
    size_t total = 0;
    size_t written = 0;
    size_t already = 0;
    size_t outOfRange = 0;
    size_t mismatch = 0;
    size_t failed = 0;
};

struct State {
    Image image{};
    bool imageFound = false;
    uint64_t imageId = 0;
    bool identityTried = false; // Advisory cache: one full-file attempt per discovered process image.

    Config config;
    bool configLoaded = false;
    bool configFromBaked = false;
    std::vector<uint8_t> configBytes;
    char configPath[512]{};
    uint64_t configMtime = 0;
    /*
     * Fingerprint of the candidate set as observed at the last read: the newest readable candidate
     * and its mtime. RefreshConfigLocked runs on every monitor pass, and the steady state is "no
     * file changed", so this is what lets the read be skipped outright. It describes the candidate
     * set rather than the file that won the election, because a newest-but-unparsable candidate
     * would otherwise never match and the skip could never fire.
     */
    char configProbePath[512]{};
    uint64_t configProbeMtime = 0;
    bool configProbeValid = false;

    LocatedSites sites;
    uint32_t attemptedMask = 0;
    bool bakedTried = false;
    bool packTried = false;
    bool cacheTried = false;
    bool squareTried = false;

    std::vector<AppliedPatch> applied;
    bool repatchNeeded = true;
    bool restoreBlocked = false;
    bool writeRetryPending = false;

    char imageHow[32]{};
    char siteSource[96]{};
    PlanResult lastPlan;
    PatchStats patchStats;
};

State g_state;
std::mutex g_workMutex;
std::atomic<bool> g_imageKnown{false};

uint64_t NowMs() {
    struct timespec ts {};
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return static_cast<uint64_t>(ts.tv_sec) * 1000ull +
           static_cast<uint64_t>(ts.tv_nsec / 1000000);
}

/*
 * Render a count that may legitimately be unset. "0" in the load line would read as a setting
 * someone deliberately chose, which is the one thing this value never is (see kColsUnset).
 */
std::string DescribeCount(uint32_t count) {
    if (count == kColsUnset) return std::string("未设置");
    return std::to_string(count);
}

bool IsLauncherProcess() {
    int fd = open("/proc/self/cmdline", O_RDONLY | O_CLOEXEC);
    if (fd < 0) return false;
    char buf[256];
    const ssize_t n = read(fd, buf, sizeof(buf) - 1);
    close(fd);
    if (n <= 0) return false;
    buf[n] = '\0';
    return strcmp(buf, "com.miui.home") == 0;
}

// Permission changes are journaled separately from whether the instruction landed.
// Fresh /proc metadata is a bounded identity check, not a lock against concurrent remapping.
struct CodeWordOps {
    const Image& image;
    long PageSize() { return sysconf(_SC_PAGESIZE); }
    bool Page(uintptr_t address, size_t size, nhk::CodePage& page) {
        int fd = open("/proc/self/maps", O_RDONLY | O_CLOEXEC);
        if (fd < 0) return false;
        try {
            std::string text; std::array<char,4096> chunk{}; bool good = true;
            unsigned interruptions = 0;
            for (;;) {
                const ssize_t n = read(fd,chunk.data(),chunk.size());
                if (n < 0 && errno == EINTR && ++interruptions <= 32) continue;
                if (n < 0) { good = false; break; }
                if (n == 0) break;
                if (size_t(n) > chunk.size() || size_t(n) > 2*1024*1024-text.size()) {
                    good = false; break;
                }
                text.append(chunk.data(),size_t(n));
            }
            const int closed = close(fd); fd = -1;
            if (closed != 0) good = false; // close(EINTR) must not be retried
            return good && nhk::ParseCodePage(text,address,size,page);
        } catch (...) {
            if (fd >= 0) (void)close(fd);
            return false; // Allocation/parsing faults must retain the caller journal, not escape.
        }
}
    bool Accept(const nhk::CodePage& page) {
        return page.inode && page.inode == image.sourceInode
            && uint64_t(makedev(page.major,page.minor)) == image.sourceDevice;
    }
    bool Read(uintptr_t address, uint32_t& out) {
        uint32_t value = 0;
        if (!nhk::safe_read(address,std::as_writable_bytes(std::span(&value,1)))) return false;
        out = value; return true;
    }
    bool Protect(uintptr_t page, size_t size, int protection) {
        return mprotect(reinterpret_cast<void*>(page),size,protection) == 0;
    }
    long Write(uintptr_t address, uint32_t word, uint32_t expected) {
        // Preserve one aligned atomic instruction change. Kernel byte-copy APIs do not
        // promise instruction-word atomicity. An unmapping race still requires a lease.
        return __atomic_compare_exchange_n(reinterpret_cast<uint32_t*>(address),&expected,
            word,false,__ATOMIC_RELEASE,__ATOMIC_RELAXED) ? 4 : 0;
    }
    void Flush(uintptr_t address, size_t size) {
        __builtin___clear_cache(reinterpret_cast<char*>(address),reinterpret_cast<char*>(address+size));
    }
};

nhk::WordWriteResult WriteWord(State* state, uintptr_t address, uint32_t expected,
    uint32_t word, uint32_t restore) {
    if (!state) return {};
    CodeWordOps ops{state->image};
    return nhk::WriteCodeWord(ops,address,expected,word,restore);
}

bool RecoverWrite(State* state, nhk::WordWriteJournal& recovery) {
    if (!state) return false;
    CodeWordOps ops{state->image};
    return nhk::RecoverCodeWord(ops,recovery);
}

bool ReadWord(const State* state, uintptr_t address, uint32_t* out) {
    if (!state || !out || address < state->image.base
        || address - state->image.base > UINT32_MAX) return false;
    // CodeView publishes a scalar only after a complete, fault-reporting read.
    return CodeView(state->image).Word(uint32_t(address - state->image.base), out);
}

struct FeatureSlot {
    uint32_t num;
    uint32_t bit;
    const char* name;
};

constexpr FeatureSlot kFeatureSlots[] = {
        {kFeatureNoClear, kWantNoClear, "去掉最近任务清理键"},
        {kFeatureFolderCols, kWantFolderCols, "文件夹每行应用数"},
        {kFeatureHideClear, kWantHideClear, "仅隐藏最近任务清理键"},
        {kFeaturePadGrid, kWantPadGrid, "平板桌面网格"},
        {kFeaturePhoneGrid, kWantPhoneGrid, "手机桌面行列"},
        {kFeatureFoldGrid, kWantFoldGrid, "折叠屏桌面行列"},
        {kFeatureIconSize, kWantIconSize, "三端桌面图标大小"},
};
constexpr size_t kFeatureSlotCount = sizeof(kFeatureSlots) / sizeof(kFeatureSlots[0]);

uint32_t MaskOf(const LocatedSites& sites) {
    uint32_t mask = 0;
    for (const FeatureSlot& f : kFeatureSlots) {
        if (FeatureOk(sites, f.num)) mask |= f.bit;
    }
    return mask;
}

uint32_t AdoptInto(LocatedSites* dst, const LocatedSites& src) {
    uint32_t adopted = 0;
    for (const FeatureSlot& f : kFeatureSlots) {
        if (!FeatureOk(src, f.num)) continue;
        CopyFeature(dst, src, f.num);
        adopted |= f.bit;
    }
    return adopted;
}

std::string DescribeMask(uint32_t mask) {
    std::string s;
    for (const FeatureSlot& f : kFeatureSlots) {
        if (!s.empty()) s += " ";
        s += "功能";
        s += std::to_string(f.num);
        s += (mask & f.bit) ? "=就绪" : "=缺";
    }
    return s;
}

void AppendSource(State* state, const char* tag) {
    const size_t len = strlen(state->siteSource);
    if (len == 0) {
        snprintf(state->siteSource, sizeof(state->siteSource), "%s", tag);
        return;
    }
    if (len + 1 >= sizeof(state->siteSource)) return;
    snprintf(state->siteSource + len, sizeof(state->siteSource) - len, "+%s", tag);
}

using MadviseFn = int (*)(void*, size_t, int);

std::mutex g_protectMutex;
std::vector<std::pair<uintptr_t, uintptr_t>> g_protectedRanges;
MadviseFn g_madviseOriginal = nullptr;

void RefreshProtectedRanges(const std::vector<AppliedPatch>& applied) {
    const long pageSize = sysconf(_SC_PAGESIZE);
    std::lock_guard<std::mutex> lock(g_protectMutex);
    g_protectedRanges.clear();
    if (pageSize < 4 || (uint64_t(pageSize) & (uint64_t(pageSize) - 1))) return;
    for (const AppliedPatch& p : applied) {
        if (!p.address || (p.address & 3u) || p.address > UINTPTR_MAX - uintptr_t(pageSize)) continue;
        const uintptr_t begin = p.address & ~(static_cast<uintptr_t>(pageSize) - 1);
        const uintptr_t end = begin + static_cast<uintptr_t>(pageSize);
        g_protectedRanges.emplace_back(begin, end);
    }
}

bool IntersectsProtected(uintptr_t begin, uintptr_t end) {
    std::lock_guard<std::mutex> lock(g_protectMutex);
    for (const auto& range : g_protectedRanges) {
        if (begin < range.second && range.first < end) return true;
    }
    return false;
}

int HookMadvise(void* address, size_t length, int advice) {
    if (advice == MADV_DONTNEED && address != nullptr && length > 0) {
        const uintptr_t begin = reinterpret_cast<uintptr_t>(address);
        const uintptr_t end = begin + length;
        if (end > begin && IntersectsProtected(begin, end)) {
            return 0;
        }
    }
    if (g_madviseOriginal != nullptr) return g_madviseOriginal(address, length, advice);
    return -1;
}

void InstallMadviseGuard() {
    if (g_entries == nullptr || g_entries->hookFunc == nullptr) return;
    void* target = dlsym(RTLD_DEFAULT, "madvise");
    if (target == nullptr) {
        LOGW("找不到 madvise 符号，改为只靠周期巡检保护补丁");
        return;
    }
    void* backup = nullptr;
    const int rc = g_entries->hookFunc(target, reinterpret_cast<void*>(&HookMadvise), &backup);
    if (rc == 0 && backup != nullptr) {
        g_madviseOriginal = reinterpret_cast<MadviseFn>(backup);
        LOGI("已挂 madvise 页保护钩子（原函数 %p）", backup);
    } else {
        LOGW("madvise 钩子安装失败 rc=%d，改为只靠周期巡检保护补丁", rc);
    }
}

bool RevertApplied(State* state) {
    if (!state) return false;
    std::vector<AppliedPatch> pending;
    pending.reserve(state->applied.size());
    for (auto it = state->applied.rbegin(); it != state->applied.rend(); ++it) {
        AppliedPatch patch = *it;
        patch.reverting = true;
        if (!RecoverWrite(state,patch.recovery)) { pending.push_back(patch); continue; }
        uint32_t current = 0;
        if (!ReadWord(state, patch.address, &current)) {
            pending.push_back(patch);
            continue;
        }
        if (current == patch.oldWord) continue;
        if (current != patch.newWord) { pending.push_back(patch); continue; }
        const auto result = WriteWord(state,patch.address,patch.newWord,patch.oldWord,patch.oldWord);
        patch.recovery = result.recovery;
        if (!result.complete || !ReadWord(state, patch.address, &current) || current != patch.oldWord) {
            // Retain both read faults and write/readback failures. A foreign value
            // is never overwritten, and no newer plan may mix with this old one.
            pending.push_back(patch);
        }
    }
    std::reverse(pending.begin(), pending.end());
    state->applied = std::move(pending);
    RefreshProtectedRanges(state->applied);
    return state->applied.empty();
}

/*
 * Decode the immediate a movz carries, halved because a Dart Smi stores its value scaled by two.
 * The plan works in instruction words, so this is what turns a patch line into something a reader
 * can check against the settings page.
 */
long DecodeMovzSmi(uint32_t word) {
    // movz w?, #imm (0x52800000) and movz x?, #imm (0xD2800000) differ only in the size bit.
    if ((word & 0x7F800000u) != 0x52800000u && (word & 0x7F800000u) != 0x52800000u + 0x02000000u) {
        return -1;
    }
    const uint32_t imm16 = (word >> 5) & 0xFFFFu;
    const uint32_t shift = ((word >> 21) & 3u) * 16u;
    return static_cast<long>((imm16 << shift) >> 1);
}

PatchStats ApplyPatches(State* state, const std::vector<PlanPatch>& patches) {
    PatchStats stats;
    stats.total = patches.size();
    for (const auto& p : state->applied) {
        if (p.reverting) { stats.failed = patches.size(); return stats; }
    }
    /*
     * Record an applied slot once. A retry pass re-examines addresses that are already in the
     * list - the "already in place" branch below is the common case on the second attempt - and
     * appending unconditionally made `applied` grow without bound, which in turn made every
     * later RevertApplied/Watchdog pass walk an ever longer list.
     */
    auto record = [state](uintptr_t address, uint32_t oldWord, uint32_t newWord) {
        for (auto& existing : state->applied) {
            if (existing.address == address) {
                existing.oldWord = oldWord;
                existing.newWord = newWord;
                return;
            }
        }
        state->applied.push_back({address, oldWord, newWord});
    };
    size_t& written = stats.written;
    size_t& already = stats.already;
    size_t& missing = stats.failed;
    size_t& mismatch = stats.mismatch;
    size_t& outOfRange = stats.outOfRange;
    for (const PlanPatch& p : patches) {
        if (uintptr_t(p.va) > UINTPTR_MAX - state->image.base) { ++outOfRange; continue; }
        const uintptr_t address = state->image.base + static_cast<uintptr_t>(p.va);
        if (!CodeView(state->image).InText(p.va)) {
            ++outOfRange;
            LOGW("跳过越界补丁 va=%#x（不在 libapp.so 的映射段内）", p.va);
            continue;
        }
        uint32_t current = 0;
        if (!ReadWord(state, address, &current)) { ++missing; continue; }
        if (current == p.patch) {
            ++already;
            LOGI("补丁已就位 %s：va=%#x，无需改动", p.what != nullptr ? p.what : "?", p.va);
            record(address, p.expect, p.patch);
            continue;
        }
        if (current != p.expect) {
            ++mismatch;
            LOGW("跳过补丁 va=%#x：内存里是 %08x，期望原值 %08x（已被改动？）",
                 p.va, current, p.expect);
            continue;
        }
        const auto result = WriteWord(state,address,p.expect,p.patch,p.expect);
        if (result.complete) {
            ++written;
            const long before = DecodeMovzSmi(p.expect);
            const long after = DecodeMovzSmi(p.patch);
            if (before >= 0 && after >= 0) {
                LOGI("补丁 %s：va=%#x %ld → %ld", p.what != nullptr ? p.what : "?", p.va, before,
                     after);
            } else {
                LOGI("补丁 %s：va=%#x %08x → %08x", p.what != nullptr ? p.what : "?", p.va,
                     p.expect, p.patch);
            }
            record(address, p.expect, p.patch);
        } else {
            ++missing;
            if (result.recovery.pending()) {
                // A pending recovery is journal state, not a known-good slot: record it once and
                // keep the journal that the restore pass needs.
                bool existing = false;
                for (auto& slot : state->applied) {
                    if (slot.address == address) {
                        slot = {address, p.expect, p.patch, true, result.recovery};
                        existing = true;
                        break;
                    }
                }
                if (!existing) {
                    state->applied.push_back({address,p.expect,p.patch,true,result.recovery});
                }
                state->restoreBlocked = true;
            }
        }
    }
    LOGI("补丁应用完成：新写入 %zu，已就位 %zu，越界跳过 %zu，原值不符跳过 %zu，写入失败 %zu",
         written, already, outOfRange, mismatch, missing);
    RefreshProtectedRanges(state->applied);
    return stats;
}

void Watchdog(State* state) {
    size_t repaired = 0;
    for (AppliedPatch& p : state->applied) {
        // A pending restore belongs to the retiring plan, never to repair work.
        if (p.reverting) continue;
        uint32_t current = 0;
        if (!ReadWord(state, p.address, &current)) continue;
        if (current == p.newWord) continue;
        if (current == p.oldWord) {
            const auto result = WriteWord(state,p.address,p.oldWord,p.newWord,p.oldWord);
            if (result.complete) ++repaired;
            else {
                state->writeRetryPending = state->repatchNeeded = true;
                if (result.recovery.pending()) {
                    p.recovery = result.recovery; p.reverting = true;
                    state->restoreBlocked = true;
                }
            }
        } else {
            LOGW("补丁页被改写 地址=%#lx 现在是 %08x（期望 %08x）",
                 static_cast<unsigned long>(p.address), current, p.newWord);
        }
    }
    if (repaired > 0) {
        LOGI("巡检：发现 %zu 处补丁被系统回收，已重新写入", repaired);
    }
}

/*
 * Whether the legacy on-disk configuration channel may be read (see RefreshConfigLocked).
 *
 * Cached in a function-local static on purpose: the answer is a pure function of a system property,
 * so unlike the state HomeTweaksPrepareForLauncherChild resets, inheriting it across the fork is
 * correct rather than poison - and the question is asked on every monitor pass.
 */
bool config_blob_channel_enabled() {
    static const bool enabled = [] { return DebugPropertyEnabled(kPropConfigBlob); }();
    return enabled;
}

bool RefreshConfigLocked(State* state, bool allowBakedFallback, bool fast) {
    Config config;
    std::vector<uint8_t> bytes;
    char path[512]{};
    uint64_t mtime = 0;

    /*
     * The on-disk configuration channel is opt-in, and off by default.
     *
     * It reads eight candidate paths - two of them world-writable, plus /data/local/tmp and /data/adb,
     * the two places a "push this file and restart" guide would tell someone to use - and the values
     * it finds win over the settings page on every pass, because the push path (PushTweaksConfig) sets
     * configLoaded without setting configBytes, so the read below always looks like a change. A file
     * left behind by an older build therefore kept configuring the desktop of a user who had set
     * nothing: that is exactly how a "why is my layout five columns after I open a folder" report is
     * produced. The settings page pushed over the module's own channel is the single source of truth;
     * this channel stays for calibration runs and is enabled with the property below.
     */
    if (!config_blob_channel_enabled()) return state->configLoaded;

    /*
     * Early exit on an unchanged candidate set.
     *
     * This pass re-arms itself unconditionally (every 500 ms while still starting, once per loop
     * period afterwards), and LoadConfigBlob opens, reads and parses every candidate path each
     * time - up to eight paths, most of them on another filesystem. In the steady state none of
     * them has moved, so all of that was pure waste: the mtime was already being read and recorded
     * and was never used to skip anything. One stat() per candidate is orders of magnitude cheaper
     * than reading them, so the read now only happens when the newest candidate actually moved.
     *
     * The fingerprint is re-stamped on every path out of the read below, including the failing one,
     * so a candidate that exists but cannot be parsed does not turn into a read on every pass.
     */
    char probePath[512]{};
    uint64_t probeMtime = 0;
    const size_t probeCount = fast ? kFastConfigPaths : kAllConfigPaths;
    const bool probed = NewestConfigCandidate(probePath, sizeof(probePath), &probeMtime, probeCount);
    if (probed && state->configLoaded && state->configProbeValid
        && state->configProbeMtime == probeMtime
        && strcmp(state->configProbePath, probePath) == 0) {
        return true;
    }

    const bool read = fast
            ? LoadConfigBlobFast(&config, &bytes, path, sizeof(path), &mtime)
            : LoadConfigBlob(&config, &bytes, path, sizeof(path), &mtime);
    if (probed) {
        memcpy(state->configProbePath, probePath, sizeof(state->configProbePath));
        state->configProbeMtime = probeMtime;
        state->configProbeValid = true;
    } else {
        state->configProbeValid = false;
    }
    if (read) {
        const bool bytesChanged = !state->configLoaded || bytes != state->configBytes;
        const bool pathChanged = strcmp(path, state->configPath) != 0;
        if (bytesChanged || pathChanged) {
            state->config = config;
            state->configBytes = bytes;
            memcpy(state->configPath, path, sizeof(state->configPath));
            state->configMtime = mtime;
            state->configLoaded = true;
            state->configFromBaked = false;
        }
        if (bytesChanged) {
            state->repatchNeeded = true;
            LOGI("已加载配置 %s（总开关=%s，功能 %zu 项，文件夹每行 %s 个）[文件通道]",
                 path, config.masterEnabled() ? "开" : "关", config.enabled.size(),
                 DescribeCount(config.folderCols).c_str());

            if (strcmp(path, kLocalConfigPath) != 0) {
                if (MirrorConfigToPath(kLocalConfigPath, bytes.data(), bytes.size(), mtime)) {
                    LOGI("已把配置镜像到 %s", kLocalConfigPath);
                } else {
                    LOGW("配置镜像到 %s 失败，本次仍按已读到的配置生效", kLocalConfigPath);
                }
            }
        }
        return true;
    }

    return state->configLoaded;
}

void AcquireSitesBySymbols(State* state, const CodeView& code, uint32_t wanted) {
    SymbolIndex& index = SymbolIndex::Instance();
    if (!index.EnsureLoaded(state->image)) {
        LOGW("符号表不可用（%s），只能回退到构建期指纹", index.status());
        return;
    }

    state->squareTried = true;
    const uint64_t t0 = NowMs();
    LocatedSites found = state->sites;
    LocateSites(code, state->config, &found);
    state->sites = found;

    const uint32_t adopted = MaskOf(found);
    const uint32_t before = state->attemptedMask;
    state->attemptedMask |= adopted;
    if ((adopted & ~before) != 0) AppendSource(state, "符号表");

    LOGI("符号定位完成：用时 %llu ms（%s）→ %s", static_cast<unsigned long long>(NowMs() - t0),
         index.status(), DescribeMask(adopted).c_str());

    if (adopted != 0 && (adopted & ~before) != 0) {
        SaveSitesCache(state->imageId, found);
    }
    (void) wanted;
}

bool NeedAcquire(const State* state, uint32_t wanted) {
    if ((MaskOf(state->sites) & wanted) != wanted) return true;
    if ((wanted & kWantFolderCols) != 0 && !state->sites.ok16sq && !state->squareTried) {
        return true;
    }
    return false;
}

void AcquireSites(State* state, bool allowScan) {    CodeView code(state->image);
    const uint32_t wanted = WantedFeatureMask(state->config);
    if (wanted == 0) return;
    // A failed identity is a cache miss, not an invitation to hash the whole file
    // on every settings refresh. Child State reset permits the next image attempt.
    if (!state->identityTried) {
        state->identityTried = true;
        state->imageId = ImageIdentity(state->image);
    }

    if (!state->packTried && NeedAcquire(state, wanted)) {
        state->packTried = true;
        const char* packPaths[8];
        const size_t packCount = SitePackPaths(packPaths, 8);
        for (size_t pi = 0; pi < packCount; ++pi) {
            LocatedSites pack;
            if (!LoadSitePack(packPaths[pi], &pack)) continue;
            if (!pack.AnyOk()) continue;
            LocatedSites adoptedPack;
            for (const FeatureSlot& f : kFeatureSlots) {
                LocatedSites probe = pack;
                KeepOnlyFeature(&probe, f.num);
                if (ValidateSitesShape(code, probe)) {
                    CopyFeature(&adoptedPack, probe, f.num);
                }
            }
            const uint32_t adopted = AdoptInto(&state->sites, adoptedPack);
            state->attemptedMask |= adopted;
            if (adopted != 0) {
                AppendSource(state, "站点包");
                LOGI("站点包命中 %s：%s", packPaths[pi],
                     DescribeMask(adopted).c_str());
            }
            break;
        }
    }

    if (!state->cacheTried && NeedAcquire(state, wanted)) {
        state->cacheTried = true;
        LocatedSites cached;
        if (LoadSitesCache(state->imageId, &cached)) {
            if (cached.AnyOk() &&
                (ValidateSites(code, cached) || ValidateSitesShape(code, cached))) {
                const uint32_t taken = AdoptInto(&state->sites, cached);
                if (taken != 0) AppendSource(state, "缓存");
                LOGI("站点缓存命中（镜像指纹 %016llx）：%s",
                     static_cast<unsigned long long>(state->imageId),
                     DescribeMask(taken).c_str());
            } else {
                LOGW("站点缓存校验失败（桌面代码与缓存不符），丢弃并改走符号定位");
                DropSitesCache();
            }
        }
    }

    if (NeedAcquire(state, wanted)) {
        AcquireSitesBySymbols(state, code, wanted);
    }

    if (!allowScan) return;
    const uint32_t missing = wanted & ~state->attemptedMask & ~MaskOf(state->sites);
    if (missing == 0 && !NeedAcquire(state, wanted)) return;
    state->attemptedMask |= missing;

    const uint64_t t0 = NowMs();
    LocatedSites found = state->sites;
    const uint32_t beforeScan = MaskOf(found);
    LocateSites(code, state->config, &found);
    const uint64_t cost = NowMs() - t0;
    state->sites = found;
    state->attemptedMask |= MaskOf(found);
    if ((MaskOf(found) & ~beforeScan) != 0) AppendSource(state, "全文扫描");

    LOGI("全文扫描完成：用时 %llu ms（本次诉求位=%u，结果 %s）",
         static_cast<unsigned long long>(cost), missing, DescribeMask(MaskOf(found)).c_str());
    if (found.AnyOk()) {
        SaveSitesCache(state->imageId, found);
    }
}

void LogPlanReasons(const PlanResult& plan, const Config& cfg) {
    if (!cfg.masterEnabled()) {
        LOGI("总开关关闭，已还原全部补丁");
        return;
    }
    const uint32_t wanted = WantedFeatureMask(cfg);
    if ((wanted & kWantNoClear) && !plan.ok9) {
        LOGW("功能 9（去掉清理按钮）定位失败：%s", plan.why9);
    }
    if ((wanted & kWantFolderCols) && !plan.ok16) {
        LOGW("功能 16（文件夹每行应用数）定位失败：%s", plan.why16);
    }
    if ((wanted & kWantHideClear) && !plan.ok18) {
        LOGW("功能 18（隐藏清理按钮）定位失败：%s", plan.why18);
    }
    if ((wanted & kWantPadGrid) && !plan.ok4) {
        LOGW("功能 4（平板桌面网格）定位失败：%s", plan.why4);
    }
    if ((wanted & kWantPhoneGrid) && !plan.ok19) {
        LOGW("功能 19（手机桌面行列）定位失败：%s%s", plan.why19,
                 plan.ok19r ? "" : " [行数未就绪]");
    }
    if ((wanted & kWantFoldGrid) && !plan.ok20) {
        LOGW("功能 20（折叠屏桌面行列）定位失败：%s", plan.why20);
    }
    if ((wanted & kWantIconSize) && !plan.ok21) {
        LOGW("功能 21（三端桌面图标大小）定位失败：%s", plan.why21);
    }
}

std::string DescribePlan(const PlanResult& plan) {
    std::string s;
    const bool ok[7] = {plan.ok9,  plan.ok16, plan.ok18, plan.ok4, plan.ok19,
                        plan.ok20, plan.ok21};
    for (size_t i = 0; i < kFeatureSlotCount; ++i) {
        if (i != 0) s += " ";
        s += "功能";
        s += std::to_string(kFeatureSlots[i].num);
        s += ok[i] ? "=计划可应用" : "=计划不可应用";
    }
    return s;
}

/*
 * The status report used to live here: BuildStatusText() rendered a multi-line plain-text summary of
 * the located sites, the applied patches and the failures, and WriteStatusFile() wrote it to a file
 * in the launcher's data dir plus a second copy in Downloads.
 *
 * It is gone on purpose, along with WhyOf() and JoinEnabled() which existed only to feed it. It was
 * a diagnostics feature that ran unconditionally on every settle pass, and the Downloads copy alone
 * was reason enough to remove it:
 *
 *   - MediaProvider indexes everything under /sdcard. Every write landed as `hometweaks.status.tmp`
 *     and was then renamed, so the indexer raced the rename and logged "Database update failed while
 *     renaming ... .tmp" every single time.
 *   - HyperOS's gallery cached the path and re-queried it, logging a miss on each pass.
 *   - It showed up in the user's Downloads, which is not where a module's private bookkeeping
 *     belongs.
 *
 * Nothing ever read either copy back - it was write-only - so the entire feature is gone rather
 * than gated behind a property. Everything it used to say is already in logcat, which is where a
 * device that misbehaves is actually diagnosed: LogPlanReasons() below names each failed feature and
 * its reason, and ApplyPatches() logs every skip and write failure. `logcat -s HomeTweaks` is the
 * replacement for `cat hometweaks.status`, and it costs no file in any user-visible directory.
 */

void SettleLocked(State* state, bool allowScan) {
    if (state == nullptr || !state->imageFound || !state->configLoaded) return;
    if (state->restoreBlocked) state->repatchNeeded = true;
    if (!state->repatchNeeded) {
        /*
         * The plan is complete and the sites are known, but a previous write failed. Retry the
         * writes only: no revert, no re-scan. Everything the pass needs is already in state, so
         * this is a few dozen atomic stores instead of a full image sweep.
         */
        if (state->writeRetryPending) {
            // A write is only retried once no slot is mid-restore: mixing a retry with a
            // half-finished revert is what the ApplyPatches guard refuses.
            bool reverting = false;
            for (const auto& p : state->applied) if (p.reverting) { reverting = true; break; }
            if (reverting) {
                state->restoreBlocked = true;
                state->repatchNeeded = true;
                return;
            }
            const PatchStats retry = ApplyPatches(state, state->lastPlan.patches);
            state->patchStats = retry;
            state->writeRetryPending = retry.failed != 0;
            return;
        }
        state->repatchNeeded = false;
        return;
    }

    if (!RevertApplied(state)) {
        state->restoreBlocked = true;
        state->patchStats.failed = state->applied.size();
        return;
    }
    state->restoreBlocked = false;
    AcquireSites(state, allowScan);

    const uint64_t t0 = NowMs();
    PlanResult plan;
    DerivePatches(CodeView(state->image), state->config, state->sites, &plan);
    const PatchStats stats = ApplyPatches(state, plan.patches);
    state->lastPlan = plan;
    state->patchStats = stats;

    const uint32_t wanted = WantedFeatureMask(state->config);
    const uint32_t done = (MaskOf(state->sites) | state->attemptedMask) & wanted;
    /*
     * A failed write is not the same thing as an unfinished site search, and the two must drive
     * different work. `done != wanted` means symbols are still missing, so a full re-scan is the
     * only way forward. `stats.failed != 0` means the addresses are known and the *write* failed
     * - typically because the target page was still being relocated during boot. Re-scanning the
     * whole image every pass does not make that write succeed, it only burns the launcher's CPU.
     * Keep the retry, drop the rescan: `writeRetryPending` schedules another pass but no longer
     * sets `repatchNeeded` on its own.
     */
    state->writeRetryPending = stats.failed != 0;
    state->repatchNeeded = state->restoreBlocked || (done != wanted);

    LOGI("配置落地：共 %zu 条补丁（%s），用时 %llu ms%s",
         plan.patches.size(), DescribePlan(plan).c_str(),
         static_cast<unsigned long long>(NowMs() - t0),
         state->repatchNeeded ? "（尚未全部定位，后台继续）" : "");
    LogPlanReasons(plan, state->config);
}

bool TryDiscoverImage() {
    if (g_imageKnown.load(std::memory_order_acquire)) return true;

    Image image{};
    const char* how = nullptr;

    if (FindImageByName(kTargetLibName, &image)) {
        how = "dl_iterate_phdr";
    } else if (FindImageFromApkMaps(kDesktopApkMarker, kTargetLibName, &image)) {
        how = "maps+apk";
    } else {
        return false;
    }

    std::lock_guard<std::mutex> work(g_workMutex);
    if (!g_state.imageFound) {
        g_state.image = image;
        g_state.imageFound = true;
        g_imageKnown.store(true, std::memory_order_release);
        snprintf(g_state.imageHow, sizeof(g_state.imageHow), "%s",
                 how != nullptr ? how : "未知");
        LOGI("已找到 %s（%s）：base=%#lx 段数=%zu 路径=%s", kTargetLibName, how,
             static_cast<unsigned long>(image.base), image.segmentCount, image.path);
        DumpImage(image);
    }
    return true;
}

uint64_t MonitorPeriodMs(const State& state) {
    /*
     * The period is a *policy*, and it has exactly two values.
     *
     * 1500 ms - the steady cadence. Taken when there is nothing left to do, and equally when
     *           the runtime is knowingly stuck: a restore that could not complete, or a write
     *           that keeps failing. Those states are unfinished, but re-attempting them at the
     *           start-up rate changes nothing. During boot the target image is still being
     *           relocated, so an unfinished pass cannot converge no matter how often it runs -
     *           and running it twenty times a second is what starves the launcher main thread of
     *           g_workMutex and keeps the desktop off the screen. A stuck state is therefore
     *           retried on the *slow* cadence, not the fast one.
     * 50 ms   - the start-up / applying cadence. Taken while the image or the configuration is
     *           still being found, and while a plan is actively being applied. A genuinely
     *           progressed plan settles in a pass or two, so this stays bounded.
     */
    if (state.imageFound && state.configLoaded && !state.repatchNeeded) return 1500;
    if (state.restoreBlocked || state.writeRetryPending) return 1500;
    return 50;
}

void MonitorLoop() {
    uint64_t nextConfigCheck = 0;

    for (;;) {
        if (!TryDiscoverImage()) {
            static uint64_t firstFailAt = 0;
            static bool dumped = false;
            const uint64_t now = NowMs();
            if (firstFailAt == 0) firstFailAt = now;
            if (!dumped && now - firstFailAt > 3000) {
                dumped = true;
                LOGW("一直找不到 %s（已等 %llu ms），记一次现场供排查", kTargetLibName,
                     static_cast<unsigned long long>(now - firstFailAt));
                DumpLoadedLibraries(50);
                DumpApkMaps(30);
            }
        }

        uint64_t periodMs = 50;
        {
            std::lock_guard<std::mutex> work(g_workMutex);
            State* state = &g_state;

            const uint64_t now = NowMs();
            /*
             * The deadline only binds while the loop is still fast - the 50 ms period below, which
             * is what start-up and a repatch use. In the steady state the loop period itself is the
             * real cadence, so this reads as "check on every pass, but not more than twice a second".
             * The check is affordable at that rate because RefreshConfigLocked is now a stat() of
             * the candidates unless one of them actually changed.
             */
            if (!state->configLoaded || now >= nextConfigCheck) {
                nextConfigCheck = now + (state->configLoaded ? 500 : 100);
                RefreshConfigLocked(state, true, false);
            }

            SettleLocked(state, true);

            if (!state->applied.empty()) Watchdog(state);

            periodMs = MonitorPeriodMs(*state);
        }

        {
            std::unique_lock<std::mutex> lock(g_wakeMutex);
            g_wakeCv.wait_for(lock, std::chrono::milliseconds(periodMs),
                              [] { return g_wakeFlag; });
            g_wakeFlag = false;
        }
    }
}

void FastPatchOnLibraryLoad() {
    TryDiscoverImage();

    std::lock_guard<std::mutex> work(g_workMutex);
    State* state = &g_state;
    if (!state->imageFound) return;
    if (!state->applied.empty()) return;
    if (!RefreshConfigLocked(state, true, true)) return;
    SettleLocked(state, false);
}

void OnNativeLibraryLoaded(const char* name, void* ) {
    g_loadedCount.fetch_add(1);
    if (name == nullptr) return;
    const size_t len = strlen(name);
    const size_t want = strlen(kTargetLibName);
    if (len < want || strcmp(name + (len - want), kTargetLibName) != 0) return;
    LOGI("检测到 %s 加载，立即落补丁", name);

    FastPatchOnLibraryLoad();

    {
        std::lock_guard<std::mutex> lock(g_wakeMutex);
        g_wakeFlag = true;
    }
    g_wakeCv.notify_all();
}

}
}

/*
 * The file's implementation lives in an anonymous namespace; the entry points below are the module's
 * own, so they are reopened in hometweaks proper. The anonymous members stay reachable from here
 * because an unnamed namespace is visible throughout the translation unit that contains it.
 */
namespace hometweaks {
namespace {

void WakeMonitor() {
    {
        std::lock_guard<std::mutex> lock(g_wakeMutex);
        g_wakeFlag = true;
    }
    g_wakeCv.notify_all();
}

} // namespace

/**
 * Start the tweaks runtime for this process.
 *
 * Called by the module's own native entry point when it is running inside the desktop, which is the
 * only process where the target image exists. The configuration is not read from the desktop's data
 * directory here: that file belongs to the desktop's uid and is written by a different process in
 * the upstream design, while this module delivers its values over its own channel - see
 * PushTweaksConfig.
 */
void StartHomeTweaks() {
    if (!IsLauncherProcess()) return;
    if (g_tweaksStarted.exchange(true, std::memory_order_acq_rel)) return;
    pthread_t thread;
    if (pthread_create(&thread, nullptr,
            [](void*) -> void* {
                // Named for field triage: an unnamed thread in the launcher cannot be attributed
                // to a component, and this one is a permanent loop.
                (void)pthread_setname_np(pthread_self(), "hc-home-tweaks");
                MonitorLoop();
                return nullptr;
            },
            nullptr) == 0) {
        pthread_detach(thread);
    } else {
        LOGE("HomeTweaks：监视线程创建失败");
        g_tweaksStarted.store(false, std::memory_order_release);
        return;
    }
    LOGI("HomeTweaks 已启动（目标 %s）", kTargetLibName);
    InstallMadviseGuard();
    WakeMonitor();
}

/**
 * Hand the runtime the values the settings page chose.
 *
 * Every value is taken from the module's configuration channel rather than from a blob on disk, so
 * the plan is rebuilt whenever the page changes something and there is no file to keep in sync.
 */
void PushTweaksConfig(const Config& config);

/**
 * The narrow form used by the module's existing channel: the desktop's own grid selection drives
 * feature 19 (the phone grid's columns and rows). Kept separate from PushTweaksConfig so the caller
 * does not need the blob types, and so a feature can be wired in one value at a time.
 */
/**
 * Called as soon as the target image is loaded, which is earlier than the desktop's first layout
 * calculation: a patch applied after that point changes the constant but is simply not read again,
 * which is why a plan can report "applied" while the desktop looks untouched.
 */
void HomeTweaksOnLibraryLoaded(const char* name) {
    OnNativeLibraryLoaded(name, nullptr);
}

/**
 * Drop every piece of state this process could only have inherited from its parent.
 *
 * The desktop is forked from a spawner that shares this module and, after its own setprogname,
 * looks exactly like the launcher. If the monitor was started there, the child inherits the
 * "already started" flag while the thread itself does not survive the fork - the monitor is then
 * gone for good and every later start is a no-op. The image record and the symbol table are just as
 * poisoned: they describe the parent's mappings. Called from the child's first specialization
 * signal; the exchange makes repeated calls free.
 */
void HomeTweaksPrepareForLauncherChild() {
    // Deduplicated by pid: a fork changes the pid, so the first specialization signal in the child
    // does the reset exactly once, and every later call in the same process is a no-op instead of
    // starting yet another monitor thread.
    static pid_t prepared_pid = 0;
    const pid_t self = getpid();
    if (prepared_pid == self) return;
    prepared_pid = self;
    if (!g_tweaksStarted.exchange(false, std::memory_order_acq_rel)) return;
    std::lock_guard<std::mutex> work(g_workMutex);
    g_state = State{};
    g_imageKnown.store(false, std::memory_order_release);
    SymbolIndex::Instance().ResetForTest();
}

/**
 * While true on the calling thread, symbol lookups will not start the (expensive) index build.
 *
 * Owned here rather than in the layout module because HomeTweaksFindSymbol is the single choke point
 * every symbol query goes through, so one flag covers bind_knobs, resolve_grid_fields and the probe.
 */
// Cache miss: small output chunks and an explicit deadline; timeout resets unpublished roots.
// Worker later persists a completed index, never fsync in this bounded loader attempt.
bool HomeTweaksPrimeCachedSymbols() {
    Image image{};
    if (!FindImageByName(kTargetLibName, &image)
        && !FindImageFromApkMaps(kDesktopApkMarker, kTargetLibName, &image)) return false;
    std::unique_lock<std::mutex> work(g_workMutex, std::try_to_lock);
    if (!work.owns_lock()) return false;
    if (!g_state.imageFound) {
        g_state.image = image; g_state.imageFound = true;
        g_imageKnown.store(true, std::memory_order_release);
    }
    if (!g_state.identityTried) {
        g_state.identityTried = true; g_state.imageId = ImageIdentity(g_state.image);
    }
    auto& index = SymbolIndex::Instance();
    if (index.TryLoadCached(g_state.image, g_state.imageId)) return true;
    const uint64_t start = NowMs();
    const bool ready = index.EnsureLoaded(g_state.image, 300);
    LOGI("首次无符号缓存预算解析：ready=%d elapsed=%llu ms（预算300ms）", ready ? 1 : 0,
        static_cast<unsigned long long>(NowMs() - start));
    return ready;
}

thread_local bool g_inLoaderCallback = false;

bool InLoaderCallback() { return g_inLoaderCallback; }

void SetInLoaderCallback(bool value) { g_inLoaderCallback = value; }

/**
 * Look a function up by the name the launcher's own symbol table gives it. Reading the address out
 * of the image rather than carrying it across an OTA is the whole point: the name is part of the
 * launcher's build, the address is not.
 */
bool HomeTweaksFindSymbol(const char *name, uint32_t *outVa, uint32_t *outSize) {
    if (name == nullptr || outVa == nullptr || outSize == nullptr) return false;
    SymbolIndex &index = SymbolIndex::Instance();
    /*
     * Inside the dlopen callback the symbol table may not be built yet, and building it here is what
     * froze the desktop: EnsureLoaded reads the whole .gnu_debugdata out of the 28 MB APK and runs an
     * xz pass over it, on the thread that holds the linker lock while the launcher is starting. On a
     * device under I/O pressure that exceeded the 5 s input-dispatch timeout, the launcher ANR'd, was
     * killed, forked, and did it again - which the user reported as "the layout changes do nothing".
     *
     * The loader thread therefore only ever *consumes* an index that is already built. If it is not,
     * this returns "not found" and the caller does the same thing it does for a genuinely missing
     * symbol: leaves the site alone. The maintenance worker builds the index on its own thread and
     * both re-run their binding passes immediately afterwards, so nothing is lost - only moved off
     * the critical path. EnsureLoaded is idempotent and single-shot (`attempted_`), so a worker that
     * got there first is not disturbed.
     */
    if (InLoaderCallback() && !index.attempted()) return false;
    TryDiscoverImage();
    std::lock_guard<std::mutex> work(g_workMutex);
    if (!g_state.imageFound) return false;
    if (!index.EnsureLoaded(g_state.image)) return false;
    return index.Find(name, outVa, outSize);
}

/**
 * Distance from `va` to the next symbol in the same table.
 *
 * Some Dart bodies are reported far shorter than the code that follows them -- on launcher 7722
 * `LauncherIndicatorState.build` claims 0x58 while its real extent is 0x2b4, which is larger than
 * the number of named functions in it. Anything that has to look at a whole body therefore has
 * to walk to where the next name begins, not to the size the table reports, or it stops half way
 * through the very instructions it is looking for. Returns false when `va` is the last symbol.
 */
bool HomeTweaksSymbolSpan(uint32_t va, uint32_t *outSpan) {
    if (va == 0 || outSpan == nullptr) return false;
    TryDiscoverImage();
    std::lock_guard<std::mutex> work(g_workMutex);
    if (!g_state.imageFound) return false;
    SymbolIndex &index = SymbolIndex::Instance();
    if (!index.EnsureLoaded(g_state.image)) return false;
    return index.SpanFrom(va, outSpan);
}

/**
 * Report the launcher image this module resolved.
 *
 * The layout module's own APK discovery matches a fixed /data directory pattern; the 7654 desktop
 * ships from /product/priv-app/MiuiHome, which that pattern can never match, so its geometry sites
 * and every hook knob lost their image anchor. This hands over the path dl_iterate_phdr actually
 * proved — the same image the symbol table above is read from — instead of a second guess.
 */
bool HomeTweaksTargetImage(char *path, size_t cap) {
    if (path == nullptr || cap == 0) return false;
    TryDiscoverImage();
    std::lock_guard<std::mutex> work(g_workMutex);
    if (!g_state.imageFound || g_state.image.path[0] == '\0') return false;
    snprintf(path, cap, "%s", g_state.image.path);
    return true;
}

void PushPhoneGrid(uint32_t cols, uint32_t rows) {
    Config config;
    config.flags = 1u; // master on
    config.enabled.push_back(kFeaturePhoneGrid);
    config.phoneCols = cols;
    config.phoneRows = rows;
    PushTweaksConfig(config);
}

void PushTweaksConfig(const Config& config) {
    {
        std::lock_guard<std::mutex> work(g_workMutex);
        g_state.config = config;
        g_state.configLoaded = true;
        g_state.configFromBaked = false;
        g_state.repatchNeeded = true;
        g_state.packTried = false;
        g_state.cacheTried = false;
    }
    WakeMonitor();
}

} // namespace hometweaks
