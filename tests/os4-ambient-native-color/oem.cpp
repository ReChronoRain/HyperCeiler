#include <jni.h>
#include <dlfcn.h>
#include <cstdlib>
using Init=void(*)(void*);using Hct=int(*)(void*,int*,int,int,int*);static void* object;static Hct hct;
extern "C" JNIEXPORT void JNICALL Java_local_hyperceiler_colorprobe_MainActivity_loadOem(JNIEnv* e,jclass,jstring s){const char* p=e->GetStringUTFChars(s,nullptr);void* h=dlopen(p,RTLD_NOW|RTLD_LOCAL);e->ReleaseStringUTFChars(s,p);if(!h)abort();object=dlsym(h,"process");hct=(Hct)dlsym(h,"_ZN19MainColorSingleRect15getMainColorHCTEPiiiS0_");((Init)dlsym(h,"_ZN19MainColorSingleRect4initEv"))(object);}
extern "C" JNIEXPORT jint JNICALL Java_local_hyperceiler_colorprobe_MainActivity_oem(JNIEnv* e,jclass,jintArray a,jint w,jint h){jint* p=e->GetIntArrayElements(a,nullptr);if(!p)return 0;int r=hct(object,p,w,h,nullptr);e->ReleaseIntArrayElements(a,p,JNI_ABORT);return r;}
