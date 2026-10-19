#!/usr/bin/env python3
"""Build/stage a target-specific experimental companion; never install it."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess  # nosec B404


def require(condition, message):
    """Reject a failed invariant, including when Python optimization is enabled."""
    if not condition:
        raise AssertionError(message)


ROOT = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument("--source-dir", type=Path, required=True)
p.add_argument("--build-dir", type=Path, required=True)
p.add_argument("--sdk", type=Path, required=True)
p.add_argument(
    "--ndk",
    type=Path,
    required=True,
    help="Android NDK directory (reviewed: 28.2.13676358)",
)
p.add_argument("--cmake", default="cmake", help="CMake executable (reviewed: 3.22.1)")
p.add_argument("--ninja", default="ninja", help="Ninja executable")
p.add_argument(
    "--ndk-host-tag", default="linux-x86_64", help="NDK prebuilt host directory"
)
p.add_argument("--stage", type=Path, required=True)
p.add_argument("--label", required=True)
p.add_argument("--report", type=Path, required=True)
a = p.parse_args()
if not re.fullmatch(r"test[1-9][0-9]*", a.label):
    raise SystemExit("Expected a test release label")
for path in (a.stage, a.report):
    if path.exists():
        raise SystemExit("Refusing to overwrite " + str(path))
source = a.source_dir.resolve()
build = a.build_dir.resolve()
sdk = a.sdk.resolve()
stage = a.stage.resolve()


def sha(path):
    """Hash an input or produced artifact."""
    return hashlib.sha256(path.read_bytes()).hexdigest()


expected_sdk = "957922aeec1bb31091cc37dca29267b7fa41e4e109f679af1526ee4c5e4325c8"
require(
    sha(sdk / "zygisk_next_api.h") == expected_sdk,
    "SDK header differs from reviewed API4",
)
ndk = a.ndk.resolve()
cmake = a.cmake
ninja = shutil.which(a.ninja) or str(Path(a.ninja).resolve())
llvm = ndk / "toolchains/llvm/prebuilt" / a.ndk_host_tag / "bin"
cpp = source / "app/src/main/cpp"
before = {str(f.relative_to(source)): sha(f) for f in cpp.rglob("*") if f.is_file()}
a.report.parent.mkdir(parents=True, exist_ok=True)
commands = []


def run(name, cmd):
    # Developer-selected tools/fixtures only; argv is separate and no shell is used.
    r = subprocess.run(  # nosec B603
        list(map(str, cmd)), capture_output=True, text=True
    )
    a.report.with_name(a.report.stem + "-" + name + ".log").write_text(
        r.stdout + r.stderr
    )
    commands.append(
        dict(name=name, command=list(map(str, cmd)), returncode=r.returncode)
    )
    if r.returncode:
        raise RuntimeError(name + " failed: " + r.stderr + r.stdout[-6000:])
    return r.stdout


run(
    "configure",
    [
        cmake,
        "-S",
        cpp,
        "-B",
        build,
        "-G",
        "Ninja",
        "-DCMAKE_MAKE_PROGRAM=" + str(ninja),
        "-DCMAKE_TOOLCHAIN_FILE=" + str(ndk / "build/cmake/android.toolchain.cmake"),
        "-DANDROID_ABI=arm64-v8a",
        "-DANDROID_PLATFORM=android-35",
        "-DANDROID_STL=c++_static",
        "-DCMAKE_BUILD_TYPE=RelWithDebInfo",
        "-DHYPERCEILER_HYOS_BACKEND=ON",
        "-DHYPERCEILER_ZYGISK_NEXT_API_DIR=" + str(sdk),
    ],
)
run("build", [cmake, "--build", build, "-j4"])
lib = build / "libHyperCeilerHyos.so"
dynamic = run("dynamic", [llvm / "llvm-readelf", "-d", lib])
symbols = run("symbols", [llvm / "llvm-readelf", "--dyn-syms", "--wide", lib])
require(
    re.search(r"\bOBJECT\s+GLOBAL\s+DEFAULT\s+\d+\s+zn_module\b", symbols),
    "Required invariant failed",
)
needed = re.findall(r"Shared library: \[(.*?)\]", dynamic)
require(
    set(needed)
    <= set(
        [
            "libandroid.so",
            "libbinder_ndk.so",
            "liblog.so",
            "libm.so",
            "libdl.so",
            "libc.so",
        ]
    ),
    needed,
)
require("libc++_shared.so" not in needed, "Required invariant failed")
require(
    before
    == {str(f.relative_to(source)): sha(f) for f in cpp.rglob("*") if f.is_file()},
    "Source changed during native build",
)
stage.mkdir(parents=True)
for f in (ROOT / "packaging/hyos-companion").iterdir():
    if f.name == "module.prop.in":
        (stage / "module.prop").write_text(
            f.read_text()
            .replace("@LABEL@", a.label)
            .replace("@VERSION_CODE@", a.label[4:])
        )
    elif f.is_file():
        shutil.copy2(f, stage / f.name)
dest = stage / "lib/arm64-v8a/libHyperCeilerHyos.so"
dest.parent.mkdir(parents=True)
shutil.copy2(lib, dest)
run("strip", [llvm / "llvm-strip", "--strip-debug", dest])
shutil.copy2(source / "LICENSE", stage / "LICENSE-AGPL.txt")
shutil.copy2(
    ndk / "toolchains/llvm/prebuilt" / a.ndk_host_tag / "NOTICE",
    stage / "NOTICE-LLVM.txt",
)
(stage / "SOURCE-AND-API.txt").write_text(
    "Sources: the exact HyperCeiler Git checkout used for this build; retain its commit and local diff.\n"
    "Zygisk Next SDK is an external build dependency and is not redistributed here.\n"
    "Pinned SDK commit: 22fe320814656e7b44715b5f32c967ceb51cda08\n"
    "SDK header SHA256: "
    + expected_sdk
    + "\nhttps://github.com/LSPosed/ZygiskNext/blob/22fe320814656e7b44715b5f32c967ceb51cda08/zygisk_next_api.h\n"
    "The companion contains the project native code, Apache-2.0 launcher tweaks, "
    "and the statically linked NDK C++ runtime; retain the accompanying source licenses and LLVM notice.\n"
)
run("installer-syntax", ["sh", "-n", stage / "customize.sh"])
report = dict(
    passed=True,
    label=a.label,
    source_directory=str(source),
    native_library_sha256=sha(dest),
    unstripped_library_sha256=sha(lib),
    stage=str(stage),
    needed=needed,
    external_sdk_header_sha256=expected_sdk,
    commands=commands,
    sources=[dict(path=n, sha256=d) for n, d in sorted(before.items())],
    files=[
        dict(path=str(f.relative_to(stage)), sha256=sha(f), bytes=f.stat().st_size)
        for f in sorted(stage.rglob("*"))
        if f.is_file()
    ],
    device_status="NOT_TESTED",
    limitations=[
        "Installable staging is not a completed device installation or injection test.",
        "Startup-only Dart layouts can miss their initial computation; no full feature or long-term stability guarantee.",
    ],
)
a.report.write_text(json.dumps(report, indent=2) + "\n")
print(
    json.dumps(
        {
            k: report[k]
            for k in (
                "passed",
                "label",
                "native_library_sha256",
                "stage",
                "device_status",
            )
        },
        indent=2,
    )
)
