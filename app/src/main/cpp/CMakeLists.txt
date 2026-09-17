cmake_minimum_required(VERSION 3.22.1)
project(HyperCeilerNative LANGUAGES CXX)

# Layout: nativehook/ holds the target-agnostic hook infrastructure; targets/
# holds the per-feature entry points and resolvers that bind it to one host.
# The root include dir keeps `nativehook/...` includes valid from any target.
# The library name is the LSPosed native-hook contract: it must match the
# `libHyperCeilerNative.so` entry of META-INF/xposed/native_init.list and the
# `System.loadLibrary` name in NativeHomeHooksOS4.
add_library(HyperCeilerNative SHARED targets/home/hyperceiler_home.cpp)

# The Dart-side trampolines are hand-written: they follow the Dart AOT register
# rules (x15 stack, x16/x17 scratch) rather than AAPCS, so the layout feature needs
# its own assembly sources independent of the dock option below.
enable_language(ASM)

target_sources(HyperCeilerNative PRIVATE targets/home/home_layout_targets.cpp)
target_sources(HyperCeilerNative PRIVATE targets/home/home_layout_config.cpp
    targets/home/home_layout_hooks.cpp
    targets/home/home_layout_dart_arm64.S)

# Launcher tweaks: the sign- and symbol-table-driven plan/apply path for the grid,
# icon-size and recents-clear features. Its files are third-party (Apache-2.0) and keep
# their own headers; the resolver locates every site from the image's own symbol table or
# from a signature that must match exactly once, so nothing here carries a fixed address.
enable_language(C)
set(HYPERCEILER_TWEAKS_DIR ${CMAKE_CURRENT_SOURCE_DIR}/targets/home/tweaks)
target_sources(HyperCeilerNative PRIVATE
    ${HYPERCEILER_TWEAKS_DIR}/entry.cpp
    ${HYPERCEILER_TWEAKS_DIR}/blob.cpp
    ${HYPERCEILER_TWEAKS_DIR}/image.cpp
    ${HYPERCEILER_TWEAKS_DIR}/scanner.cpp
    ${HYPERCEILER_TWEAKS_DIR}/symtab.cpp
    ${HYPERCEILER_TWEAKS_DIR}/ht_plan.cpp
    ${HYPERCEILER_TWEAKS_DIR}/htcache.cpp
    ${HYPERCEILER_TWEAKS_DIR}/xz/xz_crc32.c
    ${HYPERCEILER_TWEAKS_DIR}/xz/xz_crc64.c
    ${HYPERCEILER_TWEAKS_DIR}/xz/xz_dec_lzma2.c
    ${HYPERCEILER_TWEAKS_DIR}/xz/xz_dec_stream.c)
# Third-party sources are held to the project's warning level only as far as they compile:
# their style is not this repository's to fix, and -Werror would otherwise block the build.
file(GLOB HYPERCEILER_TWEAKS_SOURCES
    ${HYPERCEILER_TWEAKS_DIR}/*.cpp ${HYPERCEILER_TWEAKS_DIR}/xz/*.c)
set_source_files_properties(${HYPERCEILER_TWEAKS_SOURCES} PROPERTIES
    COMPILE_OPTIONS "-Wno-everything")

option(HYPERCEILER_DOCK_NATIVE_MOTION "Dynamically resolve OS4 native recents animation" ON)
if(HYPERCEILER_DOCK_NATIVE_MOTION)
    target_sources(HyperCeilerNative PRIVATE
        targets/home/dock_native_hooks.cpp
        targets/home/dock_native_motion.cpp
        targets/home/dock_native_motion_arm64.S)
    target_compile_definitions(HyperCeilerNative PRIVATE HYPERCEILER_DOCK_NATIVE_MOTION=1)
endif()

target_include_directories(HyperCeilerNative PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

target_compile_features(HyperCeilerNative PRIVATE cxx_std_20)
target_compile_options(HyperCeilerNative PRIVATE
    -Wall
    -Wextra
    -Werror
    -fvisibility=hidden
)

target_link_libraries(HyperCeilerNative PRIVATE android binder_ndk log)
