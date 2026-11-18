/* SPDX-License-Identifier: AGPL-3.0-or-later */
// Actual production NDK Binder class and receiver, not a mocked WMS. Run an
// independent UID1000 process; no system listener registration or screen input.
#include "../../app/src/main/cpp/targets/home/home_native_keyguard.cpp"
#include <cstdio>
#include <unistd.h>
int main(int argc,char** argv) {
    if(argc!=2)return 1;const auto number=std::stoul(argv[1]);
    if(number<FIRST_CALL_TRANSACTION||number>=LAST_CALL_TRANSACTION)return 2;
    const uint32_t code=uint32_t(number);
    Binder target(AIBinder_new(listener_class(),new Cookie(code,nullptr)),AIBinder_decStrong);if(!target)return 3;
    const auto send=[&](uint32_t transaction,bool locked,bool extra,binder_flags_t flags) {
        AParcel* input=nullptr;AParcel* output=nullptr;auto status=AIBinder_prepareTransaction(target.get(),&input);
        if(status!=STATUS_OK)return status;
        status=AParcel_writeBool(input,locked);
        if(status==STATUS_OK&&extra)status=AParcel_writeInt32(input,42);
        if(status==STATUS_OK){status=AIBinder_transact(target.get(),transaction,&input,&output,flags);input=nullptr;}
        if(input)AParcel_delete(input);
        if(status==STATUS_OK&&!(flags&FLAG_ONEWAY)) {
            AStatus* reply=nullptr;status=AParcel_readStatusHeader(output,&reply);
            if(status==STATUS_OK&&(!reply||!AStatus_isOk(reply)))status=STATUS_BAD_VALUE;
            if(reply)AStatus_delete(reply);
        }
        if(output)AParcel_delete(output);return status;
    };
    if(getuid()!=1000) {
        if(send(code,true,false,0)!=STATUS_PERMISSION_DENIED||hc_native_keyguard_observation().callbacks)return 4;
        std::puts("PASS actual keyguard Binder receiver refuses non-system UID");return 0;
    }
    for(auto flags:{binder_flags_t(0),binder_flags_t(FLAG_ONEWAY)}) {
        if(send(code,true,false,flags)!=STATUS_OK||send(code,false,false,flags)!=STATUS_OK)return 5;
        const auto value=hc_native_keyguard_observation();
        if(!value.known||value.locked||!value.unlock_ns)return 6;
    }
    const auto count=hc_native_keyguard_observation().callbacks;
    if(send(code,false,true,0)!=STATUS_BAD_VALUE||send(code+1,false,false,0)!=STATUS_UNKNOWN_TRANSACTION)return 7;
    if(hc_native_keyguard_observation().callbacks!=count)return 8;
    std::puts("PASS actual NDK keyguard receiver, synchronous/one-way lock-unlock, status header, monotonic epoch, excess payload and wrong transaction refusal");
}
