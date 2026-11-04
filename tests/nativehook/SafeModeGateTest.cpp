/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include "nativehook/safe_mode.h"

#include <cstdio>
#include <cstring>

namespace {

int failures = 0;

#define CHECK(expression) do { \
    if (!(expression)) { \
        std::fprintf(stderr, "FAIL line %d: %s\n", __LINE__, #expression); \
        ++failures; \
    } \
} while (false)

const char *g_value = "";
bool g_readable = true;
bool g_fill_without_nul = false;
int g_reads = 0;

bool reader(const char *, char *value, std::size_t capacity) {
    ++g_reads;
    if (!g_readable) return false;
    const std::size_t length = std::strlen(g_value);
    if (length >= capacity) return false;
    if (g_fill_without_nul) {
        std::memset(value, 'x', capacity);
        return true;
    }
    std::memcpy(value, g_value, length + 1);
    return true;
}

void reset(const char *value = "", bool readable = true, bool fill_without_nul = false) {
    g_value = value;
    g_readable = readable;
    g_fill_without_nul = fill_without_nul;
    g_reads = 0;
    nhk::home_safe_mode::set_reader_for_test(&reader);
    nhk::home_safe_mode::set_pid_for_test(1001);
}

} // namespace

int main() {
    using namespace nhk::home_safe_mode;

    reset();
    Decision result = decision();
    CHECK(result.state == State::missing && !result.blocked);
    CHECK(std::strcmp(state_name(result.state), "missing") == 0);

    reset(" home , systemui ");
    result = decision();
    CHECK(result.state == State::safe_mode && result.blocked);

    reset("systemui,home,settings");
    result = decision();
    CHECK(result.state == State::safe_mode && result.blocked);

    reset("systemui,center");
    result = decision();
    CHECK(result.state == State::normal && !result.blocked);

    reset("homex");
    result = decision();
    CHECK(result.state == State::corrupt && result.blocked);

    reset("home,,settings");
    result = decision();
    CHECK(result.state == State::corrupt && result.blocked);

    reset("home;settings");
    result = decision();
    CHECK(result.state == State::corrupt && result.blocked);

    reset("home", false);
    result = decision();
    CHECK(result.state == State::unreadable && result.blocked);

    // A reader that claims success but does not NUL-terminate is corrupt.
    reset("", true, true);
    result = decision();
    CHECK(result.state == State::corrupt && result.blocked);

    // A reader that refuses an oversized property value is unreadable, not normal.
    reset("home,settings,center,demo,systemui,home,settings,center,demo,systemui,home,settings,center,demo,systemui");
    result = decision();
    CHECK(result.state == State::unreadable && result.blocked);

    // A PID change invalidates the cached decision, as it does after fork.
    reset("systemui");
    CHECK(decision().state == State::normal);
    CHECK(g_reads == 1);
    CHECK(decision().state == State::normal);
    CHECK(g_reads == 1);
    g_value = "home";
    set_pid_for_test(1002);
    result = decision();
    CHECK(result.state == State::safe_mode && result.blocked);
    CHECK(g_reads == 2);

    // A malformed value is never treated as normal.
    CHECK(parse(nullptr).blocked);
    CHECK(parse("").state == State::missing);
    CHECK(parse("demo").state == State::normal);

    if (failures == 0) std::puts("SafeModeGateTest passed");
    return failures == 0 ? 0 : 1;
}
