/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../../app/src/main/cpp/targets/home/home_native_window_observer.cpp"
#include <android/binder_parcel.h>
#include <cstdio>
#include <memory>
namespace {
unsigned called=0,destroyed=0;int cookie=42;
void* created(void* value){return value;}
void retired(void* value){if(value==&cookie)++destroyed;}
binder_status_t receive(AIBinder* binder,transaction_code_t code,const AParcel* input,AParcel* output) {
    if(AIBinder_getUserData(binder)!=&cookie||code!=FIRST_CALL_TRANSACTION)return STATUS_UNKNOWN_TRANSACTION;
    ++called;int32_t value=0;const auto status=AParcel_readInt32(input,&value);
    if(status!=STATUS_OK)return status;
    if(value!=42||AParcel_getDataPosition(input)!=AParcel_getDataSize(input))return STATUS_BAD_VALUE;
    return AParcel_writeInt32(output,73);
}
binder_status_t other(AIBinder*,transaction_code_t,const AParcel*,AParcel*){return STATUS_BAD_VALUE;}
}
int main() {
    original.store(reinterpret_cast<Define>(dlsym(RTLD_DEFAULT,"AIBinder_Class_define")),std::memory_order_release);
    if(!original.load())return 1;watching.store(true);
    auto* type=define("android.view.IWindow",created,retired,receive);if(!type)return 2;
    using Binder=std::unique_ptr<AIBinder,decltype(&AIBinder_decStrong)>;
    using Parcel=std::unique_ptr<AParcel,decltype(&AParcel_delete)>;
    Binder first(AIBinder_new(type,&cookie),AIBinder_decStrong),second(AIBinder_new(type,&cookie),AIBinder_decStrong);
    const auto send=[&](AIBinder* target,int value)->binder_status_t {
        AParcel* raw=nullptr,*reply=nullptr;auto status=AIBinder_prepareTransaction(target,&raw);
        if(status==STATUS_OK)status=AParcel_writeInt32(raw,value);
        if(status==STATUS_OK){status=AIBinder_transact(target,FIRST_CALL_TRANSACTION,&raw,&reply,0);raw=nullptr;}
        Parcel input(raw,AParcel_delete),output(reply,AParcel_delete);
        if(status==STATUS_OK){int32_t answer=0;if(AParcel_readInt32(output.get(),&answer)!=STATUS_OK||answer!=73)return STATUS_BAD_VALUE;}
        return status;
    };
    if(!first||!second||send(first.get(),42)!=STATUS_OK||called!=1||callbacks.load()!=1||definitions.load()!=1)return 3;
    if(send(first.get(),11)!=STATUS_BAD_VALUE||called!=2||callbacks.load()!=2||ambiguous.load())return 4;
    if(send(second.get(),42)!=STATUS_OK||!ambiguous.load()||callbacks.load()!=3)return 5;
    watching.store(false);if(send(first.get(),42)!=STATUS_OK||called!=4||callbacks.load()!=3)return 6;
    auto* untouched=define("android.test.untouched",created,retired,receive);
    Binder third(AIBinder_new(untouched,&cookie),AIBinder_decStrong);
    if(!third||send(third.get(),42)!=STATUS_OK||definitions.load()!=1)return 7;
    auto* incompatible=define("android.view.IWindow",created,retired,other);
    Binder fourth(AIBinder_new(incompatible,&cookie),AIBinder_decStrong);
    if(!fourth||send(fourth.get(),42)!=STATUS_BAD_VALUE||definitions.load()!=1)return 8;
    first.reset();second.reset();third.reset();fourth.reset();
    if(destroyed!=4)return 9;
    std::puts("PASS actual NDK class factory interception preserves original callback, Parcel cursor/result, user data, constructors/destructors, disabled pass-through and ambiguous-window refusal diagnostics");
}
