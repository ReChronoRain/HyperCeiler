/* SPDX-License-Identifier: AGPL-3.0-or-later */
/*
 * Freestanding process entry for the host-side geometry regressions.
 *
 * The linker is invoked with /nodefaultlib and /entry:entry, so there is no CRT
 * startup object to lean on: `entry` is the raw PE entry point and has to do the
 * argc/argv-free job itself. It must pass the test's exit status to ExitProcess
 * rather than returning, because a Windows entry point that returns does not get
 * its return value turned into a process exit code.
 *
 *   entry:      call main -> eax; ExitProcess(eax)
 *   test_fail:  puts("ASSERTION FAILED"); ExitProcess(1)
 *
 * cassert in host-shim maps every failing assert onto test_fail, which is why
 * that symbol has to be defined here rather than in the shim: crt.obj and a shim
 * definition of the same name collide at link time. The line number arrives in
 * ecx but is unused - stdout is block-buffered under this freestanding CRT, so a
 * printf before ExitProcess would be discarded exactly when it matters most. The
 * banner plus the non-zero status is the whole contract.
 */
#include "crt.h"

/* MinGW/PE style dllimport keeps the reference indirect (__imp_*), matching the
 * import libraries run.py builds from the .def files next to this one. */
__declspec(dllimport) int puts(const char *s);
__declspec(dllimport) void ExitProcess(unsigned int code);

int main(void); /* provided by the translation unit under test */

/* clang emits a reference to _fltused from any object that needs the CRT's
 * floating-point init marker. With /nodefaultlib there is no CRT to define it,
 * so a tentative definition here satisfies the linker. Nothing reads it. */
int _fltused;

void test_fail(int line) {
    (void)line;
    puts("ASSERTION FAILED");
    ExitProcess(1);
}

/* PE entry points are __cdecl void(void); clang is happy to emit one for us. */
void entry(void) {
    ExitProcess((unsigned int)main());
}
