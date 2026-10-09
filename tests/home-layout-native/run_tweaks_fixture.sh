#!/usr/bin/env bash
# Read-only test of production Dart symbol lookup and patch planning. No AArch64 is executed.
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/../.." && pwd)"
build_dir="$(mktemp -d -t home-tweaks-tests.XXXXXX)"
trap 'rm -r "$build_dir"' EXIT
cpp="$project_dir/app/src/main/cpp"
tweaks="$cpp/targets/home/tweaks"
python3 "$project_dir/tests/home-layout-native/extract_compatibility_guards.py" \
    "$cpp/targets/home/home_layout_hooks.cpp" "$build_dir/production_compatibility_guards.inc"
for name in xz_crc32 xz_crc64 xz_dec_lzma2 xz_dec_stream; do
    "${CC:-cc}" -std=c99 -O2 -I"$tweaks/xz" -c "$tweaks/xz/$name.c" -o "$build_dir/$name.o"
done
"${CXX:-clang++}" -std=c++20 -O2 -Wall -Wextra -Werror \
    -I"$cpp" -I"$build_dir" -I"$project_dir/tests/home-layout-native/stubs" \
    "$project_dir/tests/home-layout-native/tweaks_fixture_test.cpp" \
    "$tweaks/symtab.cpp" "$tweaks/scanner.cpp" "$tweaks/image.cpp" \
    "$tweaks/ht_plan.cpp" "$tweaks/blob.cpp" "$build_dir/"*.o \
    -o "$build_dir/tweaks_fixture_test"
"$build_dir/tweaks_fixture_test" "$@"
