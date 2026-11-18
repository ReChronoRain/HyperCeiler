/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <algorithm>
#include <array>
#include <bitset>
#include <functional>
#include <memory>
#include <optional>
#include <span>
#include <string>
#include <string_view>
#include <vector>

namespace home_layout::cards {
constexpr size_t kMaxPackages=1024, kMaxPackageBytes=255, kMaxGroups=4096, kMaxMembers=128;

inline bool valid_package(std::string_view name) {
    if(name.empty()||name.size()>kMaxPackageBytes)return false;
    bool first=true;
    for(unsigned char c:name) {
        if(c=='.'){if(first)return false;first=true;continue;}
        const bool letter=(c>='A'&&c<='Z')||(c>='a'&&c<='z')||c=='_';
        if(!letter&&(first||c<'0'||c>'9'))return false;
        first=false;
    }
    return !first;
}

// A validated, sorted selection; shared identity is also the publication token.
// Null means unavailable, a non-null empty selection means explicitly disabled.
struct Selection {
    std::vector<std::string> packages;
    bool contains(std::string_view name) const {
        return std::binary_search(packages.begin(),packages.end(),name);
    }
};
using Snapshot=std::shared_ptr<const Selection>;

struct TaskPackage {
    std::optional<std::string_view> real_activity, intent_component, intent_package;
    bool hidden(const Selection& selected) const {
        const auto name=real_activity?real_activity:intent_component?intent_component:intent_package;
        return name&&selected.contains(*name);
    }
};

struct Plan {
    Snapshot selection;
    std::bitset<kMaxGroups> remove;
    size_t count=0, removed=0;
};

// Access must visit every member, including mixed-group children, using only
// borrowed SDK references whose layout has been proved by the runtime resolver.
// It returns false on an unknown/malformed group. Nothing is changed or freed
// until ALL groups pass, even if an earlier member already matches.
template<class Access>
bool prepare(std::span<void* const> groups,Snapshot selected,Access access,Plan& out) {
    if(!selected||groups.size()>kMaxGroups)return false;
    Plan next;next.selection=std::move(selected);next.count=groups.size();
    if(next.selection->packages.empty()){out=next;return true;}
    std::array<void*,kMaxGroups> owned;size_t owned_count=0;
    for(const auto group:groups)if(group)owned[owned_count++]=group;
    std::sort(owned.begin(),owned.begin()+owned_count,std::less<void*>{});
    if(std::adjacent_find(owned.begin(),owned.begin()+owned_count)!=owned.begin()+owned_count)return false;
    for(size_t i=0;i<groups.size();++i) {
        if(!groups[i])continue;
        size_t members=0;bool hidden=false;
        const bool valid=access(groups[i],[&](const TaskPackage& task) {
            if(++members>kMaxMembers)return false;
            hidden|=task.hidden(*next.selection);return true;
        });
        if(!valid||members>kMaxMembers)return false;
        if(hidden){next.remove.set(i);++next.removed;}
    }
    out=next;return true;
}

// Caller holds the publication lock and checks the captured Snapshot identity
// before committing. The SDK RVec retains its capacity and destructor/vtable;
// only its live pointer range is compacted and its length subsequently changed.
// drop destroys exactly one owned SDK box, never a borrowed TaskInfo reference.
template<class Drop>
std::optional<size_t> commit(std::span<void*> groups,const Plan& plan,const Snapshot& current,Drop drop) {
    if(!current||current!=plan.selection||groups.size()!=plan.count)return {};
    size_t kept=0;
    for(size_t i=0;i<plan.count;++i) {
        if(plan.remove[i])drop(groups[i]);
        else groups[kept++]=groups[i];
    }
    return kept;
}
}
