/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Contract test for the desktop Dart resolver's NativeHookRuntime output
 * (resolve_hook_targets): on a real libapp.so it must produce exactly one
 * candidate with three inline targets whose RVAs round-trip and whose
 * original words match the snapshot the resolver itself used.
 *
 * Usage: NativeHookDartContractTest <libapp.so> [more libapp.so ...]
 * With no arguments the test reports SKIP (there is no fixture to assert on).
 * With a fixture supplied, a read failure, a resolver miss or a broken
 * contract is a FAILURE - a regression must never be reported as a skip.
 */
#include "../../app/src/main/cpp/targets/home/dock_native_resolver.h"
#include "../../app/src/main/cpp/nativehook/resolver.h"

#include <cstdio>
#include <fstream>
#include <iterator>
#include <string>
#include <vector>

static int failures = 0;

static void check(bool condition, const char *message) {
    if (!condition) {
        std::printf("FAIL: %s\n", message);
        ++failures;
    }
}

static bool load_words(const char *path, std::vector<uint32_t> &out) {
    std::ifstream file(path, std::ios::binary);
    if (!file) return false;
    file.seekg(0, std::ios::end);
    const std::streamoff bytes = file.tellg();
    if (bytes <= 0 || bytes % 4 != 0) return false;
    file.seekg(0, std::ios::beg);
    out.resize(static_cast<size_t>(bytes / 4));
    file.read(reinterpret_cast<char *>(out.data()),
        static_cast<std::streamsize>(out.size() * 4));
    return static_cast<size_t>(file.gcount()) == out.size() * 4;
}

int main(int argc, char **argv) {
    if (argc < 2) {
        std::printf("SKIP NativeHookDartContractTest (no libapp.so fixture given)\n");
        return 0;
    }
    for (int arg = 1; arg < argc; ++arg) {
        std::vector<uint32_t> words;
        if (!load_words(argv[arg], words)) {
            std::printf("FAIL: cannot read fixture %s\n", argv[arg]);
            ++failures;
            continue;
        }
        // The file is mapped at load bias 0 for the test; the resolver works
        // on RVAs either way.
        const nhk::arm64::CodeRange range{0,
            std::span<const uint32_t>(words.data(), words.size())};
        const auto targets = dock_motion::resolve_hook_targets(
            std::span<const nhk::arm64::CodeRange>(&range, 1), /*load_bias=*/0);
        if (!targets) {
            // A fixture that cannot be resolved is a regression, not a skip:
            // the caller asked for this image explicitly.
            std::printf("FAIL: no unique resolution in %s\n", argv[arg]);
            ++failures;
            continue;
        }
        check(targets->evidence.candidate_count == 1, "exactly one candidate");
        check(nhk::evidence_acceptable(targets->evidence), "evidence acceptable");
        check(targets->evidence.stage == "dart-semantic-unique", "stage recorded");
        check(targets->targets.size() == 3, "three inline targets");
        for (size_t i = 0; i < 3; ++i) {
            const auto &target = targets->targets[i];
            check(target.kind == nhk::TargetKind::kInline, "inline kind");
            check(target.original_words.size() == 4, "four original words");
            // animate/set are whole Dart functions (prologue-opened); the scale
            // target is a call-site body that legitimately opens on its BL.
            if (i != 0) {
                check(target.original_words[0] == 0xa9bf79fd
                    && target.original_words[1] == 0xaa0f03fd,
                    "animate/set open with the Dart prologue");
            }
            // rva must round-trip through the snapshot range.
            const auto mapped = nhk::arm64::at(
                std::span<const nhk::arm64::CodeRange>(&range, 1),
                static_cast<uintptr_t>(target.rva), 4);
            check(mapped.size() == 4
                && std::equal(mapped.begin(), mapped.end(), target.original_words.begin()),
                "rva resolves back to the declared words");
        }
        check(targets->resolution.scale != targets->resolution.animate
            && targets->resolution.animate != targets->resolution.set
            && targets->resolution.scale != targets->resolution.set,
            "targets are distinct functions");
        std::printf("contract ok for %s: scale=%llx animate=%llx set=%llx\n",
            argv[arg],
            static_cast<unsigned long long>(targets->targets[0].rva),
            static_cast<unsigned long long>(targets->targets[1].rva),
            static_cast<unsigned long long>(targets->targets[2].rva));
    }
    if (failures == 0) std::printf("NativeHookDartContractTest passed\n");
    return failures == 0 ? 0 : 1;
}
