/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Desktop Dart hook view of the NativeHookRuntime image layer.
 *
 * The generic implementation (mapping inventory, container attribution,
 * generations, mapping-state comparison) lives in nativehook/native_image.h
 * and is shared with every future native/Rust target; this header only adds
 * the Dart AOT specifics (`libapp.so` discovery) and keeps the historical
 * dock_motion names so the desktop feature reads naturally.
 */
#pragma once

#include "nativehook/native_image.h"
#include "nativehook/nhk_base.h"

#include <cstdint>
#include <istream>
#include <optional>
#include <string>
#include <string_view>
#include <utility>
#include <vector>

namespace dock_motion {

inline constexpr size_t kMaxExecutableMappings = nhk::kMaxExecutableMappings;
inline constexpr size_t kMaxExecutableCodeBytes = nhk::kMaxExecutableCodeBytes;

using CodeSource = nhk::CodeSource;
using ExecutableMapping = nhk::ExecutableMapping;
using RuntimeGenerationKey = nhk::RuntimeGenerationKey;
using nhk::generation_key;
using nhk::runtime_generations;
using nhk::strip_deleted;
using FileMapping = nhk::FileMapping;
using nhk::parse_file_mappings;
using LibappContainer = nhk::ImageContainer;
using EmbeddedImage = nhk::EmbeddedImage;
using nhk::parse_embedded_image;
using nhk::source_at;
using nhk::MappingState;
using nhk::mapping_state;
using nhk::sources_for;
using nhk::load_le16;
using nhk::load_le32;
using nhk::load_le64;
using nhk::host_page_size;
using nhk::page_down;
using nhk::page_up;

/** The Dart AOT snapshot is always named `libapp.so` on this runtime. */
inline bool libapp_path(std::string_view path) {
    path = strip_deleted(path);
    return path == "libapp.so" || path.ends_with("/libapp.so");
}

/** Stored `libapp.so` entry of an APK container (any `lib/<abi>/` directory). */
inline std::optional<std::pair<uint64_t, uint64_t>> zip_stored_libapp(
    const std::string &path) {
    return nhk::zip_stored_entry(path, "libapp.so");
}

/** Select executable ranges owned by the discovered Dart image(s). */
inline std::vector<ExecutableMapping> executable_libapp_mappings(
    const std::vector<FileMapping> &mappings,
    const std::vector<LibappContainer> &containers) {
    return nhk::owned_image_mappings(mappings, containers);
}

inline std::vector<ExecutableMapping> executable_libapp_mappings(std::istream &maps) {
    return nhk::bare_image_mappings(maps, libapp_path);
}

} // namespace dock_motion

// The desktop feature previously owned these as free functions; keep the same
// shape for code outside the namespace.
using dock_motion::host_page_size;
using dock_motion::page_down;
using dock_motion::page_up;
