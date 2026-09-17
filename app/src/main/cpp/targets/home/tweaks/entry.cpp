// SPDX-License-Identifier: Apache-2.0
#include "blob.h"
#include "common.h"
#include "ht_plan.h"
#include "htcache.h"
#include "image.h"
#include "scanner.h"
#include "symtab.h"

#include <dlfcn.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
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

    char imageHow[32]{};
    char siteSource[96]{};
    PlanResult lastPlan;
    PatchStats patchStats;
    bool statusWritten = false;
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

int PageProtection(uintptr_t address) {
    FILE* maps = fopen("/proc/self/maps", "re");
    if (maps == nullptr) return -1;
    char line[512];
    int result = -1;
    while (fgets(line, sizeof(line), maps) != nullptr) {
        unsigned long long begin = 0, end = 0;
        char perms[5] = {0};
        if (sscanf(line, "%llx-%llx %4s", &begin, &end, perms) != 3) continue;
        if (address < begin || address >= end) continue;
        result = (perms[0] == 'r' ? PROT_READ : 0) | (perms[1] == 'w' ? PROT_WRITE : 0) |
                 (perms[2] == 'x' ? PROT_EXEC : 0);
        break;
    }
    fclose(maps);
    return result;
}

bool WriteWord(uintptr_t address, uint32_t word) {
    const long pageSize = sysconf(_SC_PAGESIZE);
    const uintptr_t pageStart = address & ~(static_cast<uintptr_t>(pageSize) - 1);
    const size_t span = static_cast<size_t>(address - pageStart + 4);
    int original = PageProtection(address);
    if (original < 0) original = PROT_READ | PROT_EXEC;
    if (mprotect(reinterpret_cast<void*>(pageStart), span,
                 original | PROT_WRITE | PROT_EXEC) != 0) {
        LOGE("mprotect 失败 地址=%#lx errno=%d", static_cast<unsigned long>(address), errno);
        return false;
    }
    *reinterpret_cast<volatile uint32_t*>(address) = word;
    __builtin___clear_cache(reinterpret_cast<char*>(address),
                            reinterpret_cast<char*>(address + 4));
    mprotect(reinterpret_cast<void*>(pageStart), span, original);
    return true;
}

