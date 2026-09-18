// SPDX-License-Identifier: Apache-2.0
#pragma once

#include "blob.h"
#include "scanner.h"

#include <stddef.h>
#include <stdint.h>
#include <vector>

namespace hometweaks {

constexpr uint32_t kMaxNoClearSites = 32;

constexpr uint32_t kMaxPadSites = 4;
constexpr uint32_t kMaxPhoneSites = 2;
constexpr uint32_t kMaxPhoneRowSites = 2;
constexpr uint32_t kMaxFoldSites = 2;
constexpr uint32_t kMaxFoldGuards = 16;

constexpr uint32_t kMaxIconFuncs = 4;
constexpr uint32_t kMaxIconSites = 6;
constexpr uint32_t kIconFuncMaxBytes = 0x1000;

struct RowTarget {
    const char* sig;
    uint32_t window;
};

constexpr uint32_t kWantNoClear = 1u << 0;
constexpr uint32_t kWantFolderCols = 1u << 1;
constexpr uint32_t kWantHideClear = 1u << 2;
constexpr uint32_t kWantPadGrid = 1u << 3;
constexpr uint32_t kWantFoldGrid = 1u << 4;
constexpr uint32_t kWantPhoneGrid = 1u << 5;
constexpr uint32_t kWantIconSize = 1u << 6;

struct LocatedSites {
    bool ok9 = false;
    bool ok16 = false;
    bool ok18 = false;
    bool ok4 = false;
    bool ok19 = false;
    bool ok20 = false;
    bool ok21 = false;
    char why9[192]{};
    char why16[192]{};
    char why18[192]{};
    char why4[192]{};
    char why19[192]{};
    char why20[192]{};
    char why21[192]{};

    uint32_t overlayVa = 0;
    uint32_t foldVa = 0;
    uint32_t constWord = 0;
    uint32_t noClearSites[kMaxNoClearSites]{};
    uint32_t noClearCount = 0;

    uint32_t movzVa = 0;
    uint32_t movzRd = 0;
    uint32_t storeVa = 0;
    uint32_t gateVa = 0;
    uint32_t colsOffset = 0;

    bool ok16sq = false;
    char why16sq[192]{};
    uint32_t squareFuncVa = 0;
    uint32_t squareSiteVa = 0;
    uint32_t squareCalleeVa = 0;

    uint32_t svVa = 0;
    uint32_t initVa = 0;
    uint32_t bneAt = 0;
    uint32_t bneTgt = 0;
    uint32_t tbAt = 0;
    uint32_t tbTgt = 0;
    uint32_t initBlAt = 0;
    uint32_t revTo = 0;

    uint32_t padVa = 0;
    uint32_t padSiteVa[kMaxPadSites]{};
    uint32_t padSiteRd[kMaxPadSites]{};
    uint32_t padSiteImm[kMaxPadSites]{};

    uint32_t phoneVa = 0;
    uint32_t phoneSiteVa[kMaxPhoneSites]{};
    uint32_t phoneSiteRd[kMaxPhoneSites]{};
    uint32_t phoneSiteImm[kMaxPhoneSites]{};

    bool ok19r = false;
    char why19r[192]{};
    uint32_t phoneRowFuncVa[kMaxPhoneRowSites]{};
    uint32_t phoneRowSdivVa[kMaxPhoneRowSites]{};
    uint32_t phoneRowSiteVa[kMaxPhoneRowSites]{};
    uint32_t phoneRowRd[kMaxPhoneRowSites]{};

    uint32_t foldHva = 0;
    uint32_t foldSiteVa[kMaxFoldSites]{};
    uint32_t foldSiteRd[kMaxFoldSites]{};
    uint32_t foldSiteImm[kMaxFoldSites]{};
    uint32_t foldGuardVa[kMaxFoldGuards]{};
    uint32_t foldGuardTgt[kMaxFoldGuards]{};
    uint32_t foldGuardCount = 0;

    uint32_t iconFuncVa[kMaxIconFuncs]{};
    uint32_t iconFuncSiteBeg[kMaxIconFuncs]{};
    uint32_t iconFuncSiteCount[kMaxIconFuncs]{};
    uint32_t iconSiteVa[kMaxIconSites]{};
    uint32_t iconSiteRd[kMaxIconSites]{};
    uint32_t iconSiteCount = 0;

    bool AllOk() const {
        return ok9 && ok16 && ok18 && ok4 && ok19 && ok20 && ok21;
    }
    bool AnyOk() const {
        return ok9 || ok16 || ok18 || ok4 || ok19 || ok20 || ok21;
    }
};

struct PlanPatch {
    uint32_t va;
    uint32_t expect;
    uint32_t patch;
    const char* what;
};

struct PlanResult {
    std::vector<PlanPatch> patches;
    bool ok9 = false;
    bool ok16 = false;
    bool ok18 = false;
    bool ok4 = false;
    bool ok19 = false;
    bool ok19r = false;
    bool ok20 = false;
    bool ok21 = false;
    bool ok16sq = false;
    char why9[192]{};
    char why16[192]{};
    char why18[192]{};
    char why4[192]{};
    char why19[192]{};
    char why19r[192]{};
    char why20[192]{};
    char why21[192]{};
    char why16sq[192]{};
};

uint32_t WantedFeatureMask(const Config& cfg);

void LocateSites(const CodeView& code, const Config& want, LocatedSites* out);

bool ValidateSites(const CodeView& code, LocatedSites& sites);

bool ValidateSitesShape(const CodeView& code, LocatedSites& sites);

bool LoadSitePack(const char* path, LocatedSites* out);
size_t SitePackPaths(const char** out, size_t maxCount);

void DerivePatches(const CodeView& code, const Config& cfg, const LocatedSites& sites,
                   PlanResult* out);

bool SerializeSites(const LocatedSites& sites, std::vector<uint8_t>* out);
bool ParseSites(const uint8_t* data, size_t size, LocatedSites* out);

bool FeatureOk(const LocatedSites& sites, uint32_t num);
void KeepOnlyFeature(LocatedSites* sites, uint32_t num);
void CopyFeature(LocatedSites* dst, const LocatedSites& src, uint32_t num);

bool BuildPatchPlan(const CodeView& code, const Config& cfg, PlanResult* out);

void SetForceSymbolOnlyForTest(bool on);

}
