/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recent_cards_group_contract.h"
#include "home_recent_cards_task_access.h"

namespace home_layout::cards::sdk {
// Own only the query's root boxes. Child groups and inline TaskInfos remain
// borrowed for this synchronous visit. MIXED matches Android's one-level
// mGroupedTasks.flatMap(child.mTasks), including its null-list refusal.
template<class Read,class Visit>
bool visit_group(const GroupContract& group,const TaskContract& task,uintptr_t address,Read read,Visit visit) {
    const auto word=[&](uintptr_t base,uint32_t offset,auto& value) {
        if(base>UINTPTR_MAX-offset||base+offset>UINTPTR_MAX-sizeof(value))return false;
        return read(base+offset,std::as_writable_bytes(std::span(&value,1)));
    };
    struct Vector {bool valid=false,absent=false;uintptr_t pointer=0;size_t length=0;};
    const auto vector=[&](uintptr_t base,const VectorFields& fields) {
        uint64_t capacity=0,pointer=0,length=0;
        if(!word(base,fields.capacity,capacity))return Vector{};
        if(capacity==fields.none)return Vector{true,true};
        if(!word(base,fields.pointer,pointer)||!word(base,fields.length,length)
            ||length>kMaxMembers||length>capacity||capacity>static_cast<uint64_t>(PTRDIFF_MAX)/fields.stride
            ||(length&&(!pointer||pointer%group.alignment||pointer>UINTPTR_MAX-length*fields.stride)))return Vector{};
        return Vector{true,false,static_cast<uintptr_t>(pointer),static_cast<size_t>(length)};
    };
    size_t members=0;
    const auto tasks=[&](uintptr_t base) {
        const auto items=vector(base,group.tasks);if(!items.valid)return false;
        if(items.absent)return true;
        if(items.length>kMaxMembers-members)return false;
        for(size_t i=0;i<items.length;++i) {
            ++members;
            if(!visit_task(task,items.pointer+i*group.tasks.stride,read,visit))return false;
        }
        return true;
    };
    int32_t type=0;if(!word(address,group.type,type))return false;
    if(type!=group.mixed_type)return tasks(address);
    const auto children=vector(address,group.children);
    if(!children.valid||children.absent)return false;
    for(size_t i=0;i<children.length;++i) {
        const uintptr_t child=children.pointer+i*group.children.stride;
        uint64_t tag=0;if(!word(child,group.null_child.offset,tag)||tag==group.null_child.none||!tasks(child))return false;
    }
    return true;
}
}