uint32_t ReadWord(uintptr_t address) {
    return *reinterpret_cast<volatile uint32_t*>(address);
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
    for (const AppliedPatch& p : applied) {
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

void RevertApplied(State* state) {
    for (auto it = state->applied.rbegin(); it != state->applied.rend(); ++it) {
        const uint32_t current = ReadWord(it->address);
        if (current == it->newWord && current != it->oldWord) {
            if (!WriteWord(it->address, it->oldWord)) {
                LOGW("还原失败 地址=%#lx", static_cast<unsigned long>(it->address));
            }
        }
    }
    state->applied.clear();
    RefreshProtectedRanges(state->applied);
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
    size_t& written = stats.written;
    size_t& already = stats.already;
    size_t& missing = stats.failed;
    size_t& mismatch = stats.mismatch;
    size_t& outOfRange = stats.outOfRange;
    for (const PlanPatch& p : patches) {
        const uintptr_t address = state->image.base + static_cast<uintptr_t>(p.va);
        if (!RangeInImage(state->image, address, 4)) {
            ++outOfRange;
            LOGW("跳过越界补丁 va=%#x（不在 libapp.so 的映射段内）", p.va);
            continue;
        }
        const uint32_t current = ReadWord(address);
        if (current == p.patch) {
            ++already;
            LOGI("补丁已就位 %s：va=%#x，无需改动", p.what != nullptr ? p.what : "?", p.va);
            state->applied.push_back({address, p.expect, p.patch});
            continue;
        }
        if (current != p.expect) {
            ++mismatch;
            LOGW("跳过补丁 va=%#x：内存里是 %08x，期望原值 %08x（已被改动？）",
                 p.va, current, p.expect);
            continue;
        }
        if (WriteWord(address, p.patch)) {
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
            state->applied.push_back({address, p.expect, p.patch});
        } else {
            ++missing;
        }
    }
    LOGI("补丁应用完成：新写入 %zu，已就位 %zu，越界跳过 %zu，原值不符跳过 %zu，写入失败 %zu",
         written, already, outOfRange, mismatch, missing);
    RefreshProtectedRanges(state->applied);
    return stats;
}

void Watchdog(State* state) {
    size_t repaired = 0;
    for (const AppliedPatch& p : state->applied) {
        const uint32_t current = ReadWord(p.address);
        if (current == p.newWord) continue;
        if (current == p.oldWord) {
            if (WriteWord(p.address, p.newWord)) ++repaired;
        } else {
            LOGW("补丁页被改写 地址=%#lx 现在是 %08x（期望 %08x）",
                 static_cast<unsigned long>(p.address), current, p.newWord);
        }
    }
    if (repaired > 0) {
        LOGI("巡检：发现 %zu 处补丁被系统回收，已重新写入", repaired);
    }
}

bool RefreshConfigLocked(State* state, bool allowBakedFallback, bool fast) {
    Config config;
    std::vector<uint8_t> bytes;
    char path[512]{};
    uint64_t mtime = 0;

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
            LOGI("已加载配置 %s（总开关=%s，功能 %zu 项，文件夹每行 %u 个）", path,
                 config.masterEnabled() ? "开" : "关", config.enabled.size(),
                 config.folderCols);

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
    if (state->imageId == 0) state->imageId = ImageIdentity(state->image);

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
        s += ok[i] ? "=已生效" : "=未生效";
    }
    return s;
}

const char* WhyOf(const PlanResult& plan, uint32_t num) {
    switch (num) {
        case kFeatureNoClear: return plan.why9;
        case kFeatureFolderCols: return plan.why16;
        case kFeatureHideClear: return plan.why18;
        case kFeaturePadGrid: return plan.why4;
        case kFeaturePhoneGrid: return plan.why19;
        case kFeatureFoldGrid: return plan.why20;
        case kFeatureIconSize: return plan.why21;
        default: return "";
    }
}

std::string JoinEnabled(const Config& cfg) {
    std::string s;
    for (uint32_t n : cfg.enabled) {
        if (!s.empty()) s += ",";
        s += std::to_string(n);
    }
    return s.empty() ? std::string("（一个都没开）") : s;
}

std::string BuildStatusText(const State* state) {
    const Config& cfg = state->config;
    const PlanResult& plan = state->lastPlan;
    const PatchStats& stats = state->patchStats;
    char line[640];
    std::string s;

    s += "HomeTweaks 模块状态 v";
    s += kModuleVersion;
    s += "\n";

    time_t now = time(nullptr);
    struct tm tmv {};
    localtime_r(&now, &tmv);
    snprintf(line, sizeof(line), "写入时间: %04d-%02d-%02d %02d:%02d:%02d\n",
             tmv.tm_year + 1900, tmv.tm_mon + 1, tmv.tm_mday, tmv.tm_hour, tmv.tm_min,
             tmv.tm_sec);
    s += line;

    if (state->imageFound) {
        snprintf(line, sizeof(line), "镜像: 已找到 %s（%s）\n", kTargetLibName,
                 state->imageHow[0] != '\0' ? state->imageHow : "来源未知");
        s += line;
        snprintf(line, sizeof(line), "镜像指纹: %016llx（站点缓存按它索引）\n",
                 static_cast<unsigned long long>(state->imageId));
        s += line;
    } else {
        s += "镜像: 还没找到 libapp.so（模块可能刚注入，或者桌面根本不是它）\n";
    }

    if (state->imageFound && state->image.path[0] != '\0') {
        char apkPath[sizeof(state->image.path)];
        snprintf(apkPath, sizeof(apkPath), "%s", state->image.path);
        char* bang = strstr(apkPath, "!/");
        if (bang != nullptr) *bang = '\0';
        struct stat st {};
        if (stat(apkPath, &st) == 0) {
            snprintf(line, sizeof(line), "桌面 APK: %s（%lld 字节）\n", apkPath,
                     static_cast<long long>(st.st_size));
        } else {
            snprintf(line, sizeof(line), "桌面 APK: %s（stat 失败）\n", apkPath);
        }
        s += line;
    }

    snprintf(line, sizeof(line), "站点来源: 符号表 / 签名（无写死地址）\n");
    s += line;
    snprintf(line, sizeof(line), "站点来源: %s\n",
             state->siteSource[0] != '\0' ? state->siteSource : "（本次还没定位）");
    s += line;
    const char* sym = SymbolIndex::Instance().status();
    snprintf(line, sizeof(line), "运行期符号表: %s\n",
             (sym != nullptr && sym[0] != '\0') ? sym : "（还没走到这一步）");
    s += line;

    if (!state->configLoaded) {
        s += "配置: 还没读到\n";
    } else if (false) {
        s += "配置: 读不到任何配置文件，先用写死的默认配置（功能 9 + 16）\n";
    } else {
        snprintf(line, sizeof(line), "配置来源: %s\n", state->configPath);
        s += line;
    }
    if (state->configLoaded) {
        snprintf(line, sizeof(line), "总开关: %s；启用功能: %s\n",
                 cfg.masterEnabled() ? "开" : "关", JoinEnabled(cfg).c_str());
        s += line;
        snprintf(line, sizeof(line),
                 "数值: 文件夹每行 %u；手机 %u 列 × %u 行（0=自动）；折叠屏 %u×%u；平板 %u×%u；图标 ×%.4g\n",
                 cfg.folderCols, cfg.phoneCols, cfg.phoneRows, cfg.foldMajor, cfg.foldMinor,
                 cfg.padMajor, cfg.padMinor, IconScaleValueFromCode(cfg.iconScaleCode));
        s += line;
    }

    const uint32_t wanted = WantedFeatureMask(cfg);
    s += "功能状态:\n";
    for (const FeatureSlot& f : kFeatureSlots) {
        const bool enabled = cfg.masterEnabled() && (wanted & f.bit) != 0;
        std::string value;
        if (!enabled) {
            value = (f.num == kFeatureHideClear && cfg.masterEnabled() &&
                     (wanted & kWantNoClear) != 0)
                    ? "未启用（与功能 9 互斥，同时打开时只应用 9）"
                    : "未启用";
        } else if (FeatureOk(state->sites, f.num)) {
            value = "已生效";
            if (f.num == kFeatureFolderCols) {
                value += "（每行 " + std::to_string(cfg.folderCols) + " 个）";
            } else if (f.num == kFeaturePhoneGrid) {
                value += "（" + std::to_string(cfg.phoneCols) + " 列 × ";
                value += (cfg.phoneRows == kPhoneRowsAuto)
                         ? std::string("官方自动行")
                         : std::to_string(cfg.phoneRows) + " 行";
                value += "）";
                if (cfg.phoneRows != kPhoneRowsAuto && !state->sites.ok19r) {
                    value += "，但行数那两份落点没定位到（行数仍是官方值）";
                }
            } else if (f.num == kFeatureFoldGrid) {
                value += "（" + std::to_string(cfg.foldMajor) + "×" +
                         std::to_string(cfg.foldMinor) + "）";
            } else if (f.num == kFeaturePadGrid) {
                value += "（" + std::to_string(cfg.padMajor) + "×" +
                         std::to_string(cfg.padMinor) + "）";
            } else if (f.num == kFeatureIconSize) {
                char scale[32];
                snprintf(scale, sizeof(scale), "%.4g", IconScaleValueFromCode(cfg.iconScaleCode));
                value += "（×";
                value += scale;
                value += "；官方滑块与 Rust 侧 min/max 已失效）";
            }
        } else {
            const char* why = WhyOf(plan, f.num);
            value = "没生效 —— 定位失败：";
            value += (why != nullptr && why[0] != '\0') ? why : "原因未记录";
        }
        snprintf(line, sizeof(line), "  功能 %u（%s）: %s\n", f.num, f.name, value.c_str());
        s += line;
    }

    snprintf(line, sizeof(line),
             "补丁: 共 %zu 条（新写入 %zu，已就位 %zu，越界 %zu，原值不符 %zu，写入失败 %zu）\n",
             stats.total, stats.written, stats.already, stats.outOfRange, stats.mismatch,
             stats.failed);
    s += line;

    s += "\n说明：功能 4（平板）/ 19（手机）/ 20（折叠屏）是三条互不影响的链路，\n";
    s += "一台设备只会走其中一条 —— 手上是手机的话，只有「手机桌面网格」那条会真的\n";
    s += "改到布局。这三项改的是桌面启动时那一次网格初始化，所以必须重启桌面；\n";
    s += "折叠屏 / 平板还要再清一次「系统桌面」的应用数据才会重算。\n";
    s += "功能 21（图标大小）三端各有一条独立链路，改的是「图标尺寸 = 基础尺寸 × 用户\n";
    s += "倍率」那一步里的倍率，所以不管手上是哪种设备都会生效，同样要重启桌面。\n";
    return s;
}

void WriteStatusFile(const State* state, bool alsoExternal) {
    const std::string text = BuildStatusText(state);
    const uint8_t* data = reinterpret_cast<const uint8_t*>(text.data());
    if (!WriteWholeFileAtomic(kLocalStatusPath, data, text.size())) {
        LOGW("状态文件写入失败：%s", kLocalStatusPath);
    }
    if (alsoExternal) {
        WriteWholeFileAtomic(kExternalStatusPath, data, text.size());
    }
}

void SettleLocked(State* state, bool allowScan) {
    if (state == nullptr || !state->imageFound || !state->configLoaded) return;
    if (!state->repatchNeeded) {
        if (!state->statusWritten) {
            WriteStatusFile(state, allowScan);
            state->statusWritten = true;
            state->repatchNeeded = false;
        }
        return;
    }

    RevertApplied(state);
    AcquireSites(state, allowScan);

    const uint64_t t0 = NowMs();
    PlanResult plan;
    DerivePatches(CodeView(state->image), state->config, state->sites, &plan);
    const PatchStats stats = ApplyPatches(state, plan.patches);
    state->lastPlan = plan;
    state->patchStats = stats;

    const uint32_t wanted = WantedFeatureMask(state->config);
    const uint32_t done = (MaskOf(state->sites) | state->attemptedMask) & wanted;
    state->repatchNeeded = (done != wanted);

    LOGI("配置落地：共 %zu 条补丁（%s），用时 %llu ms%s",
         plan.patches.size(), DescribePlan(plan).c_str(),
         static_cast<unsigned long long>(NowMs() - t0),
         state->repatchNeeded ? "（尚未全部定位，后台继续）" : "");
    LogPlanReasons(plan, state->config);

    WriteStatusFile(state, allowScan);
    state->statusWritten = true;
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

            if (state->imageFound && state->configLoaded && !state->repatchNeeded) {
                periodMs = 1500;
            }
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
 * Look a function up by the name the launcher's own symbol table gives it. Reading the address out
 * of the image rather than carrying it across an OTA is the whole point: the name is part of the
 * launcher's build, the address is not.
 */
bool HomeTweaksFindSymbol(const char *name, uint32_t *outVa, uint32_t *outSize) {
    if (name == nullptr || outVa == nullptr || outSize == nullptr) return false;
    TryDiscoverImage();
    std::lock_guard<std::mutex> work(g_workMutex);
    if (!g_state.imageFound) return false;
    SymbolIndex &index = SymbolIndex::Instance();
    if (!index.EnsureLoaded(g_state.image)) return false;
    return index.Find(name, outVa, outSize);
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
        g_state.statusWritten = false;
        g_state.packTried = false;
        g_state.cacheTried = false;
    }
    WakeMonitor();
}

} // namespace hometweaks
