/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
#include <cstddef>
namespace nhk {
enum class DiagnosticCode : uint32_t { installed,rearmed,validation_failed,install_failed,continuation_invalid,
    protection_failed,hook_lost,foreign_modification,generation_changed,resolver_failed,ambiguous_target,waiting };
struct DiagnosticEvent { DiagnosticCode code;uintptr_t address;uint64_t inode;uint64_t file_offset;const char* detail;const char* hook_id=nullptr; };
using DiagnosticSink=void(*)(const DiagnosticEvent&);
inline const char* diagnostic_code(DiagnosticCode c){switch(c){
    case DiagnosticCode::installed:return "INSTALL_OK";
    case DiagnosticCode::rearmed:return "REARMED";
    case DiagnosticCode::validation_failed:return "VALIDATION_FAILED";
    case DiagnosticCode::install_failed:return "INSTALL_FAILED";
    case DiagnosticCode::continuation_invalid:return "CONTINUATION_INVALID";
    case DiagnosticCode::protection_failed:return "MEMORY_PROTECTION_FAILED";
    case DiagnosticCode::hook_lost:return "HOOK_LOST";
    case DiagnosticCode::foreign_modification:return "FOREIGN_MODIFICATION";
    case DiagnosticCode::generation_changed:return "GENERATION_CHANGED";
    case DiagnosticCode::resolver_failed:return "RESOLVE_FAILED";
    case DiagnosticCode::ambiguous_target:return "AMBIGUOUS_TARGET";
    default:return "UNKNOWN";
}}
}
