/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_recent_cards_query_contract.h"
#include "home_recent_cards_group_access.h"

namespace home_layout::cards::sdk {
struct Reply {std::array<uint64_t,4> words;};
static_assert(sizeof(Reply)==QueryContract::kBytes&&alignof(Reply)==alignof(uint64_t));

template<class Read,class Commit>
std::optional<size_t> filter_reply(Reply& reply,const QueryContract& query,const GroupContract& group,
    const TaskContract& task,uintptr_t table,Snapshot selection,Read read,Commit commit,void(*drop)(void*)) {
    const auto word=[&](uint32_t offset){uint64_t value=0;std::memcpy(&value,reinterpret_cast<const std::byte*>(&reply)+offset,8);return value;};
    const auto pointer=word(query.pointer),length=word(query.length),capacity=word(query.capacity);
    if(!selection||word(query.vtable)!=table||length>kMaxGroups||length>capacity
        ||capacity>static_cast<uint64_t>(PTRDIFF_MAX)/sizeof(void*)
        ||(length&&(!pointer||pointer%alignof(void*)||pointer>UINTPTR_MAX-length*sizeof(void*))))return {};
    if(!length||selection->packages.empty())return 0;
    std::array<void*,kMaxGroups> captured;
    auto values=std::span(captured).first(static_cast<size_t>(length));
    if(!read(static_cast<uintptr_t>(pointer),std::as_writable_bytes(values)))return {};
    Plan plan;
    if(!prepare(values,std::move(selection),[&](void* value,auto visit){
        return visit_group(group,task,reinterpret_cast<uintptr_t>(value),read,visit);
    },plan))return {};
    if(!plan.removed)return 0;
    // This is the original SDK-owned writable pointer buffer. No SDK or Dart
    // reference escaped preparation, and nothing can observe the returned RVec
    // until this synchronous proxy returns. Preserve its capacity/table verbatim.
    auto owned=std::span(reinterpret_cast<void**>(pointer),static_cast<size_t>(length));size_t kept=0;
    if(!commit(owned,plan,drop,kept))return {};
    const uint64_t result=kept;
    std::memcpy(reinterpret_cast<std::byte*>(&reply)+query.length,&result,sizeof(result));
    return plan.removed;
}
}
