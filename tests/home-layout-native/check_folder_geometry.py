#!/usr/bin/env python3
"""Guard the original-body folder animation path, including partial install fallback."""

from pathlib import Path
import re
import sys


def require(condition, message):
    """Reject a failed invariant, including when Python optimization is enabled."""
    if not condition:
        raise AssertionError(message)


root = Path(__file__).resolve().parents[2]
h = (root / "app/src/main/cpp/targets/home/home_folder_geometry.h").read_text()
a = (root / "app/src/main/cpp/targets/home/home_layout_dart_arm64.S").read_text()
c = (root / "app/src/main/cpp/targets/home/home_layout_hooks.cpp").read_text(
    encoding="utf-8"
)
require(
    len(
        re.findall(
            r'\{"(?:WidgetPositionUtil.getCellPosition|FolderIconGetxController.calOriginPreviewIconLoc)"',
            h,
        )
    )
    == 5,
    "Required invariant failed",
)
stub = a[
    a.index(".macro folder_geometry_splice") : a.index(
        "/* Both finished animation branches"
    )
]
require(
    "sub sp, sp, #704" in stub and "add sp, sp, #704" in stub,
    "Required invariant failed",
)
require(
    "mov x2, sp" in stub and "bl hc_layout_folder_body" in stub,
    "Required invariant failed",
)
require(
    "hc_layout_folder_resume" in stub and "hc_layout_folder_original" in stub,
    "Required invariant failed",
)
require("ldar w10, [x9]" in stub and "cbz w10, 2f" in stub, "Required invariant failed")
require(
    "candidates[i].address + 16" in c and "full.size()" in c,
    "Required invariant failed",
)
require("frame[2] != full[2]" in c, "Required invariant failed")
require(
    "workspace_write(fp, -0x18, workspace_read<double>(fp, -0x30))" in h,
    "Required invariant failed",
)
require("workspace_read<double>(fp, -0x18)" in h, "Required invariant failed")
require(
    "top & 7"
    in (root / "app/src/main/cpp/targets/home/home_indicator_pair.h").read_text(),
    "Required invariant failed",
)
require(
    "thread_local home_layout::WorkspaceRenderSnapshot rendered_workspace" in c,
    "Required invariant failed",
)
require("folder_ready || g_knobs[2]" in c, "Required invariant failed")
require(c.count("&rendered_workspace") == 2, "Required invariant failed")
require(
    "rendered->begin_preview(fp, valid ? g[1] : 0)" in h, "Required invariant failed"
)
require("rendered->finish_preview(fp, top)" in h, "Required invariant failed")
print(
    "folder original-body guards: PASS (5 windows; whole bodies; +16 outside patches; partial install stock fallback; "
    "outgoing argument overwrite; rendered/off-state snapshots; thread-local cache; SP16/Dart8)"
)
if len(sys.argv) > 1:
    from dart_dump import Elf, Symbols, engine

    e = Elf(sys.argv[1])
    s = Symbols(sys.argv[2])
    windows = []
    for name, size, offset, words in re.findall(
        r'\{"([^"]+)", (0x\w+), (0x\w+),\s*\{([^}]+)\}\}', h
    ):
        va, z = max([(v, z) for v, z, n in s.entries if n == name], key=lambda t: t[1])
        require(z == int(size, 16), "Required invariant failed")
        off = int(offset, 16)
        expect = b"".join(
            int(w.strip(), 16).to_bytes(4, "little") for w in words.split(",")
        )
        require(e.read(va + off, 16) == expect, "Required invariant failed")
        require(
            all(
                i.mnemonic not in ["bl", "blr", "b", "ret"]
                for i in engine().disasm(expect, va + off)
            ),
            "Required invariant failed",
        )
        windows.append((va + off, va + off + 16))
        print(f"fixture {name}+{off:#x}: exact 4 instructions; no relocated Dart call")
    require(
        all(not any(a <= b < b2 for a, b2 in windows) for _, b in windows),
        "Required invariant failed",
    )
    for name, label in [
        ("WidgetPositionUtil.getCellPosition", "kFolderPositionOriginal"),
        ("FolderIconGetxController.calOriginPreviewIconLoc", "kFolderSizeOriginal"),
    ]:
        text = h.split(label + "[] = {")[1].split("};")[0]
        expect = b"".join(
            int(w, 16).to_bytes(4, "little") for w in re.findall("0x[0-9a-f]+", text)
        )
        va, z = max([(v, z) for v, z, n in s.entries if n == name], key=lambda t: t[1])
        require(len(expect) == z, "Required invariant failed")
        variant = bytearray(expect)
        calls = h.split(label.replace("Original", "Calls7719") + "[] = {")[1].split(
            "};"
        )[0]
        for offset, word in re.findall(r"\{(0x[0-9a-f]+), (0x[0-9a-f]+)\}", calls):
            at = int(offset, 16)
            variant[at : at + 4] = int(word, 16).to_bytes(4, "little")
        require(e.read(va, z) in (expect, variant), "Required invariant failed")
    print(
        "launcher 7719/7722 exact full bodies: PASS (129 + 169 original instructions)"
    )
