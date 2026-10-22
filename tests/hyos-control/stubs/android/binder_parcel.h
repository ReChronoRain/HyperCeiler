#pragma once
#include "binder_ibinder.h"
struct AParcel {int32_t values[47]{};int bytes=188;int pos=0;int fail_index=-1;};
int32_t AParcel_getDataSize(const AParcel *);
binder_status_t AParcel_readInt32(const AParcel *,int32_t *);
void AParcel_delete(AParcel *);
