#!/usr/bin/env python3
"""Regression for the page-policy interior bank and original empty-widget return."""
from pathlib import Path
import re
ROOT = Path(__file__).resolve().parents[2]
def check(hooks, assembly):
    # The resume point is derived, not literal: the four-word InlineSlot patch sits at
    # `va + anchors.result`, so the original epilogue resumes 16 bytes past it. An earlier
    # revision asserted the literal `va + 0x40c` and silently stopped matching when the
    # offset became a named anchor - a dead check is worse than no check, because the suite
    # still reported the feature as covered.
    derived = re.search(
        r"hc_layout_indicator_build_empty = g_dart->load_base \+ va \+ anchors\.result \+ (\d+);",
        hooks)
    assert derived, "the empty-widget resume must be derived from the scanned result anchor"
    skip = int(derived.group(1))
    assert skip == 16, "resume must skip the whole 4-word InlineSlot patch"
    # InlineSlot<4> replaces a 16-byte window; the resume has to land exactly at its end.
    # Any skip other than the full window either re-enters the patch or lands mid-instruction.
    assert skip == 4 * 4, "empty return must land on the first original word after the patch"
    # The patched window is [0x3fc, 0x40c) in this image: +0x408 is NOT an original
    # instruction, so a resume anywhere inside it replays our own patch bytes as code.
    patched_lo, patched_hi = 0x3FC, 0x40C
    assert not patched_lo <= patched_lo + skip < patched_hi, "empty return enters overwritten patch bytes"
    entry = assembly.split("hc_layout_indicator_edit_result_entry:")[1].split(".globl hc_layout_indicator_slide_only_entry")[0]
    assert "tbnz w0, #4, 2f" in entry, "Dart false (null+0x30) must select empty"
    empty = re.split(r"(?m)^2:$", entry)[1]; empty = re.split(r"(?m)^1:$", empty)[0]
    assert "ldr x0, [x27, #0x6250]" in empty, "empty widget must replace the bool result"
    assert empty.index("ldr x0, [x27, #0x6250]") < empty.index("br x16")
    assert "g_loader_prime_finished.load(std::memory_order_acquire)" in hooks
    assert "std::find(order.begin(), order.end(), owned_slot)" in hooks
hooks = (ROOT/"app/src/main/cpp/targets/home/home_layout_hooks.cpp").read_text(encoding="utf-8")
assembly = (ROOT/"app/src/main/cpp/targets/home/home_layout_dart_arm64.S").read_text(encoding="utf-8")
check(hooks, assembly)
for bad_h, bad_a in [(hooks.replace("anchors.result + 16;", "anchors.result + 8;"), assembly),
                     (hooks.replace("anchors.result + 16;", "va + 0x408;"), assembly),
                     (hooks, assembly.replace("ldr x0, [x27, #0x6250]", "nop")),
                     (hooks, assembly.replace("tbnz w0, #4, 2f", "tbz w0, #4, 2f"))]:
    try:
        check(bad_h, bad_a)
    except AssertionError:
        continue
    raise AssertionError("regression checker accepted a known broken policy")
print("indicator policy: bank-boundary/pool-replay/Dart-bool/loader-adoption; 3 bad variants rejected PASS")
