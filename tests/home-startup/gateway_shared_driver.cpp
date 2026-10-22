/* SPDX-License-Identifier: AGPL-3.0-or-later */
#include <cstdio>
#include <dlfcn.h>
int main(int argc, char **argv) {
    if (argc != 2) return 2;
    void *library = dlopen(argv[1], RTLD_NOW | RTLD_LOCAL);
    if (!library) { std::fprintf(stderr, "dlopen: %s\n", dlerror()); return 3; }
    auto run = reinterpret_cast<int (*)()>(dlsym(library, "run_gateway_shared_tests"));
    if (!run) { std::fprintf(stderr, "dlsym: %s\n", dlerror()); return 4; }
    int result = run();
    if (dlclose(library)) return 5;
    return result;
}
