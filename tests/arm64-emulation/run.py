#!/usr/bin/env python3
"""Build real AArch64 test executables, then execute them with QEMU Linux user mode.

This is a native-layer test, not an Android/LSPosed/HyperOS system emulator.
Vendor ELF fixtures remain data: this runner never loads or executes their code.
Tools must already exist; this script installs nothing and changes no host settings.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import resource
import shlex
import subprocess
import sys
import time


def sha256(path):
    with open(path, "rb") as handle:
        return hashlib.file_digest(handle, "sha256").hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--toolchain-root", required=True, type=Path,
                        help="Extracted Ubuntu cross-toolchain root (contains usr/bin)")
    parser.add_argument("--qemu", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--libapp", required=True, type=Path)
    parser.add_argument("--launcher", required=True, type=Path)
    parser.add_argument("--flutter", required=True, type=Path)
    parser.add_argument("--cpu", default="max")
    args = parser.parse_args()
    project = Path(__file__).resolve().parents[2]
    cpp = project / "app/src/main/cpp"
    tests = project / "tests"
    tc, out = args.toolchain_root.resolve(), args.out.resolve()
    qemu = args.qemu.resolve()
    out.mkdir(parents=True, exist_ok=True)
    resource.setrlimit(resource.RLIMIT_CORE, (0, 0))
    env = os.environ.copy()
    env["LD_LIBRARY_PATH"] = str(tc / "usr/lib/x86_64-linux-gnu")
    env["PYTHONDONTWRITEBYTECODE"] = "1"
    cxx, cc = (tc / "usr/bin" / name for name in
               ("aarch64-linux-gnu-g++-13", "aarch64-linux-gnu-gcc-13"))
    flags = [f"--sysroot={tc}", "-std=c++20", "-O2", "-Wall", "-Wextra",
             "-Werror", "-static", f"-I{cpp}"]
    report = {"host": platform.platform(), "cpu": args.cpu,
              "scope": "ARM64 Linux native tests; no Android, ART, LSPosed or vendor app launch",
              "vendor_elf_executed": False, "tools": {}, "fixtures": {},
              "commands": [], "tests": []}
    for name, path in (("qemu", qemu), ("cxx", cxx), ("cc", cc)):
        report["tools"][name] = {"path": str(path), "sha256": sha256(path),
                                "version": subprocess.check_output(
                                    [str(path), "--version"], env=env, text=True).splitlines()[0]}
    for name in ("libapp", "launcher", "flutter"):
        path = getattr(args, name).resolve()
        setattr(args, name, path)
        report["fixtures"][name] = {"path": str(path), "sha256": sha256(path)}

    def command(name, argv, timeout=180):
        argv = list(map(str, argv))
        started = time.monotonic()
        logfile = out / f"{name}.log"
        with logfile.open("w") as log:
            log.write("$ " + shlex.join(argv) + "\n")
            log.flush()
            try:
                result = subprocess.run(argv, cwd=project, env=env, stdout=log,
                                        stderr=subprocess.STDOUT, timeout=timeout)
                code = result.returncode
            except subprocess.TimeoutExpired:
                log.write(f"TIMEOUT after {timeout} seconds\n")
                code = 124
        report["commands"].append({"name": name, "argv": argv, "exit": code,
                                    "seconds": round(time.monotonic() - started, 3),
                                    "log": str(logfile)})
        return code

    def execute(name, binary, params=(), diagnostic=False):
        code = command(name, [qemu, "-cpu", args.cpu, binary, *params])
        item = {"name": name, "status": "PASS" if code == 0 else "FAIL",
                "exit": code, "diagnostic": diagnostic,
                "binary_sha256": sha256(binary), "log": str(out / f"{name}.log")}
        report["tests"].append(item)
        print(f"{item['status']} {name}" + (" (diagnostic)" if diagnostic else ""), flush=True)

    def build(name, source, extra=(), params=()):
        binary = out / name
        code = command(name + ".build", [cxx, *flags, source, *extra, "-o", binary])
        if code:
            report["tests"].append({"name": name, "status": "BUILD_FAIL", "exit": code})
            print(f"BUILD_FAIL {name}", flush=True)
            return None
        # ELF e_machine=183 proves this is the AArch64 executable, not a host binary.
        header = binary.read_bytes()[:20]
        if header[:6] != b"\x7fELF\x02\x01" or int.from_bytes(header[18:20], "little") != 183:
            raise RuntimeError(f"Not a little-endian ARM64 executable: {binary}")
        execute(name, binary, params)
        return binary

    build("DockNativeArm64Test", tests / "home-dock-window/DockNativeArm64Test.cpp",
          [tests / "home-dock-window/DockNativeArm64Harness.S",
           cpp / "targets/home/dock_native_motion_arm64.S"])
    for name in ("NativeHookElfTest", "NativeHookGotTest", "NativeHookBankTest",
                 "NativeHookCoreResolverTest", "NativeHookPageGuardTest", "NativeHookImageViewTest"):
        build(name, tests / f"nativehook/{name}.cpp")
    for name in ("DockNativeRuntimeTest", "DockNativeContainerTest", "DockNativeResolverTest"):
        params = [args.libapp] if name == "DockNativeResolverTest" else []
        build(name, tests / f"home-dock-window/{name}.cpp", params=params)
    build("NativeHookDartContractTest", tests / "nativehook/NativeHookDartContractTest.cpp",
          params=[args.libapp])
    build("NativeHookFlutterImageTest", tests / "nativehook/NativeHookFlutterImageTest.cpp",
          params=[args.flutter])

    for name in ("workspace_geometry_test", "folder_geometry_test", "folder_render_snapshot_test",
                 "indicator_pair_test", "hotseat_capacity_test", "code_variants_test"):
        build(name, tests / f"home-layout-native/{name}.cpp")
    for kind, test, fixture in (("rust", "unwind_test", args.launcher),
                                ("dart", "dart_targets_test", args.libapp)):
        binary = build(test, tests / f"home-layout-native/{test}.cpp")
        symbols = out / f"{kind}-symbols.txt"
        generated = command(kind + "-symbols", [sys.executable,
            tests / "home-layout-native/fixture_symbols.py", fixture,
            "--kind", kind, "--output", symbols])
        if binary and generated == 0:
            execute(test + "-rom", binary, [fixture, symbols], diagnostic=(kind == "dart"))

    tweaks = cpp / "targets/home/tweaks"
    objects = []
    for name in ("xz_crc32", "xz_crc64", "xz_dec_lzma2", "xz_dec_stream"):
        obj = out / f"{name}.o"
        code = command(name + ".build", [cc, f"--sysroot={tc}", "-std=c99", "-O2",
            f"-I{tweaks / 'xz'}", "-c", tweaks / f"xz/{name}.c", "-o", obj])
        if code:
            raise RuntimeError(f"XZ prerequisite failed: {name}")
        objects.append(obj)
    stubs = f"-I{tests / 'home-layout-native/stubs'}"
    build("symbol_reset_test", tests / "home-layout-native/symbol_reset_test.cpp",
          [stubs, tweaks / "symtab.cpp", *objects])
    guards = out / "production_compatibility_guards.inc"
    code = command("extract-guards", [sys.executable,
        tests / "home-layout-native/extract_compatibility_guards.py",
        cpp / "targets/home/home_layout_hooks.cpp", guards])
    if code:
        raise RuntimeError("Guard extraction failed")
    build("tweaks_fixture_test", tests / "home-layout-native/tweaks_fixture_test.cpp",
          [stubs, f"-I{out}", *[tweaks / f"{name}.cpp" for name in
                                ("symtab", "scanner", "image", "ht_plan", "blob")], *objects],
          params=[args.libapp])
    report["totals"] = {status: sum(t["status"] == status for t in report["tests"])
                        for status in ("PASS", "FAIL", "BUILD_FAIL")}
    report["unexpected_failures"] = sum(t["status"] != "PASS" and not t.get("diagnostic", False)
                                        for t in report["tests"])
    (out / "results.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report["totals"]), flush=True)
    # Preserve any diagnostic failure in the exit code; do not convert it into a pass.
    return int(any(t["status"] != "PASS" for t in report["tests"]))


if __name__ == "__main__":
    sys.exit(main())
