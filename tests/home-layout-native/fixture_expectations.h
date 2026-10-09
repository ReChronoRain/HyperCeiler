/* SPDX-License-Identifier: AGPL-3.0-or-later */
#pragma once
#include <cassert>
#include <cstdint>
#include <fstream>
#include <map>
#include <string>

// Generated independently from the fixture's .gnu_debugdata by fixture_symbols.py.
// An expected address is never used as an input to the production resolver.
inline std::map<std::string, uint64_t> fixture_expectations(const char *path) {
    std::ifstream file(path);
    assert(file);
    std::map<std::string, uint64_t> expected;
    std::string name;
    uint64_t value = 0;
    while (file >> name >> std::hex >> value) {
        assert(value != 0);
        assert(expected.emplace(name, value).second);
    }
    assert(file.eof() && !expected.empty());
    return expected;
}
