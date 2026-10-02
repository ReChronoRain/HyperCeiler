// SPDX-License-Identifier: Apache-2.0
#pragma once

#include <android/log.h>
#include <stddef.h>
#include <stdint.h>

#define HT_LOG_TAG "HomeTweaks"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, HT_LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, HT_LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, HT_LOG_TAG, __VA_ARGS__)

namespace hometweaks {

struct NativeAPIEntries {
    uint32_t version;
    int (*hookFunc)(void* target, void* replacement, void** backup);
    int (*unhookFunc)(void* target);
};

constexpr const char* kTargetLibName = "libapp.so";

constexpr const char* kDesktopApkMarker = "/com.miui.home-";

constexpr char kBlobMagic[4] = {'H', 'T', 'W', 'K'};
constexpr uint32_t kBlobFormatVersion = 1;
constexpr uint32_t kBlobFlagEnabled = 1u << 0;

constexpr size_t kMd5HexLen = 32;
constexpr size_t kMaxVersions = 64;
constexpr size_t kMaxPatches = 4096;
constexpr size_t kMaxLabelLen = 96;

}
