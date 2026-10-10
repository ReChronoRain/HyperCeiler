# Exact Weather card image binding

This suite compiles the actual `weather_card_binding.h` against the exact OS4.0.0.44 Weather Dart and Flutter ELF bytes. It verifies 54 selected immutable byte ranges (16,324 bytes), actual ELF/program headers and notes, serialized Class254 identity/layout, InstructionsTable boundaries, complete constructor/recompute/consumer functions, complete VM entry3 and eight complete Flutter stack/interrupt functions. The generated header is checked against a fresh extraction before each run.

Run from the workspace root using a new output directory:

```bash
python3 -B source/tests/weather-card-binding/run.py \
  --output acceptance/weather-card-next/binding-v1/reproduction-new
```

Regenerate/check only:

```bash
python3 -B source/tests/weather-card-binding/generate.py --check \
  --output /tmp/weather-card-binding-guard-check-new
```

The suite includes 44 checks, 130,592 individually mutated original guard bits, all 16,324 guarded bytes individually mutated in fully installed state, seven compiled source-fault controls, and independent Android ARM64 API35 NDK instantiation with exceptions/RTTI disabled. Host execution uses UBSan and forbids heap allocation inside the production component calls. ROM memory reads and loaded bases are simulated; code under guard is not executed.

The `weather_card` interface is:

```cpp
struct Binding {
    uintptr_t dart_base;
    uintptr_t flutter_base;
    std::array<uintptr_t, 2> epilogue; // absolute addresses: recompute, constructor
};
bool resolve(uintptr_t dart, uintptr_t flutter, Read&& read, Binding& out);
bool verify_state(const Binding&, Read&& read,
                  const std::array<uint32_t, 2>& expected_branch,
                  unsigned owned_mask);
bool verify_owned(const Binding&, Read&& read,
                  const std::array<uint32_t, 2>& expected_branch);
```

`Read(address, span<byte>)` must return false when it cannot safely read a span. Each read is at most 256 bytes. There are no heap allocations, loader calls, Binder operations, locks or writes inside the component. A supplied reader may still perform syscalls or incur page faults.

`resolve` requires every original byte, including both epilogue words, and publishes `out` only after all checks succeed. It fills both absolute epilogue addresses. The caller must preserve this completed `Binding`; constructing only its two bases does not produce a valid binding.

`verify_state` uses bit0 for recompute at Dart RVA `0x3a8408` and bit1 for the constructor at RVA `0x3aece0`. An unset bit requires the original `0xaa1603e0` word. A set bit requires that site's exact supplied branch word. Only those four bytes may be normalized back to the original word during whole-range comparisons; the remaining three original epilogue instructions and all surrounding bytes must match. Both sites are reread at the end. `verify_owned` is the mask3 convenience function. Masks outside 0..3 are rejected without truncation.

The supplied plan must use non-self AArch64 `B`, not `BL`, avoid address wrap, and branch outside both source-image spans. **The installer remains responsible for approving the actual gateway bytes, kind, lease address, RX mapping, branch-to-template correspondence and lifetimes.** Exact word comparison does not turn a caller-supplied plan into an authenticated gateway. Mask0 also validates the supplied plan; `resolve` is the initial operation before a plan exists.

The caller must independently establish that the Dart base came from the correct observed snapshot and that the Flutter base is its actual owner under the validated loader/caller/handle lifecycle. Two correct images at two addresses do not prove that association. Final rereads detect certain changes during validation but do not create an atomic multiword installation, a reservation or a complete TOCTOU defense.

This suite does not approve installer/HWS integration or real device effects. The standalone card gateway execution has separate evidence, and all device status remains `NOT_TESTED`.
