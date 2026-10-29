/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cstdint>
#include <cstddef>
#include <vector>
using binder_status_t=int32_t;
using transaction_code_t=uint32_t;
constexpr binder_status_t STATUS_OK=0,STATUS_UNKNOWN_TRANSACTION=-74,STATUS_BAD_VALUE=-22,STATUS_FAILED_TRANSACTION=-2147483646;
constexpr int ANDROID_LOG_INFO=4,ANDROID_LOG_WARN=5;
struct AParcel {std::vector<std::byte> bytes;size_t position=0;};
struct AIBinder_Class {};
struct AIBinder {};
extern "C" {
int __android_log_print(int,const char*,const char*,...);
AIBinder* AServiceManager_getService(const char*);
const AIBinder_Class* AIBinder_getClass(AIBinder*);
const char* AIBinder_Class_getDescriptor(const AIBinder_Class*);
AIBinder_Class* AIBinder_Class_define(const char*,void*(*)(void*),void(*)(void*),binder_status_t(*)(AIBinder*,transaction_code_t,const AParcel*,AParcel*));
bool AIBinder_associateClass(AIBinder*,const AIBinder_Class*);
void AIBinder_decStrong(AIBinder*);
binder_status_t AIBinder_prepareTransaction(AIBinder*,AParcel**);
binder_status_t AIBinder_transact(AIBinder*,transaction_code_t,AParcel**,AParcel**,uint32_t);
binder_status_t AParcel_writeInt32(AParcel*,int32_t);
binder_status_t AParcel_writeInt64(AParcel*,int64_t);
binder_status_t AParcel_readInt32(const AParcel*,int32_t*);
int32_t AParcel_getDataPosition(const AParcel*);
int32_t AParcel_getDataSize(const AParcel*);
void AParcel_delete(AParcel*);
}
