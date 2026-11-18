/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "../home-recent-cards-sdk/fixture.h"
#include "targets/home/home_native_vibrator_contract.h"
#include <iostream>
#ifdef __ANDROID__
#include <android/binder_ibinder.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <dlfcn.h>
#include "targets/home/home_native_window.h"

static void verify_policy(const home_native_vibrator::Policy& policy) {
    using Check=AIBinder*(*)(const char*);
    auto check=reinterpret_cast<Check>(dlsym(RTLD_DEFAULT,"AServiceManager_checkService"));
    assert(check);
    home_native_platform::Binder power(check("power"),AIBinder_decStrong);
    assert(power);
    const auto* binding=AIBinder_Class_define("android.os.IPowerManager",
        [](void* data){return data;},[](void*){},
        [](AIBinder*,transaction_code_t,const AParcel*,AParcel*)->binder_status_t{return STATUS_UNKNOWN_TRANSACTION;});
    assert(binding&&AIBinder_associateClass(power.get(),binding));
    std::string reason;
    auto window=home_native_platform::window(reason);
    assert(window);
    const auto read=[&](AIBinder* target,uint32_t code) {
        auto reply=home_native_platform::call(target,code,[](AParcel*){return STATUS_OK;},reason);
        int32_t value=-1;
        assert(reply&&AParcel_readInt32(reply.get(),&value)==STATUS_OK&&value>=0&&value<=1);
        assert(AParcel_getDataPosition(reply.get())==AParcel_getDataSize(reply.get()));
        return value;
    };
    const auto interactive=read(power.get(),policy.interactive.transaction);
    const auto locked=read(window.get(),policy.locked.transaction);
    std::cout<<"PASS live Android policy interactive="<<interactive<<" locked="<<locked<<"\n";
}
static void verify_platform(uint32_t transaction) {
    using Check=AIBinder*(*)(const char*);
    auto check=reinterpret_cast<Check>(dlsym(RTLD_DEFAULT,"AServiceManager_checkService"));
    assert(check);
    std::unique_ptr<AIBinder,decltype(&AIBinder_decStrong)> service(check("vibrator_manager"),AIBinder_decStrong);
    assert(service);
    const auto* contract=AIBinder_Class_define("android.os.IVibratorManagerService",
        [](void* data){return data;},[](void*){},
        [](AIBinder*,transaction_code_t,const AParcel*,AParcel*)->binder_status_t{return STATUS_UNKNOWN_TRANSACTION;});
    assert(contract&&AIBinder_associateClass(service.get(),contract));
    AParcel* request=nullptr;AParcel* reply=nullptr;
    assert(AIBinder_prepareTransaction(service.get(),&request)==STATUS_OK&&request);
    assert(AIBinder_transact(service.get(),transaction,&request,&reply,0)==STATUS_OK&&reply);
    std::unique_ptr<AParcel,decltype(&AParcel_delete)> parcel(reply,AParcel_delete);
    AStatus* status=nullptr;
    assert(AParcel_readStatusHeader(reply,&status)==STATUS_OK&&status);
    std::unique_ptr<AStatus,decltype(&AStatus_delete)> header(status,AStatus_delete);
    assert(AStatus_isOk(status));
    std::vector<int32_t> ids;
    assert(AParcel_readInt32Array(reply,&ids,[](void* value,int32_t length,int32_t** buffer) {
        if(length<=0||length>16)return false;
        auto& ids=*static_cast<std::vector<int32_t>*>(value);
        ids.resize(length);*buffer=ids.data();return true;
    })==STATUS_OK);
    assert(AParcel_getDataPosition(reply)==AParcel_getDataSize(reply));
    std::cout<<"PASS live Android read-only vibrator count="<<ids.size()<<"\n";
}
#endif

int main(int argc,char** argv) {
    assert(argc==2);
    std::ifstream stream(argv[1],std::ios::binary);
    std::vector<char> input((std::istreambuf_iterator<char>(stream)),{});
    assert(!input.empty());
    Corpus files{Bytes(input.size())};
    std::memcpy(files[0].data(),input.data(),input.size());
    const auto sdk=graph(files);
    const auto contract=home_native_vibrator::resolve(sdk);
    assert(contract);
    const auto policy=home_native_vibrator::resolve_policy(sdk);
    assert(policy);
    const auto power=sdk.body(policy->interactive.proxy);
    assert(power);
    auto damaged=files;
    write(damaged,{power->address.image,power->address.va+policy->interactive.submit_at*4},0xd503201fu);
    assert(!home_native_vibrator::resolve_policy(graph(damaged)));
    const auto operation=contract->vibrate;
    const auto body=sdk.body(operation.proxy);
    assert(body);
    unsigned changed=0;
    for(size_t i=0;i<operation.submit_at;++i) {
        const auto word=body->words[i];
        if((word&0xffe0001fu)!=0x52800001u||((word>>5)&0xffffu)!=operation.transaction)continue;
        auto shifted=files;
        write(shifted,{operation.proxy.image,operation.proxy.va+i*4},word^(1u<<9));
        const auto next=home_native_vibrator::resolve(graph(shifted));
        assert(next&&next->vibrate.transaction==(operation.transaction^16u));
        ++changed;
    }
    assert(changed==1);
    auto broken=files;
    write(broken,{operation.proxy.image,operation.proxy.va+operation.submit_at*4},0xd503201fu);
    assert(!home_native_vibrator::resolve(graph(broken)));
#ifdef __ANDROID__
    verify_platform(contract->ids.transaction);
    verify_policy(*policy);
#endif
    std::cout<<"PASS runtime SDK vibrate and getVibratorIds dispatch, shifted transaction decoded, missing submit rejected, screen policy transactions and bool replies verified\n";
}
