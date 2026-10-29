/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
namespace home_layout {
constexpr int32_t kEndpointDiagnosticsMagic=0x48434431;
constexpr int32_t kEndpointDiagnosticsSchema=1;
constexpr int32_t kEndpointRevision=20261028;
constexpr int32_t kIndependentScalesCapability=1;
constexpr int32_t kDockTitleCapability=2;
constexpr int32_t kRecentLayoutCapability=4;
constexpr int32_t kDrawerSearchCapability=8;
constexpr int32_t kLayoutPoliciesCapability=16;
constexpr int32_t kDockHorizontalCapability=32;
constexpr int32_t kInfiniteScrollCapability=64;
struct EndpointDiagnostics {int32_t schema=0,revision=0,capabilities=0,pid=0;};
enum class EndpointDiagnosticState { Missing, Valid, Malformed, Unsupported };
// A trailing diagnostic block cannot invalidate a functional snapshot. Old clients
// ignore it; new clients report a missing/unknown version without guessing disabled preferences.
template<class Read> EndpointDiagnosticState read_endpoint_diagnostics(Read read,EndpointDiagnostics& out){
    out={};int32_t magic=0;
    if(!read(magic))return EndpointDiagnosticState::Missing;
    if(magic!=kEndpointDiagnosticsMagic||!read(out.schema))return EndpointDiagnosticState::Malformed;
    if(out.schema!=kEndpointDiagnosticsSchema)return EndpointDiagnosticState::Unsupported;
    if(!read(out.revision)||!read(out.capabilities)||!read(out.pid)
        ||out.revision<1||out.capabilities<0||out.pid<1)return EndpointDiagnosticState::Malformed;
    return EndpointDiagnosticState::Valid;
}
}
