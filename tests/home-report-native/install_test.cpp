/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "targets/home/home_report_guard.h"
#include "targets/home/home_report_hooks.h"

#include <array>
#include <cassert>
#include <cstring>
#include <fstream>
#include <iostream>
#include <string>
#include <thread>
#include <vector>

extern "C" void hc_home_report_entry() {}
extern "C" void *hc_home_report_original;
using home_report::InstallStatus;

struct Row { const char *name; void *address; size_t size; };
std::vector<Row> rows;
int hooks = 0, unhooks = 0, hook_result = 0, unhook_result = 0;
bool publish_original = true;
void original() {}
void enumerate(void *, home_report::SymbolVisitor visit, void *data) {
    for (const auto &row : rows) {
        if (!visit(row.name, row.address, row.size, data)) break;
    }
}
int hook(void *, void *replacement, void **saved) {
    ++hooks;
    assert(replacement == reinterpret_cast<void *>(&hc_home_report_entry));
    assert(saved == &hc_home_report_original);
    if (hook_result == 0 && publish_original) *saved = reinterpret_cast<void *>(&original);
    return hook_result;
}
int unhook(void *) { ++unhooks; return unhook_result; }

int main(int argc, char **argv) {
    assert(argc == 3);
    alignas(4) std::array<std::byte, home_report::kBodyBytes> bytes{};
    std::ifstream input(argv[2], std::ios::binary);
    input.read(reinterpret_cast<char *>(bytes.data()), bytes.size());
    assert(input.gcount() == static_cast<std::streamsize>(bytes.size()));
    const std::string scenario = argv[1];
    rows.push_back({home_report::kSymbol.data(), bytes.data(), bytes.size()});
    home_report::Runtime runtime{nullptr, enumerate, hook, unhook};
    auto call = [&] { return home_report::install(runtime).status; };
    assert(!home_report::enabled());
    if (scenario == "guard") {
        assert(home_report::accepts(home_report::kSymbol, bytes));
        assert(!home_report::accepts("UninstallController._shouldShowReportElse", bytes));
        assert(!home_report::accepts(home_report::kSymbol, std::span(bytes).first(bytes.size()-4)));
        for (size_t byte = 0; byte < bytes.size(); ++byte) {
            for (unsigned bit = 0; bit < 8; ++bit) {
                auto changed = bytes;
                changed[byte] ^= std::byte{static_cast<unsigned char>(1u << bit)};
                assert(!home_report::accepts(home_report::kSymbol, changed));
            }
        }
    } else if (scenario == "not_loaded_then_retry") {
        rows.clear();
        assert(call() == InstallStatus::TargetNotFound && hooks == 0);
        rows.push_back({home_report::kSymbol.data(), bytes.data(), bytes.size()});
        assert(call() == InstallStatus::Installed && hooks == 1);
    } else if (scenario == "invalid_runtime") {
        runtime.inline_unhook = nullptr;
        assert(call() == InstallStatus::InvalidRuntime && hooks == 0);
    } else if (scenario == "unreadable") {
        rows[0].address = reinterpret_cast<void *>(4);
        assert(call() == InstallStatus::UnreadableBody && hooks == 0);
    } else if (scenario == "wrong_size") {
        rows[0].size -= 4;
        assert(call() == InstallStatus::UnsupportedBody && hooks == 0);
    } else if (scenario == "unaligned") {
        rows[0].address = bytes.data() + 1;
        assert(call() == InstallStatus::UnsupportedBody && hooks == 0);
    } else if (scenario == "mutated_body") {
        bytes.back() ^= std::byte{1};
        assert(call() == InstallStatus::UnsupportedBody && hooks == 0);
    } else if (scenario == "wrong_name") {
        rows[0].name = "UninstallController._shouldShowReportElse";
        assert(call() == InstallStatus::TargetNotFound && hooks == 0);
    } else if (scenario == "duplicate_address") {
        rows.push_back({home_report::kSymbol.data(), bytes.data()+4, bytes.size()});
        assert(call() == InstallStatus::AmbiguousSymbol && hooks == 0);
    } else if (scenario == "duplicate_size") {
        rows.push_back({home_report::kSymbol.data(), bytes.data(), bytes.size()-4});
        assert(call() == InstallStatus::AmbiguousSymbol && hooks == 0);
    } else if (scenario == "duplicate_same") {
        rows.push_back(rows[0]);
        assert(call() == InstallStatus::Installed && hooks == 1);
    } else if (scenario == "failure_then_retry") {
        hook_result = 1;
        assert(call() == InstallStatus::HookFailed && hc_home_report_original == nullptr);
        hook_result = 0;
        assert(call() == InstallStatus::Installed && hooks == 2);
    } else if (scenario == "missing_original_then_retry") {
        publish_original = false;
        assert(call() == InstallStatus::MissingOriginalRolledBack && unhooks == 1);
        publish_original = true;
        assert(call() == InstallStatus::Installed && hooks == 2);
    } else if (scenario == "rollback_failure_terminal") {
        home_report::set_enabled(true);
        publish_original = false;
        unhook_result = 1;
        assert(call() == InstallStatus::RollbackFailed && unhooks == 1);
        assert(!home_report::enabled());
        assert(call() == InstallStatus::RollbackFailed && hooks == 1 && unhooks == 1);
    } else if (scenario == "idempotence_and_switch") {
        assert(call() == InstallStatus::Installed);
        assert(call() == InstallStatus::AlreadyInstalled && hooks == 1);
        home_report::set_enabled(true);
        assert(home_report::enabled());
        home_report::set_enabled(false);
        assert(!home_report::enabled() && hooks == 1 && unhooks == 0);
    } else if (scenario == "conflicting_image") {
        assert(call() == InstallStatus::Installed);
        rows[0].address = bytes.data()+4;
        assert(call() == InstallStatus::ConflictingTarget && hooks == 1);
    } else if (scenario == "concurrent_install") {
        std::array<InstallStatus, 16> results{};
        std::vector<std::thread> threads;
        for (size_t i=0;i<results.size();++i) threads.emplace_back([&,i] { results[i]=call(); });
        for (auto &thread : threads) thread.join();
        size_t installed=0;
        for (const auto result : results) {
            if (result==InstallStatus::Installed) ++installed;
            else assert(result==InstallStatus::AlreadyInstalled);
        }
        assert(installed==1 && hooks==1);
    } else {
        assert(false && "unknown scenario");
    }
    std::cout << "PASS " << scenario << '\n';
}
