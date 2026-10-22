# ARM64 execution without a connected phone

`run.py` cross-compiles the existing native tests as **AArch64 Linux ELF executables**
and runs them with QEMU user-mode emulation. It verifies each executable's ELF
architecture before running it and saves the command, exit code, log and binary
hash. No Android device, system service, binfmt registration or privileged host
change is required.

The Dock assembly test directly executes the production
`app/src/main/cpp/targets/home/dock_native_motion_arm64.S`. Its checks cover
register/NZCV preservation, Dart stack preservation, tagged/compressed pointers,
immutable banks, scene filters, scalar updates, independent unlock projection,
nonblocking eventfd notification and invalid-pointer rejection counters.

The other tests execute ARM64 builds of production native parsing, resolution,
geometry and patch-planning code. The supplied launcher's `libapp.so`,
`libapp_launcher.so` and `libhyper_os_flutter.so` are **data fixtures**. Their bytes
are parsed and validated but are never loaded as executable vendor code.

This does **not** boot HyperOS, initialize ART or Flutter, load LSPosed, inject a
hook into a running system app or validate rendering, touch interaction, GPU
drivers, battery consumption, actual concurrent races or device recovery.
Passing results therefore do not establish full module compatibility.

## Requirements

- Python 3.11 or newer on Linux.
- QEMU's `qemu-aarch64-static` binary.
- Ubuntu GCC 13's AArch64 cross toolchain and its arm64 glibc/C++ development
  dependencies extracted under one directory, including `usr/bin`,
  `usr/lib/gcc-cross`, `usr/libexec/gcc-cross`, `usr/aarch64-linux-gnu` and
  the binutils host shared libraries under `usr/lib/x86_64-linux-gnu`.
- The three ELF files extracted from the target ROM.

The recorded run uses Ubuntu's official archive packages, downloaded with
`apt-get download` and unpacked with `dpkg-deb --extract` into a private cache.
Package hashes were checked against APT metadata; no package maintainer scripts
were run. Version/hash provenance belongs with the validation output.

```bash
python3 tests/arm64-emulation/run.py \
  --toolchain-root /path/to/extracted-toolchain-root \
  --qemu /path/to/extracted-toolchain-root/usr/bin/qemu-aarch64-static \
  --out /path/to/emulation-results \
  --libapp /path/to/launcher/libapp.so \
  --launcher /path/to/launcher/libapp_launcher.so \
  --flutter /path/to/libhyper_os_flutter.so
```

The default CPU model is QEMU `max`, which supplies an ARM64 instruction
environment rather than a faithful model of the phone's SoC. This is a glibc
native test process, not Android's Bionic environment. Existing Android NDK
compilation checks remain a separate validation step.

`results.json` includes all test outcomes. A failure in the legacy structural
Dart diagnostic is marked `diagnostic: true` so its scope is visible, but still
retains `FAIL` and a nonzero runner exit. The runner never converts that failure
into a pass. Logs are safe to collect individually; the output directory also
contains compiled test executables and intermediate object files.

Test-only maintenance needed to execute the previously skipped Dock test: remove
an invalid GCC diagnostic pragma, supply the four diagnostic counters already
referenced by the production assembly, and assert their rejection paths. The
production assembly is unchanged.

QEMU documents the user-mode syscall translation and host/guest memory-model
limitations at <https://www.qemu.org/docs/master/user/main.html>.
