/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_native_input_pilfer.h"
#include "home_recent_cards_live_image.h"
#include <android/binder_status.h>
namespace home_native_input {
struct PilferResult {bool accepted=false;int32_t exception=0,status=0;};
// Borrowed from the still-owned SDK monitor. The caller must serialize close
// before monitor destruction. No SDK/Dart object is cloned or freed here.
class CheckedPilfer {
    using Function=AStatus*(*)(void*);
    void* host_;
    Function function_;
    CheckedPilfer(void* host,Function function):host_(host),function_(function){}
public:
    static std::optional<CheckedPilfer> bind(const sdk::Graph& graph,std::span<const sdk::LiveImage> images,
        const PilferContract& contract,void* monitor) {
        const auto pointer=reinterpret_cast<uintptr_t>(monitor);uintptr_t host=0,table=0,target=0;
        if(!pointer||pointer>UINTPTR_MAX-std::max(contract.data,contract.table)
            ||!sdk::read_live(pointer+contract.data,std::as_writable_bytes(std::span(&host,1)))
            ||!sdk::read_live(pointer+contract.table,std::as_writable_bytes(std::span(&table,1)))||!host||!table
            ||table>UINTPTR_MAX-contract.method||!sdk::read_live(table+contract.method,std::as_writable_bytes(std::span(&target,1))))return {};
        for(size_t i=0;i<images.size();++i)if(target>=images[i].base
            &&graph.metadata(i)->contains(target-images[i].base,4,nhk::elf::kFlagExecute))
            return CheckedPilfer(reinterpret_cast<void*>(host),reinterpret_cast<Function>(target));
        return {};
    }
    PilferResult claim()const {
        // Every non-null path in the actual SDK consumes this same status
        // pointer through AStatus_delete. A void return is never evidence.
        std::unique_ptr<AStatus,decltype(&AStatus_delete)> result(function_(host_),AStatus_delete);
        return result?PilferResult{false,AStatus_getExceptionCode(result.get()),AStatus_getStatus(result.get())}:PilferResult{true,0,0};
    }
};
}
