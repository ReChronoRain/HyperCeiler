// SPDX-License-Identifier: Apache-2.0
#pragma once

#include "common.h"

#include <vector>

namespace hometweaks {

constexpr uint32_t kBlobFormatV2 = 2;
constexpr uint32_t kBlobFormatV3 = 3;
constexpr uint32_t kBlobFormatV4 = 4;
constexpr uint32_t kBlobFormatV5 = 5;
constexpr uint32_t kBlobFormatV6 = 6;
constexpr uint32_t kBlobFormatV7 = 7;

constexpr uint32_t kFeaturePadGrid = 4;
constexpr uint32_t kFeatureNoClear = 9;
constexpr uint32_t kFeatureFolderCols = 16;
constexpr uint32_t kFeatureHideClear = 18;
constexpr uint32_t kFeaturePhoneGrid = 19;
constexpr uint32_t kFeatureFoldGrid = 20;
constexpr uint32_t kFeatureIconSize = 21;

constexpr uint32_t kFolderColsMin = 1;
constexpr uint32_t kFolderColsMax = 16;

constexpr uint32_t kPadGridMin = 2;
constexpr uint32_t kPadGridMax = 16;

constexpr uint32_t kPhoneColsMin = 1;
constexpr uint32_t kPhoneColsMax = 8;

constexpr uint32_t kPhoneRowsAuto = 0;
constexpr uint32_t kPhoneRowsMin = 2;
constexpr uint32_t kPhoneRowsMax = 16;

constexpr uint32_t kDefaultPadMajor = 8;
constexpr uint32_t kDefaultPadMinor = 5;
constexpr uint32_t kDefaultPhoneCols = 5;
constexpr uint32_t kDefaultFoldMajor = 8;
constexpr uint32_t kDefaultFoldMinor = 5;

constexpr uint32_t kIconScaleSlot = 7;
constexpr uint32_t kIconScaleCodeDefault = 0x70u;

inline bool IconScaleCodeValid(uint32_t code) {
    if (code > 0xFFu) return false;
    if ((code & 0x80u) != 0u) return false;
    if (((code >> 6) & 1u) == 1u) return true;
    return ((code >> 4) & 3u) == 0u;
}

double IconScaleValueFromCode(uint32_t code);

struct Config {
    uint32_t flags = 0;
    std::vector<uint32_t> enabled;
    uint32_t folderCols = 3;
    uint32_t padMajor = kDefaultPadMajor;
    uint32_t padMinor = kDefaultPadMinor;
    uint32_t phoneCols = kDefaultPhoneCols;
    uint32_t phoneRows = kPhoneRowsAuto;
    uint32_t foldMajor = kDefaultFoldMajor;
    uint32_t foldMinor = kDefaultFoldMinor;
    uint32_t iconScaleCode = kIconScaleCodeDefault;

    bool masterEnabled() const { return (flags & 1u) != 0; }
    bool Has(uint32_t num) const {
        for (uint32_t n : enabled) {
            if (n == num) return true;
        }
        return false;
    }
};

bool ParseConfigBlob(const uint8_t* data, size_t size, Config* out);

size_t ConfigCandidatePaths(const char** out, size_t maxCount);

constexpr const char* kLocalConfigPath = "/data/user/0/com.miui.home/files/hometweaks.bin";

constexpr const char* kLocalStatusPath = "/data/user/0/com.miui.home/files/hometweaks.status";

constexpr const char* kExternalStatusPath =
        "/storage/emulated/0/Download/HomeTweaks/hometweaks.status";

size_t SitePackPaths(const char** out, size_t maxCount);

bool LoadConfigBlob(Config* out, std::vector<uint8_t>* bytesOut, char* usedPath, size_t pathCap,
                    uint64_t* outMtime);

bool LoadConfigBlobFast(Config* out, std::vector<uint8_t>* bytesOut, char* usedPath,
                        size_t pathCap, uint64_t* outMtime);

/** Candidate paths LoadConfigBlobFast covers: the local file plus the two adb drop points. */
constexpr size_t kFastConfigPaths = 3;

/** Cap LoadConfigBlob uses. ConfigCandidatePaths currently returns fewer; both clamp. */
constexpr size_t kAllConfigPaths = 16;

/**
 * Cheap change probe over the config candidates: the newest readable one and its mtime, gathered
 * with stat() only - no open, no read, no parse.
 *
 * The monitor loop refreshes the config on every pass, and reading all candidates each time is
 * pure waste in the steady state, where nothing has changed. Comparing this fingerprint against
 * the one observed at the last read is what lets the caller skip the read entirely. It is
 * deliberately a property of the *candidate set* (newest path + mtime, the same election
 * LoadConfigBlob runs) rather than of the file that won, so a newest-but-unparsable candidate
 * cannot make the comparison fail forever.
 *
 * Returns false when no candidate exists, which the caller must treat as "read it".
 */
bool NewestConfigCandidate(char* usedPath, size_t pathCap, uint64_t* outMtime, size_t maxPaths);

bool ReadWholeFile(const char* path, std::vector<uint8_t>* out, uint64_t* outMtime);

bool WriteWholeFileAtomic(const char* path, const uint8_t* data, size_t size);

bool MirrorConfigToPath(const char* path, const uint8_t* data, size_t size,
                        uint64_t sourceMtime);

constexpr size_t kMaxBlobBytes = 8u * 1024u * 1024u;

}
