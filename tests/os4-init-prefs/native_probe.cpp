/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include <jni.h>
#include <atomic>
namespace { jint values[7]{}; std::atomic_bool fail{false}; }
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM*, void*) { values[0]++; return JNI_VERSION_1_6; }
extern "C" JNIEXPORT jint JNICALL Java_com_sevtinge_hyperceiler_libhook_rules_home_other_NativeHomeHooksOS4_nativeConfigure(
    JNIEnv* env,jobject,jboolean high,jboolean prestart,jboolean glass) {
    values[1]++;
    if(fail.exchange(false)) { env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),"configure unavailable"); return 0; }
    values[2]=high; values[3]=prestart; values[4]=glass; return 1;
}
extern "C" JNIEXPORT jint JNICALL Java_com_sevtinge_hyperceiler_libhook_rules_home_other_NativeHomeHooksOS4_nativeSetDockEnabled(
    JNIEnv*,jobject,jboolean enabled) { values[5]++;values[6]=enabled;return 1; }
extern "C" JNIEXPORT jint JNICALL Java_com_sevtinge_hyperceiler_libhook_rules_home_other_NativeHomeHooksOS4_nativeStatus(
    JNIEnv*,jobject) { return 1; }
extern "C" JNIEXPORT jintArray JNICALL Java_fixture_NativeProbe_snapshot(JNIEnv* env,jobject) {
    auto result=env->NewIntArray(7); env->SetIntArrayRegion(result,0,7,values);return result;
}
extern "C" JNIEXPORT void JNICALL Java_fixture_NativeProbe_failConfigureOnce(JNIEnv*,jobject) {fail.store(true);}
