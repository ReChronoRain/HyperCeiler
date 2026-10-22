#pragma once
#include <cstdint>
using binder_status_t=int32_t;
using transaction_code_t=uint32_t;
struct AParcel;
struct AIBinder_Class {const char *descriptor;};
struct AIBinder {const AIBinder_Class *clazz;};
const AIBinder_Class *AIBinder_getClass(AIBinder *);
const char *AIBinder_Class_getDescriptor(const AIBinder_Class *);
const AIBinder_Class *AIBinder_Class_define(const char *,void *(*)(void *),void (*)(void *),binder_status_t (*)(AIBinder *,transaction_code_t,const AParcel *,AParcel *));
bool AIBinder_associateClass(AIBinder *,const AIBinder_Class *);
binder_status_t AIBinder_prepareTransaction(AIBinder *,AParcel **);
binder_status_t AIBinder_transact(AIBinder *,transaction_code_t,AParcel **,AParcel **,uint32_t);
void AIBinder_decStrong(AIBinder *);
