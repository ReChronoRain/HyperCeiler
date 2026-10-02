#!/usr/bin/env bash
# Host test runner for the NativeHookRuntime (app/src/main/cpp/nativehook/) and
# the desktop Dart hook's host-testable layers.
#
# Compiles and runs every host test outside Android. On Linux (CI) it also
# builds DockNativeArm64Test with its assembly harness; on macOS that one is
# skipped (needs <sys/eventfd.h> and GNU as directives), and the page-guard
# reader compiles with the -DNHK_NO_PROC_MAPS fallback.
#
# Usage:
#   tests/run_host_native_tests.sh [libapp.so ...]
# Any libapp.so arguments are handed to the resolver/contract tests.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CPP="$ROOT/app/src/main/cpp"
OUT="${TMPDIR:-/tmp}/nativehook-host-tests"
mkdir -p "$OUT"

CXX="${CXX:-clang++}"
CXXFLAGS=(-std=c++20 -O2 -Wall -Wextra -Werror)
case "$(uname -s)" in
  Linux) ;;
  *) CXXFLAGS+=(-DNHK_NO_PROC_MAPS) ;;
esac

failures=0

# build <name> <source> [extra sources/compile flags...]
build() {
  local name="$1"; shift
  local source="$1"; shift
  if ! "$CXX" "${CXXFLAGS[@]}" -I"$CPP" "$@" "$source" -o "$OUT/$name" \
      >"$OUT/$name.build.log" 2>&1; then
    printf 'FAIL %s (compile; see %s/%s.build.log)\n' "$name" "$OUT" "$name"
    failures=$((failures + 1))
    return 1
  fi
  return 0
}

# run <name> [program args...]
run() {
  local name="$1"; shift
  if "$OUT/$name" "$@" >"$OUT/$name.log" 2>&1; then
    printf 'PASS %s\n' "$name"
  else
    printf 'FAIL %s (see %s/%s.log)\n' "$name" "$OUT" "$name"
    failures=$((failures + 1))
  fi
}

# --- NativeHookRuntime core tests (no external fixtures). ---
for test in NativeHookElfTest NativeHookGotTest NativeHookBankTest \
            NativeHookCoreResolverTest NativeHookPageGuardTest NativeHookImageViewTest; do
  build "$test" "$ROOT/tests/nativehook/$test.cpp" && run "$test"
done

# --- Desktop Dart hook layers. ---
build DockNativeRuntimeTest "$ROOT/tests/home-dock-window/DockNativeRuntimeTest.cpp" \
  && run DockNativeRuntimeTest
build DockNativeContainerTest "$ROOT/tests/home-dock-window/DockNativeContainerTest.cpp" \
  && run DockNativeContainerTest

# Resolver + contract tests accept real libapp.so fixtures; with no arguments
# they still verify decoders and skip the image-dependent assertions.
build DockNativeResolverTest "$ROOT/tests/home-dock-window/DockNativeResolverTest.cpp" \
  && run DockNativeResolverTest "$@"
build NativeHookDartContractTest "$ROOT/tests/nativehook/NativeHookDartContractTest.cpp" \
  && run NativeHookDartContractTest "$@"

# Real-image end-to-end test for the page-lifetime guard path: set NHK_FLUTTER_LIB
# to a copy of the runtime library to reproduce the multi-gap segment layout and
# its far-above-4MiB dynamic table. Without it the test reports SKIP.
#
# The two invocations are written out rather than gathered into an array:
# bash 3.2 (Apple's default) rejects `"${empty[@]}"` under `set -u`, which made
# the whole script exit before this point.
if [[ -n "${NHK_FLUTTER_LIB:-}" ]]; then
  build NativeHookFlutterImageTest "$ROOT/tests/nativehook/NativeHookFlutterImageTest.cpp" \
    && run NativeHookFlutterImageTest "$NHK_FLUTTER_LIB"
else
  build NativeHookFlutterImageTest "$ROOT/tests/nativehook/NativeHookFlutterImageTest.cpp" \
    && run NativeHookFlutterImageTest
fi

# --- Assembly harness: needs Linux *and* an aarch64 host. ---
# The harness executes ARM64 instructions, so it cannot run under an x86_64
# runner: the OS check alone was not enough (ubuntu-latest is x86_64).
HOST_OS="$(uname -s)"
HOST_ARCH="$(uname -m)"
if [[ "$HOST_OS" == "Linux" && "$HOST_ARCH" == "aarch64" ]]; then
  build DockNativeArm64Test "$ROOT/tests/home-dock-window/DockNativeArm64Test.cpp" \
    "$ROOT/tests/home-dock-window/DockNativeArm64Harness.S" \
    "$ROOT/app/src/main/cpp/targets/home/dock_native_motion_arm64.S" \
    && run DockNativeArm64Test
else
  printf 'SKIP DockNativeArm64Test (needs Linux + aarch64; host is %s/%s)\n' \
    "$HOST_OS" "$HOST_ARCH"
fi

if [[ "$failures" -eq 0 ]]; then
  printf '\nAll host native tests passed.\n'
else
  printf '\n%d host native test(s) failed.\n' "$failures"
fi
exit "$failures"
