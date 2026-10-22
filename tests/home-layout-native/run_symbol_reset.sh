#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/../.." && pwd)"
build_dir="$(mktemp -d -t symbol-reset-tests.XXXXXX)"
trap 'rm -r "$build_dir"' EXIT
cpp="$project_dir/app/src/main/cpp"
tweaks="$cpp/targets/home/tweaks"
for name in xz_crc32 xz_crc64 xz_dec_lzma2 xz_dec_stream; do
    "${CC:-cc}" -std=c99 -O2 -I"$tweaks/xz" -c "$tweaks/xz/$name.c" -o "$build_dir/$name.o"
done
"${CXX:-clang++}" -std=c++20 -O2 -Wall -Wextra -Werror \
    -I"$cpp" -I"$project_dir/tests/home-layout-native/stubs" \
    "$project_dir/tests/home-layout-native/symbol_reset_test.cpp" \
    "$tweaks/symtab.cpp" "$build_dir/"*.o -o "$build_dir/symbol_reset_test"
"$build_dir/symbol_reset_test"
