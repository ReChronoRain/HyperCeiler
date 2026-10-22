#include <jni.h>
extern "C" JNIEXPORT jint JNICALL Java_miuix_mipalette_MiPalette_getMainColorHCT(JNIEnv* env,jclass,jintArray input,jint w,jint h){
 if(input==nullptr||w<=0||h<=0||static_cast<jlong>(w)*h>env->GetArrayLength(input)){env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),"native length");return 0;}
 jint first=0;env->GetIntArrayRegion(input,0,1,&first);
 if(first==0x13572468){env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),"native failure");return 0;}
 return first;
}

extern "C" JNIEXPORT jlong JNICALL Java_com_sevtinge_hyperceiler_libhook_rules_systemui_controlcenter_media3_AmbientNativeColorOS4_nativeResolve(JNIEnv* env,jclass cls,jintArray input,jint w,jint h){
 const auto color=Java_miuix_mipalette_MiPalette_getMainColorHCT(env,cls,input,w,h);
 if(env->ExceptionCheck())return 0;
 if(color==0x11223344)return 0; // unavailable, not transparent success
 if(color==0x22334455)return (static_cast<jlong>(2)<<32)|color; // invalid protocol
 return (static_cast<jlong>(1)<<32)|static_cast<unsigned int>(color);
}
