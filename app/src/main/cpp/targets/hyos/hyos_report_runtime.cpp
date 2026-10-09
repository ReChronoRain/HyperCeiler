/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "hyos_report_runtime.h"
#include "hyos_control.h"
#include "hyos_runtime_guard.h"
#include "targets/home/home_report_hooks.h"
#include "targets/home/home_report_guard.h"
#include "nativehook/elf_image.h"
#include "nativehook/memory_io.h"
#include <zygisk_next_api.h>
#include <android/log.h>
#include <array>
#include <optional>
#include <vector>

namespace {
constexpr char kTag[] = "HyperCeiler.HomeReport";
bool g_installed = false; // The single control worker owns discovery/installation.
int g_last_status = -1;
struct Context {
    const ZygiskNextAPI *api;
    ZnSymbolResolver *resolver;
    uintptr_t base;
    std::vector<nhk::elf::ProgramSegment> segments;
};

bool verified_image(uintptr_t base, std::vector<nhk::elf::ProgramSegment> &segments) {
    if (base == 0 || base > UINTPTR_MAX - hyos_guard::kDartNoteVa) return false;
    std::array<uint8_t, 32> note{};
    if (!nhk::safe_read(base + hyos_guard::kDartNoteVa,
            std::as_writable_bytes(std::span(note))) || note != hyos_guard::kDartBuildIdNote) return false;
    // ELF/program-header parsing is bounded. Require an AArch64 DSO and take
    // executable ownership from this verified image, not a basename guess.
    std::array<std::byte, 4096> header{};
    if (!nhk::safe_read(base, header)) return false;
    auto parsed = nhk::elf::parse_program_segments(header);
    if (!parsed) return false;
    segments = std::move(*parsed);
    return true;
}

bool contains_code(const Context &ctx, uintptr_t address, size_t size) {
    if (address < ctx.base || size == 0) return false;
    const uint64_t va = address - ctx.base;
    for (const auto &segment : ctx.segments) {
        if (segment.type != nhk::elf::kProgramTypeLoad
            || (segment.flags & nhk::elf::kFlagExecute) == 0
            || (segment.flags & nhk::elf::kFlagWrite) != 0 || va < segment.vaddr) continue;
        const auto delta = va - segment.vaddr;
        if (delta < segment.filesz && size <= segment.filesz - delta) return true;
    }
    return false;
}

void enumerate(void *opaque, home_report::SymbolVisitor visitor, void *data) {
    auto &ctx = *static_cast<Context *>(opaque);
    struct VisitorContext { Context *context; home_report::SymbolVisitor visitor; void *data; } state{&ctx, visitor, data};
    ctx.api->forEachSymbols(ctx.resolver,
        [](const char *name, void *address, size_t size, void *opaque_state) {
            auto &state = *static_cast<VisitorContext *>(opaque_state);
            if (name == nullptr || home_report::kSymbol != name) return true;
            if (!contains_code(*state.context, reinterpret_cast<uintptr_t>(address), size)) {
                // Preserve a matching-but-invalid row so the installer rejects
                // it instead of silently selecting another valid-looking row.
                return state.visitor(name, reinterpret_cast<void *>(1), size, state.data);
            }
            return state.visitor(name, address, size, state.data);
        }, &state);
}
} // namespace

void hyperceiler_hyos_retry_home_report(const ZygiskNextAPI *api) {
    if (g_installed || !hyos_control::feature_enabled(hyos_control::kHideReport)
        || api == nullptr || api->newSymbolResolver == nullptr || api->freeSymbolResolver == nullptr
        || api->getBaseAddress == nullptr || api->forEachSymbols == nullptr
        || api->inlineHook == nullptr || api->inlineUnhook == nullptr) return;
    ZnSymbolResolver *resolver = api->newSymbolResolver("libapp.so", nullptr);
    if (resolver == nullptr) return; // The runtime may not have mapped Dart yet.
    Context context{api, resolver, reinterpret_cast<uintptr_t>(api->getBaseAddress(resolver)), {}};
    if (!verified_image(context.base, context.segments)) {
        api->freeSymbolResolver(resolver);
        if (g_last_status != -2) __android_log_print(ANDROID_LOG_WARN, kTag,
            "declined: loaded Dart image identity/header does not match supplied Home 7719");
        g_last_status = -2;
        return;
    }
    if (!hyos_control::feature_enabled(hyos_control::kHideReport)) {
        api->freeSymbolResolver(resolver);
        return;
    }
    const auto result = home_report::install({&context, enumerate, api->inlineHook, api->inlineUnhook});
    api->freeSymbolResolver(resolver);
    const auto status = static_cast<int>(result.status);
    if (status != g_last_status) __android_log_print(ANDROID_LOG_INFO, kTag,
        "guarded report predicate=%s", home_report::status_name(result.status));
    g_last_status = status;
    g_installed = result.status == home_report::InstallStatus::Installed
        || result.status == home_report::InstallStatus::AlreadyInstalled;
}
