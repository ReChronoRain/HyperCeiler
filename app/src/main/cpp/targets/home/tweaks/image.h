// SPDX-License-Identifier: Apache-2.0
#pragma once

#include "common.h"

#include <string>

namespace hometweaks {

struct Segment {
    uintptr_t begin;
    uintptr_t end;
    uint32_t flags;
};

struct Image {
    uintptr_t base;
    char path[512];
    Segment segments[16];
    size_t segmentCount;
    uint64_t apkEntryOffset;
    bool fromApkEntry;
};

bool FindImageByName(const char* basename, Image* out);

bool FindImageFromApkMaps(const char* apkMarker, const char* libName, Image* out);

/**
 * True only when the launcher APK this process has mapped was read and carries no entry of that
 * name at all - the property that separates "this launcher variant cannot carry the image" from
 * "the image has not been located yet". False means "not established" and must not stop a retry.
 */
bool LauncherApkLacksEntry(const char* apkMarker, const char* libName);

void DumpLoadedLibraries(int maxEntries);
void DumpApkMaps(int maxEntries);
void DumpImage(const Image& image);

bool RangeInImage(const Image& image, uintptr_t address, size_t length);

bool ReadImageFile(const Image& image, uint64_t offset, void* out, size_t length);

}
