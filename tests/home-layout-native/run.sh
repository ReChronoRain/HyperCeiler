#!/usr/bin/env bash
# Host tests for the home-layout natives.
#
# Each test is a self-contained binary built with the project's own headers.
# run.sh [<libapp_launcher.so> [<libapp.so>]] - optional fixtures enable the
# cross-checks against a real launcher image.
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/../.." && pwd)"
build_dir="$(mktemp -d -t home-layout-tests.XXXXXX)"
trap 'rm -r "$build_dir"' EXIT

build() {
    local name="$1"
    shift
    "${CXX:-clang++}" -std=c++20 -Wall -Wextra -Werror -O2 \
        -I"$project_dir/app/src/main/cpp" \
        "$project_dir/tests/home-layout-native/$name.cpp" -o "$build_dir/$name"
}

build unwind_test
build dart_targets_test
build workspace_geometry_test
build folder_geometry_test
build folder_render_snapshot_test
build indicator_pair_test
build hotseat_capacity_test
build code_variants_test

# unwind_test <libapp_launcher.so> verifies the Rust-side resolver against the
# extracted launcher image; dart_targets_test <libapp.so> verifies the Dart-side
# structural resolver and cross-checks it with whatever the symbol table says.
if [ "$#" -ge 1 ]; then
    python3 "$project_dir/tests/home-layout-native/fixture_symbols.py" "$1" \
        --kind rust --output "$build_dir/rust-symbols.txt"
    "$build_dir/unwind_test" "$1" "$build_dir/rust-symbols.txt"
else
    "$build_dir/unwind_test"
fi
if [ "$#" -ge 2 ]; then
    python3 "$project_dir/tests/home-layout-native/fixture_symbols.py" "$2" \
        --kind dart --output "$build_dir/dart-symbols.txt"
    "$build_dir/dart_targets_test" "$2" "$build_dir/dart-symbols.txt"
else
    "$build_dir/dart_targets_test"
fi

"$build_dir/workspace_geometry_test"
"$build_dir/folder_geometry_test"
"$build_dir/folder_render_snapshot_test"

"$build_dir/indicator_pair_test"

"$build_dir/hotseat_capacity_test"
"$build_dir/code_variants_test"

python3 "$project_dir/tests/home-layout-native/check_indicator_policy.py"

python3 "$project_dir/tests/home-layout-native/check_folder_geometry.py"

bash "$project_dir/tests/home-layout-native/run_symbol_reset.sh"
