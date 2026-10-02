#!/usr/bin/env python3
"""Regression for the page-policy interior bank and original empty-widget return."""
from pathlib import Path
import re
ROOT = Path(__file__).resolve().parents[2]
def check(hooks, assembly):
    resume = int(re.search(r"hc_layout_indicator_build_empty = g_dart->load_base \+ va \+ (0x[0-9a-f]+);", hooks)[1], 16)
    # InlineSlot<4> replaces [0x3fc, 0x40c). +0x408 is NOT an original instruction.
    assert not 0x3fc <= resume < 0x40c, "empty return enters overwritten patch bytes"
    assert resume == 0x40c, "must resume original epilogue after replaying the pool load"
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
for bad_h, bad_a in [(hooks.replace("va + 0x40c;", "va + 0x408;"), assembly),
                     (hooks, assembly.replace("ldr x0, [x27, #0x6250]", "nop")),
                     (hooks, assembly.replace("tbnz w0, #4, 2f", "tbz w0, #4, 2f"))]:
    try:
        check(bad_h, bad_a)
    except AssertionError:
        continue
    raise AssertionError("regression checker accepted a known broken policy")
print("indicator policy: bank-boundary/pool-replay/Dart-bool/loader-adoption; 3 bad variants rejected PASS")
