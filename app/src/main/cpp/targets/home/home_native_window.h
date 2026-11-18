/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <android/binder_ibinder.h>
#include <android/binder_parcel.h>
#include <android/binder_status.h>
#include <cstring>
#include <dlfcn.h>
#include <memory>
#include <string>
namespace home_native_platform {
using Binder=std::unique_ptr<AIBinder,decltype(&AIBinder_decStrong)>;
using Parcel=std::unique_ptr<AParcel,decltype(&AParcel_delete)>;
inline Binder window(std::string& reason) {
    // Standard platform service API omitted from app NDK headers, no vendor RVA.
    const auto check=reinterpret_cast<AIBinder*(*)(const char*)>(dlsym(RTLD_DEFAULT,"AServiceManager_checkService"));
    Binder result(check?check("window"):nullptr,AIBinder_decStrong);
    if(!result){reason="platform WindowManager unavailable";return result;}
    const auto* type=AIBinder_getClass(result.get());
    if(type) {
        const char* descriptor=AIBinder_Class_getDescriptor(type);
        if(descriptor&&!std::strcmp(descriptor,"android.view.IWindowManager"))return result;
    } else {
        static auto* type=AIBinder_Class_define("android.view.IWindowManager",[](void* data){return data;},[](void*){},
            [](AIBinder*,transaction_code_t,const AParcel*,AParcel*)->binder_status_t{return STATUS_UNKNOWN_TRANSACTION;});
        if(type&&AIBinder_associateClass(result.get(),type))return result;
    }
    reason="WindowManager Binder descriptor unavailable";result.reset();return result;
}
inline bool callbacks(std::string& reason) {
    const auto start=reinterpret_cast<void(*)()>(dlsym(RTLD_DEFAULT,"ABinderProcess_startThreadPool"));
    if(!start){reason="platform Binder callback pool unavailable";return false;}
    // Idempotent; never change the launcher's global pool size.
    start();return true;
}
template<class Write> Parcel call(AIBinder* target,uint32_t code,Write write,std::string& reason,bool* unsupported=nullptr) {
    AParcel* input=nullptr;AParcel* output=nullptr;
    auto status=AIBinder_prepareTransaction(target,&input);
    if(status==STATUS_OK&&input)status=write(input);
    if(status==STATUS_OK&&input){status=AIBinder_transact(target,code,&input,&output,0);input=nullptr;}
    if(input)AParcel_delete(input);
    Parcel reply(output,AParcel_delete);
    if(status!=STATUS_OK||!output){if(unsupported&&status==STATUS_UNKNOWN_TRANSACTION)*unsupported=true;reason="platform Binder transport status="+std::to_string(status);return {nullptr,AParcel_delete};}
    AStatus* value=nullptr;status=AParcel_readStatusHeader(output,&value);
    std::unique_ptr<AStatus,decltype(&AStatus_delete)> result(value,AStatus_delete);
    if(status!=STATUS_OK||!value){reason="platform status header unavailable";return {nullptr,AParcel_delete};}
    if(!AStatus_isOk(value)) {
        const auto exception=AStatus_getExceptionCode(value);const char* message=AStatus_getMessage(value);
        if(unsupported&&(exception==EX_SECURITY||exception==EX_ILLEGAL_ARGUMENT||exception==EX_UNSUPPORTED_OPERATION))*unsupported=true;
        reason="platform Binder exception="+std::to_string(exception)+" message="+std::string(message?message:"").substr(0,160);
        return {nullptr,AParcel_delete};
    }
    return reply;
}
}
