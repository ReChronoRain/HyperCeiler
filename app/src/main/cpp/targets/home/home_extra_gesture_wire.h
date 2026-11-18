/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include "home_extra_gesture_state.h"
#include <string_view>
#include <utility>
namespace home_extra_gestures {
inline constexpr std::string_view descriptor="com.sevtinge.hyperceiler.home.IExtraGestureSnapshot";
inline constexpr int32_t snapshot_ack=0x48474531,snapshot_version=1,snapshot_query=1;
enum class SnapshotStatus {unavailable=0,ready=1,closed=2};
struct Snapshot {
    int64_t producer=0,revision=0,published=0;
    int32_t mask=0;
    float slop=0;
    int32_t double_timeout=0,long_timeout=0;
    std::optional<Geometry> geometry;
};
inline constexpr int32_t control_query=2,control_ack=0x4847454c,control_version=2;
enum class ControlOp:int32_t {take=1,renew=2,release=3};
enum class ControlStatus:int32_t {denied=0,owned=1,released=2};
struct Grant {int64_t producer=0,revision=0,serial=0,until=0;};
struct ControlReply {ControlStatus status;Grant grant;};
template<class Read,class End> std::optional<ControlReply> read_control(Read read,End end,int64_t now) {
    int32_t ack=0,version=0,status=-1;Grant value;
    if(!read(ack)||!read(version)||!read(status)||ack!=control_ack||version!=control_version||status<0||status>2
        ||!read(value.producer)||!read(value.revision)||!read(value.serial)||!read(value.until)||!end())return {};
    if(status==1) {
        if(value.producer<=0||value.revision<=0||value.serial<=0||value.until<=now||value.until-now>2000)return {};
    } else if(value.producer||value.revision||value.serial||value.until)return {};
    return ControlReply{ControlStatus(status),value};
}
struct SnapshotReply {SnapshotStatus status;std::optional<Snapshot> value;};
// One frame carries configuration, timings and the real owner geometry.
// Unknown flags, stale publications and trailing fields never reuse a partial
// or previous snapshot. This is our own protocol, not a vendor memory layout.
template<class Read,class End> std::optional<SnapshotReply> read_snapshot(Read read,End end,int64_t now) {
    int32_t ack=0,version=0,status=-1;
    if(!read(ack)||!read(version)||!read(status)||ack!=snapshot_ack||version!=snapshot_version
        ||status<0||status>2)return {};
    if(status!=1)return end()?std::optional<SnapshotReply>{{SnapshotStatus(status),{}}}:std::nullopt;
    Snapshot snapshot;int32_t present=-1;
    if(!read(snapshot.producer)||!read(snapshot.revision)||!read(snapshot.published)||!read(snapshot.mask)
        ||!read(snapshot.slop)||!read(snapshot.double_timeout)||!read(snapshot.long_timeout)||!read(present)
        ||snapshot.producer<=0||snapshot.revision<=0||snapshot.published<0||snapshot.published>now
        ||now-snapshot.published>1500||snapshot.mask<0||snapshot.mask>31
        ||!std::isfinite(snapshot.slop)||snapshot.slop<=0||snapshot.double_timeout<=0||snapshot.long_timeout<=0
        ||present<0||present>1)return {};
    if(present) {
        Geometry geometry;int64_t owner=0;int32_t handle=-1;
        if(!read(owner)||!read(geometry.width)||!read(geometry.height)||!read(geometry.rotation)
            ||!read(geometry.density)||!read(geometry.nav_top)||!read(geometry.corner)||!read(handle)
            ||owner<=0||geometry.width>32768||geometry.height>32768||geometry.rotation<0||geometry.rotation>3
            ||handle<0||handle>1)return {};
        geometry.owner=uint64_t(owner);if(!geometry.valid())return {};
        if(handle) {
            Region region;
            if(!read(region.left)||!read(region.top)||!read(region.right)||!read(region.bottom)||!region.valid())return {};
            geometry.handle=region;
        }
        snapshot.geometry=geometry;
    }
    if(!end())return {};
    return SnapshotReply{SnapshotStatus::ready,std::move(snapshot)};
}
}
