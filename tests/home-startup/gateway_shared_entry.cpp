/* SPDX-License-Identifier: AGPL-3.0-or-later */
// The C++ data uses the production -fvisibility=hidden setting. Assembly target
// definitions remain global/default unless the production gateway hides them.
// Do not link this fixture with -Bsymbolic: that would mask the original PLT bug.
int gateway_test_main();
extern "C" __attribute__((visibility("default"))) int run_gateway_shared_tests() {
    return gateway_test_main();
}
