#!/usr/bin/env bash
# Host tests for the home-layout natives.
#
# run.sh [<libapp_launcher.so> [<libapp.so>]] - optional fixtures enable the
# cross-checks against a real launcher image.
#
# Toolchain, and why there are two paths
# --------------------------------------
# The only C++ compiler guaranteed to exist here is the NDK one, and it ships no
# host libc++ for a Windows target. That is enough for the six tests below,
# which are pure arithmetic over the production headers: they build the way
# tests/folder-close-landing does it, --target=x86_64-pc-windows-msvc with
# -nostdinc++ against the freestanding shim, linked with ld.lld's `link` flavour.
#
# `unwind_test` and `dart_targets_test` are different in kind: they parse ELF
# sections and read files, so they need <span>, <fstream> and a real std::vector.
# A declaration-only shim cannot carry that - the link would either fail or, worse,
# link against stubs that return zeros. Rather than pretend, they are SKIPPED with
# a visible notice when only the NDK toolchain is present. Install a host clang++
# (or put one on PATH) and they run for real.
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/../.." && pwd)"
# GNU mktemp needs at least six X in the template; Git Bash for Windows enforces
# the same rule, so the shorter "-t name" form aborted the suite before it ran.
build_dir="$(mktemp -d -t home-layout-tests-XXXXXX)"
trap 'rm -r "$build_dir"' EXIT

# Pure-arithmetic tests: buildable with the freestanding shim.
SHIM_TESTS="workspace_geometry_test folder_geometry_test folder_render_snapshot_test
drop_render_snapshot_test indicator_pair_test hotseat_capacity_test"
# Tests that need a real host standard library.
STDLIB_TESTS="unwind_test dart_targets_test"

ndk_bin="${HC_NDK_BIN:-/d/build-tools/android-sdk/ndk/28.2.13676358/toolchains/llvm/prebuilt/windows-x86_64/bin}"
have_host_clang=0
command -v clang++ >/dev/null 2>&1 && have_host_clang=1

if [ -x "$ndk_bin/ld.lld.exe" ] && [ "$have_host_clang" -eq 0 ]; then
    shim="$(cd "$project_dir/tests/folder-close-landing/host-shim" && pwd -W)"
    link="$(cd "$project_dir/tests/folder-close-landing/host-link" && pwd -W)"
    tests="$(cd "$project_dir/tests/home-layout-native" && pwd -W)"
    cpp="$(cd "$project_dir/app/src/main/cpp" && pwd -W)"
    # MSYS rewrites /nodefaultlib into a path unless path conversion is off, but
    # with it off the tools need Windows paths. `pwd -W` above supplies those.
    export MSYS_NO_PATHCONV=1

    # The import libraries and the freestanding entry point are generated, not
    # committed; build them once per run.
    "$ndk_bin/clang-cl.exe" --target=x86_64-pc-windows-msvc -c "$link/crt.c" \
        -I "$link" -o "$link/crt.obj"
    for stem in msvcrt kernel32 printf; do
        "$ndk_bin/llvm-dlltool.exe" -d "$link/$stem.def" -l "$link/$stem.lib"
    done

    build() {
        local name="$1"
        "$ndk_bin/clang++.exe" --target=x86_64-pc-windows-msvc -std=c++20 -nostdinc++ \
            -O2 -Wall -Wextra -Werror -fno-exceptions -fno-rtti -fno-stack-protector \
            -I "$shim" -I "$link" -I "$cpp" \
            -c "$tests/$name.cpp" -o "$build_dir/$name.obj"
        "$ndk_bin/ld.lld.exe" -flavor link -entry:entry /nodefaultlib /subsystem:console \
            "$build_dir/$name.obj" "$link/crt.obj" "$link/msvcrt.lib" "$link/kernel32.lib" \
            "$link/printf.lib" -out:"$build_dir/$name.exe"
    }

    for name in $SHIM_TESTS; do build "$name"; done
    for name in $SHIM_TESTS; do "$build_dir/$name.exe"; done

    echo
    echo "SKIPPED (need a host C++ standard library; only the NDK toolchain is present):"
    for name in $STDLIB_TESTS; do echo "  - $name"; done
else
    if [ "$have_host_clang" -eq 0 ]; then
        echo "no C++ toolchain: install clang++, or set HC_NDK_BIN to an NDK bin directory" >&2
        exit 1
    fi
    for name in $SHIM_TESTS $STDLIB_TESTS; do
        clang++ -std=c++20 -Wall -Wextra -Werror -O2 \
            -I"$project_dir/app/src/main/cpp" \
            "$project_dir/tests/home-layout-native/$name.cpp" -o "$build_dir/$name"
    done
    # unwind_test <libapp_launcher.so> verifies the Rust-side resolver against the
    # extracted launcher image; dart_targets_test <libapp.so> cross-checks the
    # Dart-side resolver against whatever the symbol table names.
    if [ "$#" -ge 1 ]; then "$build_dir/unwind_test" "$1"; else "$build_dir/unwind_test"; fi
    if [ "$#" -ge 2 ]; then "$build_dir/dart_targets_test" "$2"; else "$build_dir/dart_targets_test"; fi
    for name in $SHIM_TESTS; do "$build_dir/$name"; done
fi

# The two checkers are plain Python and need ordinary paths, so path conversion
# is restored for them (the NDK tools above need it off).
( unset MSYS_NO_PATHCONV
  python3 "$project_dir/tests/home-layout-native/check_indicator_policy.py"
  python3 "$project_dir/tests/home-layout-native/check_folder_geometry.py" )
