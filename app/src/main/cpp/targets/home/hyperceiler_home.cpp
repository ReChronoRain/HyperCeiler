/*
 * This file is part of HyperCeiler.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>
#include <sys/system_properties.h>
#include <unistd.h>

#include <atomic>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string_view>

#ifdef HYPERCEILER_DOCK_NATIVE_MOTION
void start_dock_native_motion(int (*hook)(void *, void *, void **), int (*unhook)(void *));
void set_dock_motion_feature_enabled(bool enabled);
uint32_t dock_native_motion_state();
#endif

namespace {

constexpr char kLogTag[] = "HyperCeilerHomeNative";

using HookFunction = int (*)(void *function, void *replacement, void **backup);
using UnhookFunction = int (*)(void *function);
using NativeOnModuleLoaded = void (*)(const char *name, void *handle);

struct NativeApiEntries {
    // Loader-owned LSPosed ABI. unhook_func is optional at runtime: immutable
    // per-generation banks never need to tear down a live replacement.
    uint32_t version;
    HookFunction hook_func;
    UnhookFunction unhook_func;
};

HookFunction g_hook_function = nullptr;
UnhookFunction g_unhook_function = nullptr;
std::atomic_bool g_property_hook_installed = false;
std::atomic_bool g_process_name_hook_installed = false;
std::atomic_bool g_high_device_level = false;
std::atomic_bool g_disable_prestart = false;
std::atomic_bool g_soft_glass = false;
std::atomic_bool g_configured = false;
// 0=not specialized yet, 1=launcher, 2=another app. The spawner's zero state is
// inherited independently by every child.
std::atomic_int g_process_kind = 0;

using SystemPropertyGet = int (*)(const char *name, char *value);
SystemPropertyGet g_original_system_property_get = nullptr;
using SetProcessName = void (*)(const char *name);
SetProcessName g_original_setprogname = nullptr;

bool is_launcher_process() {
    char name[128]{};
    FILE *file = std::fopen("/proc/self/cmdline", "r");
    if (file == nullptr) return false;
    const size_t size = std::fread(name, 1, sizeof(name) - 1, file);
    std::fclose(file);
    return size > 0 && std::string_view(name) == "com.miui.home";
}

void start_motion_after_specialization() {
#ifdef HYPERCEILER_DOCK_NATIVE_MOTION
    const int kind = g_process_kind.load(std::memory_order_acquire);
    // A launcher child re-asserts the start on every later signal. setprogname can run
    // before the hook API is published, and a start that failed at that moment is
    // otherwise unrecoverable in-process: the chain then never publishes a sample and
    // following looks dead until the desktop is restarted. start_dock_native_motion()
    // is idempotent, so re-asserting costs a single atomic exchange.
    if (kind == 1) {
        start_dock_native_motion(g_hook_function, g_unhook_function);
        return;
    }
    if (kind != 0) return;
    char name[128]{};
    FILE *file = std::fopen("/proc/self/cmdline", "r");
    if (file == nullptr) return;
    const size_t size = std::fread(name, 1, sizeof(name) - 1, file);
    std::fclose(file);
    if (size == 0) return;
    const std::string_view process(name);
    if (process == "com.miui.home") {
        g_process_kind.store(1, std::memory_order_release);
        start_dock_native_motion(g_hook_function, g_unhook_function);
    } else if (process != "usap64" && process != "hyos_spawner") {
        // Avoid a /proc read on every property query in unrelated descendants.
        g_process_kind.store(2, std::memory_order_release);
    }
#endif
}

bool is_hyos_spawner_process() {
    char executable[128]{};
    const ssize_t size = readlink("/proc/self/exe", executable, sizeof(executable) - 1);
    if (size <= 0 || static_cast<size_t>(size) >= sizeof(executable)) return false;
    executable[size] = '\0';
    return std::string_view(executable) == "/system_ext/bin/hyos_spawner";
}

bool is_library(std::string_view path, std::string_view name) {
    return path == name || (path.size() > name.size()
        && path.ends_with(name) && path[path.size() - name.size() - 1] == '/');
}

bool is_prestart_property(std::string_view name) {
    return name == "persist.sys.usap_pool_enabled" ||
        name == "persist.sys.dynamic_usap_enabled" ||
        name == "persist.sys.prestart.proc" ||
        name == "persist.sys.prestart.feedback.enable" ||
        name == "persist.sys.launch_response_optimization.enable";
}

int copy_property_value(char *destination, std::string_view value) {
    if (destination == nullptr) return 0;
    const size_t length = value.copy(destination, PROP_VALUE_MAX - 1);
    destination[length] = '\0';
    return static_cast<int>(length);
}

int hooked_system_property_get(const char *name, char *value) {
    // HYOS maps its AOT libapp outside the ordinary linker callback path. The
    // inherited property hook is the earliest reliable post-specialization signal.
    start_motion_after_specialization();
    if (name != nullptr) {
        const std::string_view property(name);
        if (g_disable_prestart.load(std::memory_order_relaxed) &&
            is_prestart_property(property)) {
            return copy_property_value(value, "false");
        }
        if (g_soft_glass.load(std::memory_order_relaxed) &&
            property == "persist.sys.background_blur_supported") {
            return copy_property_value(value, "true");
        }
        if (g_high_device_level.load(std::memory_order_relaxed)) {
            if (property == "ro.config.low_ram.threshold_gb") {
                return copy_property_value(value, "false");
            }
            if (property == "ro.miui.backdrop_sampling_enabled") {
                return copy_property_value(value, "true");
            }
            if (property == "ro.config.device_level_for_animation" ||
                property == "persist.sys.computilityV2.devicelevel") {
                return copy_property_value(value, "2");
            }
        }
    }
    return g_original_system_property_get != nullptr
        ? g_original_system_property_get(name, value) : 0;
}

void hooked_setprogname(const char *name) {
    if (g_original_setprogname != nullptr) g_original_setprogname(name);
#ifdef HYPERCEILER_DOCK_NATIVE_MOTION
    // hyos_spawner calls setprogname after fork and before the launcher enters
    // its native runtime. The hook is inherited by the child, so this is an
    // exact post-specialization signal and does not depend on a later property
    // read or on a fixed launcher address.
    if (name != nullptr && std::string_view(name) == "com.miui.home"
        && g_process_kind.exchange(1, std::memory_order_acq_rel) != 1) {
        __android_log_print(ANDROID_LOG_INFO, kLogTag,
            "launcher child detected after setprogname; starting native motion");
        start_dock_native_motion(g_hook_function, g_unhook_function);
    }
#endif
}

void install_process_name_hook() {
    if (g_hook_function == nullptr ||
        g_process_name_hook_installed.exchange(true, std::memory_order_acq_rel)) {
        return;
    }
    void *target = dlsym(RTLD_DEFAULT, "setprogname");
    if (target == nullptr ||
        g_hook_function(target, reinterpret_cast<void *>(hooked_setprogname),
            reinterpret_cast<void **>(&g_original_setprogname)) != 0) {
        g_process_name_hook_installed.store(false, std::memory_order_release);
        __android_log_print(ANDROID_LOG_ERROR, kLogTag, "failed to hook setprogname");
        return;
    }
    __android_log_print(ANDROID_LOG_INFO, kLogTag,
        "HyperOS 4 launcher specialization hook installed");
}

void install_property_hook() {
    if (g_hook_function == nullptr ||
        g_property_hook_installed.exchange(true, std::memory_order_acq_rel)) {
        return;
    }

    void *target = dlsym(RTLD_DEFAULT, "__system_property_get");
    if (target == nullptr ||
        g_hook_function(target, reinterpret_cast<void *>(hooked_system_property_get),
            reinterpret_cast<void **>(&g_original_system_property_get)) != 0) {
        g_property_hook_installed.store(false, std::memory_order_release);
        __android_log_print(ANDROID_LOG_ERROR, kLogTag,
            "failed to hook __system_property_get");
        return;
    }
    __android_log_print(ANDROID_LOG_INFO, kLogTag,
        "HyperOS 4 launcher property hooks installed");
}

void on_library_loaded(const char *name, void *) {
    if (name == nullptr) return;
    const std::string_view library_name(name);
    const bool launcher = is_launcher_process();
    if ((launcher || is_hyos_spawner_process())
        && is_library(library_name, "libapp_launcher.so")) {
        install_property_hook();
    }
#ifdef HYPERCEILER_DOCK_NATIVE_MOTION
    // HYOS can map libapp.so outside the normal linker callback path. Any later
    // library load is enough to identify the specialized launcher; the worker
    // itself waits for and dynamically resolves libapp.so.
    if (launcher) {
        install_property_hook();
        start_dock_native_motion(g_hook_function, g_unhook_function);
    }
#endif
}

jint native_status() {
    jint status = 0;
    if (g_configured.load(std::memory_order_acquire)) status |= 1 << 0;
    if (g_hook_function != nullptr) status |= 1 << 1;
    if (g_unhook_function != nullptr) status |= 1 << 2;
#ifdef HYPERCEILER_DOCK_NATIVE_MOTION
    const uint32_t motion = dock_native_motion_state();
    if ((motion & 1U) != 0) status |= 1 << 3;
    if ((motion & 2U) != 0) status |= 1 << 4;
    if ((motion & 4U) != 0) status |= 1 << 5;
#endif
    if (is_launcher_process()) status |= 1 << 6;
    if (is_hyos_spawner_process()) status |= 1 << 7;
    return status;
}

}  // namespace

extern "C" JNIEXPORT jint JNICALL
Java_com_sevtinge_hyperceiler_libhook_rules_home_other_NativeHomeHooksOS4_nativeConfigure(
    JNIEnv *, jobject, jboolean high_device_level, jboolean disable_prestart,
    jboolean soft_glass) {
    g_high_device_level.store(high_device_level == JNI_TRUE, std::memory_order_relaxed);
    g_disable_prestart.store(disable_prestart == JNI_TRUE, std::memory_order_relaxed);
    g_soft_glass.store(soft_glass == JNI_TRUE, std::memory_order_relaxed);
    g_configured.store(true, std::memory_order_release);
#ifdef HYPERCEILER_DOCK_NATIVE_MOTION
    if (is_launcher_process() && g_hook_function != nullptr) {
        start_dock_native_motion(g_hook_function, g_unhook_function);
    }
#endif
    return native_status();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_sevtinge_hyperceiler_libhook_rules_home_other_NativeHomeHooksOS4_nativeStatus(
    JNIEnv *, jobject) {
    return native_status();
}

// The launcher-native motion chain is started by process name, so the Dock preference has to be
// pushed in explicitly: without this a launcher inside the module's scope keeps its health worker
// (measured at 22% of a core before the A-group fixes) running for a Dock nobody enabled.
extern "C" JNIEXPORT jint JNICALL
Java_com_sevtinge_hyperceiler_libhook_rules_home_other_NativeHomeHooksOS4_nativeSetDockEnabled(
    JNIEnv *, jobject, jboolean enabled) {
#ifdef HYPERCEILER_DOCK_NATIVE_MOTION
    set_dock_motion_feature_enabled(enabled == JNI_TRUE);
#endif
    return native_status();
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeApiEntries *entries) {
    if (entries == nullptr || entries->hook_func == nullptr) return nullptr;
    const bool spawner = is_hyos_spawner_process();
    __android_log_print(ANDROID_LOG_INFO, kLogTag,
        "native v34 hook API version=%u unhook=%d hyosSpawner=%d launcher=%d",
        entries->version, entries->unhook_func != nullptr, spawner, is_launcher_process());
    g_hook_function = entries->hook_func;
    g_unhook_function = entries->unhook_func;
    // Install before libapp_launcher/libapp run their static initialization and cache the
    // properties. The load callback remains as a retry path for unusual linker ordering.
    if (spawner || is_launcher_process()) install_property_hook();
    if (spawner) install_process_name_hook();
#ifdef HYPERCEILER_DOCK_NATIVE_MOTION
    // In the normal path the spawner does not contain libapp.so. Starting here
    // would spend the entire retry budget before a launcher child is forked.
    if (is_launcher_process()) start_dock_native_motion(g_hook_function, g_unhook_function);
#endif
    // Retaining the callback is harmless in unrelated scoped processes and is
    // required when HYOS specializes a process after native_init returned.
    return on_library_loaded;
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
jint JNI_OnLoad(JavaVM *, void *) {
    return JNI_VERSION_1_6;
}
