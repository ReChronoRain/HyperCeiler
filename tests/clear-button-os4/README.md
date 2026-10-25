# OS4 clear-button runtime discovery

`home_clear_feature_resolver.h` reads executable PT_LOAD instructions. It does
not use section tables, Dart names, precomputed RVAs, build IDs or private-symbol
caches. The fixtures retain original virtual addresses only as test oracles.
The production engine deliberately excludes features 9 and 18 from the legacy
symbol/fingerprint planner and from imported site caches/packs.

Hide mode identifies one controller visibility update, both original animation
calls, and the initializer that publishes that controller. Disable mode links:

1. The fold container's typed controller lookup, mode field, dock-state query,
   and its original constant-empty-widget return arm.
2. The normal/fold selector using exactly the same controller lookup and field.
3. The selector's overlay insertion method, including its own entry field check,
   entry publication, overlay insertion and process-wide owner publication.
4. Every direct caller of that insertion method. Each must immediately overwrite
   X0, so omitting the insertion cannot leave a live return value behind.

The original four-word fold return is copied from the current executable's own
empty arm; no substitute managed object or allocation is introduced. Branches
into the replacement window are rejected. The engine checks live preimages both
at discovery and before later reapplication after reverting its own journal.
Unknown/missing/ambiguous graphs preserve OEM behavior and produce a limited
`HyperCeiler.HomeLayoutEngine` diagnostic.

The fixed frame/register roles, compressed-reference instruction widths and Dart
bool bit are the verified ARM64 ABI. The linked control-flow shapes are semantic
contracts supported by the captured releases; this is not a cross-version
compatibility promise. Field offsets, pool operands, branch/call destinations and
patch locations come from the current image.

Run the host resolver and actual production patch planner:

```sh
python3 tests/clear-button-os4/run.py --corpus /tmp/hc-clear-arm-corpus
```

Then run the actual emitted return instructions with Python Unicorn installed:

```sh
python3 tests/clear-button-os4/verify_arm.py --corpus /tmp/hc-clear-arm-corpus
```

7751, 8583 and 8584 fixtures cover independent relocation, field/pool movement,
missing dependencies, ambiguity, corrupted instructions, unsafe call results,
inbound branches, on/off defaults, exact journal preimages and reapplication
conflicts. The production planner is linked with a symbol resolver that always
fails and counts requests; successful plans must make zero requests. ARM64
execution covers the original and relocated plans, original pool load, Dart
frame return, all GPR/Q registers, flags and the separate native SP.

This suite does not run the desktop UI. Device acceptance still needs the
Default / Hide / Disable choices exercised across recents reopen and a launcher
restart, including the actual folded layout on suitable hardware. Verify both
button visibility and clear behavior, plus return to the default setting. Host
patch generation, a ready log or a phone-only ordinary layout cannot establish
the whole device matrix.
