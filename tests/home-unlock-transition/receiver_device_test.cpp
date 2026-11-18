/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../../app/src/main/cpp/targets/home/home_native_unlock_transition.cpp"
#include "../../app/src/main/cpp/targets/home/home_native_unlock_bundle.h"
#include <cstdio>
#include <vector>
#include <unistd.h>
namespace {std::atomic<unsigned> wake_count{0};void notified(){wake_count.fetch_add(1);}}
int main(int argc,char**) {
    if(argc==2) {
        for(int style=0;style<8;++style) {
            home_native_platform::Parcel parcel(AParcel_create(),AParcel_delete);
            if(!parcel||!home_native_glass::write_unlock(parcel.get(),INT64_C(2147483981),style))return 8;
            std::vector<uint8_t> bytes(AParcel_getDataSize(parcel.get()));
            if(AParcel_marshal(parcel.get(),bytes.data(),0,bytes.size())!=STATUS_OK)return 9;
            std::printf("BUNDLE%d ",style);for(auto byte:bytes)std::printf("%02x",unsigned(byte));std::puts("");
        }
        home_native_platform::Parcel parcel(AParcel_create(),AParcel_delete);
        if(home_native_glass::write_unlock(parcel.get(),0,0)||home_native_glass::write_unlock(parcel.get(),1,-1)
            ||home_native_glass::write_unlock(parcel.get(),1,8))return 10;
        return 0;
    }
    Binder target(AIBinder_new(listener_class(),new Cookie(notified)),AIBinder_decStrong);if(!target)return 1;
    const auto send=[&](uint32_t code,int64_t at,bool extra) {
        AParcel* input=nullptr;AParcel* output=nullptr;auto status=AIBinder_prepareTransaction(target.get(),&input);
        if(status==STATUS_OK&&code==FIRST_CALL_TRANSACTION) {
            status=AParcel_writeInt64(input,at);
            if(status==STATUS_OK)status=AParcel_writeInt32(input,18);
        }
        if(status==STATUS_OK&&extra)status=AParcel_writeInt32(input,42);
        if(status==STATUS_OK){status=AIBinder_transact(target.get(),code,&input,&output,FLAG_ONEWAY);input=nullptr;}
        if(input)AParcel_delete(input);if(output)AParcel_delete(output);return status;
    };
    const auto at=now();
    if(getuid()!=1000) {
        if(send(FIRST_CALL_TRANSACTION,at,false)!=STATUS_PERMISSION_DENIED||hc_native_unlock_observation().accepted
            ||send(FIRST_CALL_TRANSACTION+1,0,false)!=STATUS_PERMISSION_DENIED||hc_native_home_window_generation()!=0
            ||send(FIRST_CALL_TRANSACTION+3,0,false)!=STATUS_PERMISSION_DENIED)return 2;
        std::puts("PASS actual early-unlock receiver rejects non-system caller");return 0;
    }
    if(send(FIRST_CALL_TRANSACTION,at,false)!=STATUS_OK)return 3;
    if(send(FIRST_CALL_TRANSACTION,at,false)!=STATUS_OK||send(FIRST_CALL_TRANSACTION,at-1,false)!=STATUS_OK
        ||send(FIRST_CALL_TRANSACTION,at-5000,false)!=STATUS_OK||send(FIRST_CALL_TRANSACTION,at+5000,false)!=STATUS_OK)return 4;
    const auto snapshot=hc_native_unlock_observation();
    if(snapshot.epoch_ms!=at||snapshot.flags!=18||snapshot.accepted!=1||snapshot.received!=5||wake_count.load()!=1)return 5;
    if(send(FIRST_CALL_TRANSACTION,at,true)!=STATUS_BAD_VALUE||send(FIRST_CALL_TRANSACTION+4,at,false)!=STATUS_UNKNOWN_TRANSACTION)return 6;
    const auto window=[&](bool known,bool visible,bool extra) {
        AParcel* input=nullptr;AParcel* output=nullptr;auto status=AIBinder_prepareTransaction(target.get(),&input);
        if(status==STATUS_OK)status=AParcel_writeBool(input,known);
        if(status==STATUS_OK)status=AParcel_writeBool(input,visible);
        if(status==STATUS_OK&&extra)status=AParcel_writeInt32(input,5);
        if(status==STATUS_OK){status=AIBinder_transact(target.get(),FIRST_CALL_TRANSACTION+2,&input,&output,FLAG_ONEWAY);input=nullptr;}
        if(input)AParcel_delete(input);if(output)AParcel_delete(output);return status;
    };
    const auto ownership=[&](int64_t claim,bool owned,bool extra) {
        AParcel* input=nullptr;AParcel* output=nullptr;auto status=AIBinder_prepareTransaction(target.get(),&input);
        if(status==STATUS_OK)status=AParcel_writeInt64(input,claim);
        if(status==STATUS_OK)status=AParcel_writeBool(input,owned);
        if(status==STATUS_OK&&extra)status=AParcel_writeInt32(input,5);
        if(status==STATUS_OK){status=AIBinder_transact(target.get(),FIRST_CALL_TRANSACTION+3,&input,&output,FLAG_ONEWAY);input=nullptr;}
        if(input)AParcel_delete(input);if(output)AParcel_delete(output);return status;
    };
    dock_epoch.store(2);
    if(ownership(2,true,true)!=STATUS_BAD_VALUE||dock_owned.load())return 14;
    if(ownership(2,true,false)!=STATUS_OK||!dock_owned.load())return 15;
    if(ownership(1,false,false)!=STATUS_OK||!dock_owned.load())return 17;
    if(ownership(0,false,false)!=STATUS_BAD_VALUE||!dock_owned.load())return 18;
    if(ownership(2,false,false)!=STATUS_OK||dock_owned.load())return 16;
    if(window(false,true,false)!=STATUS_BAD_VALUE||window(true,true,true)!=STATUS_BAD_VALUE)return 11;
    if(window(true,true,false)!=STATUS_OK||visibility.load()!=3||window(true,false,false)!=STATUS_OK||visibility.load()!=1)return 12;
    if(send(FIRST_CALL_TRANSACTION+1,0,true)!=STATUS_BAD_VALUE||hc_native_home_window_generation()!=0
        ||send(FIRST_CALL_TRANSACTION+1,0,false)!=STATUS_OK||hc_native_home_window_generation()!=1||wake_count.load()!=6||visibility.load()!=0)return 7;
    static_cast<Cookie*>(AIBinder_getUserData(target.get()))->active.store(false);
    if(send(FIRST_CALL_TRANSACTION+1,0,false)!=STATUS_OK||hc_native_home_window_generation()!=1
        ||ownership(2,true,false)!=STATUS_OK||dock_owned.load()||wake_count.load()!=6)return 13;
    std::puts("PASS actual NDK early-unlock receiver, epoch/flags and retirement, fresh/replay/late/future checks and malformed transaction refusal");
}
