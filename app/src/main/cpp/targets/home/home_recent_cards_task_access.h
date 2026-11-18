/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recent_cards_filter.h"
#include "home_recent_cards_task_contract.h"

namespace home_layout::cards::sdk {
// Synchronous borrowed TaskInfo access. The caller owns the group reply for
// this entire invocation; no SDK object or string address survives the visitor.
// Read must copy the entire span or return false. A live callback adapter must
// not fall back to procfs/file/Provider queries; establish that capability on
// the setup worker before using the contract.
// The contract comes only from resolve_task for the currently loaded images.
template<class Read,class Visit>
bool visit_task(const TaskContract& contract,uintptr_t task,Read read,Visit visit) {
    const auto word=[&](uintptr_t base,uint32_t offset,uint64_t& out) {
        if(base>UINTPTR_MAX-offset||base+offset>UINTPTR_MAX-sizeof(out))return false;
        return read(base+offset,std::as_writable_bytes(std::span(&out,1)));
    };
    struct Object {bool valid=false,present=false;uintptr_t address=0;};
    const auto optional=[&](uintptr_t base,const OptionalField& field) {
        uint64_t tag=0;
        if(!word(base,field.offset,tag))return Object{};
        return Object{true,tag!=field.none,base+field.offset};
    };
    const auto deliver=[&](uintptr_t object,const StringFields& fields,unsigned origin) {
        uint64_t pointer=0,length=0;
        if(!word(object,fields.pointer,pointer)||!word(object,fields.length,length))return false;
        std::array<char,kMaxPackageBytes> bytes{};
        std::string_view name;
        // Every valid selection is ASCII and at most 255 bytes. A larger name
        // cannot match, but remains present so lower-priority fields stay ignored.
        if(length<=bytes.size()) {
            if(length&&(pointer>UINTPTR_MAX-length
                ||!read(static_cast<uintptr_t>(pointer),std::as_writable_bytes(std::span(bytes).first(length)))))return false;
            name={bytes.data(),static_cast<size_t>(length)};
        }
        TaskPackage result;
        if(origin==0)result.real_activity=name;
        if(origin==1)result.intent_component=name;
        if(origin==2)result.intent_package=name;
        return visit(result);
    };
    const auto real=optional(task,contract.activities[3]);
    if(!real.valid)return false;
    if(real.present)return deliver(real.address,contract.component_string,0);
    const auto intent=optional(task,contract.base_intent);
    if(!intent.valid)return false;
    if(!intent.present)return visit(TaskPackage{});
    const auto component=optional(intent.address,contract.intent_component);
    if(!component.valid)return false;
    if(component.present)return deliver(component.address,contract.component_string,1);
    const auto package=optional(intent.address,contract.intent_package);
    if(!package.valid)return false;
    if(package.present)return deliver(package.address,contract.package_string,2);
    return visit(TaskPackage{});
}
}
