/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <android/binder_parcel.h>
#include <string>
#include <string_view>

namespace home_native_parcelable {
// Bundle's AnyParcelable payload retains Java's class descriptor. The SDK
// typed read_from_parcel consumes the body, after that descriptor. This reads
// the platform wire format; it never skips a guessed number of bytes.
inline bool read_payload(AParcel* parcel,std::string_view expected) {
    if(AParcel_setDataPosition(parcel,0)!=STATUS_OK)return false;
    std::string name(expected.size()+1,'\0');
    const auto result=AParcel_readString(parcel,&name,[](void* text,int32_t length,char** buffer) {
        auto& output=*static_cast<std::string*>(text);
        if(length<=0||static_cast<size_t>(length)!=output.size())return false;
        *buffer=output.data();return true;
    });
    return result==STATUS_OK&&std::string_view(name.data(),expected.size())==expected&&name.back()=='\0';
}
}
