/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
// Deliberately a routing adapter, NOT production weather_card_runtime.cpp.
// Configurable results stand in for template allocation, guards and code writes.
// Tests inspect actual controller-supplied callbacks; no Dart code is installed.
#include "targets/weather/weather_card_runtime.h"
#include <atomic>
#include <functional>
namespace card_fixture {
using Phase = weather_card_runtime::Phase;
using Error = weather_card_runtime::Error;
std::atomic<Phase> phase{Phase::Cold};
std::atomic<int> prepares{0}, installs{0}, services{0}, cancels{0};
bool prepare_ok = true;
Phase installation_result = Phase::Installed;
weather_card_runtime::ReadFresh read = nullptr;
uintptr_t seen_dart = 0, seen_flutter = 0;
weather_snapshot::Snapshot at_install{}, at_service{};
bool install_read = false, service_read = false;
std::function<void()> prepare_event, install_event;
}
namespace test_card_runtime {
using Phase = weather_card_runtime::Phase;
using Status = weather_card_runtime::Status;
bool prepare(weather_card_runtime::ReadFresh read) noexcept {
    ++card_fixture::prepares;
    card_fixture::read = read;
    if (card_fixture::prepare_event) card_fixture::prepare_event();
    if (!card_fixture::prepare_ok) return false;
    auto cold = Phase::Cold;
    return card_fixture::phase.compare_exchange_strong(cold, Phase::Prepared);
}
bool install_before_snapshot(uintptr_t dart, uintptr_t flutter) noexcept {
    ++card_fixture::installs;
    card_fixture::seen_dart = dart;
    card_fixture::seen_flutter = flutter;
    card_fixture::install_read = card_fixture::read
        && card_fixture::read(card_fixture::at_install);
    if (card_fixture::install_event) card_fixture::install_event();
    auto prepared = Phase::Prepared;
    return card_fixture::phase.compare_exchange_strong(prepared, card_fixture::installation_result)
        && card_fixture::installation_result == Phase::Installed;
}
void service() noexcept {
    // This only records freshness; lease publication/expiry is NOT simulated.
    card_fixture::service_read = card_fixture::read
        && card_fixture::read(card_fixture::at_service);
    ++card_fixture::services;
}
void cancel() noexcept {
    ++card_fixture::cancels;
    if (card_fixture::phase.load() != Phase::Residue) card_fixture::phase = Phase::Missed;
}
Status status() noexcept {
    return {card_fixture::phase.load(), weather_card_runtime::Error::None, 0, 0};
}
}
