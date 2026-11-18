/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <android/binder_parcel.h>
#include <array>
#include <algorithm>
#include <bit>
#include <cstdint>
#include <string_view>
namespace home_native_glass {
// Our existing DockGlassHost request, encoded using Android Bundle's typed
// Parcel wire. No SDK insertion ABI or object layout is fabricated here.
inline bool write_unlock(AParcel* parcel,int64_t epoch,int32_t style) {
    constexpr std::array<std::string_view,8> names={"daybreak","depth_flip","gale","orbit_sweep",
        "ripple","perspective_fold","capsule_fission","auto_aim"};
    if(!parcel||epoch<=0||style<0||size_t(style)>=names.size())return false;
    struct Field {std::string_view key;int32_t kind;};
    std::array<Field,2> fields={{{"startedAtMs",6},{"style",0}}};
    const auto hash=[](std::string_view value){uint32_t h=0;for(unsigned char c:value)h=31*h+c;return std::bit_cast<int32_t>(h);};
    std::sort(fields.begin(),fields.end(),[&](const auto& a,const auto& b){return hash(a.key)<hash(b.key);});
    bool ok=AParcel_writeInt32(parcel,-1)==STATUS_OK&&AParcel_writeInt32(parcel,0x4c444e42)==STATUS_OK;
    const auto start=AParcel_getDataPosition(parcel);ok=ok&&AParcel_writeInt32(parcel,fields.size())==STATUS_OK;
    for(const auto& field:fields) {
        ok=ok&&AParcel_writeString(parcel,field.key.data(),field.key.size())==STATUS_OK&&AParcel_writeInt32(parcel,field.kind)==STATUS_OK;
        if(field.kind==6)ok=ok&&AParcel_writeInt64(parcel,epoch)==STATUS_OK;
        else ok=ok&&AParcel_writeString(parcel,names[style].data(),names[style].size())==STATUS_OK;
    }
    const auto end=AParcel_getDataPosition(parcel);
    return ok&&AParcel_setDataPosition(parcel,0)==STATUS_OK&&AParcel_writeInt32(parcel,end-start)==STATUS_OK
        &&AParcel_setDataPosition(parcel,end)==STATUS_OK&&AParcel_writeBool(parcel,false)==STATUS_OK
        &&AParcel_setDataPosition(parcel,0)==STATUS_OK;
}
}
