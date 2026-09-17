#!/usr/bin/env bash
# Host tests for the home-layout natives.
#
# Each test is a self-contained binary built with the project's own headers.
# run.sh [<libapp_launcher.so> [<libapp.so>]] - optional fixtures enable the
# cross-checks against a real launcher image.
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/../.." && pwd)"
build_dir="$(mktemp -d -t home-layout-tests)"
trap 'rm -r "$build_dir"' EXIT

build() {
    local name="$1"
    shift
    clang++ -std=c++20 -Wall -Wextra -Werror -O2 \
        -I"$project_dir/app/src/main/cpp" \
        "$project_dir/tests/home-layout-native/$name.cpp" -o "$build_dir/$name"
}

build unwind_test
build dart_targets_test

# unwind_test <libapp_launcher.so> verifies the Rust-side resolver against the
# extracted launcher image; dart_targets_test <libapp.so> verifies the Dart-side
# structural resolver and cross-checks it with whatever the symbol table says.
if [ "$#" -ge 1 ]; then
    "$build_dir/unwind_test" "$1"
else
    "$build_dir/unwind_test"
fi
if [ "$#" -ge 2 ]; then
    "$build_dir/dart_targets_test" "$2"
else
    "$build_dir/dart_targets_test"
fi
