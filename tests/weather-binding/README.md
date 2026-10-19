# Pinned Weather image and import binding

`weather_guard.generated.h` is generated only from the four exact ROM ELF files named and hashed in the channel, allocator and Dart evidence. The generator verifies full input SHA256, source region hashes, exported function sizes, relevant relocation records, immutable PT_LOAD ranges, and absence of relocations inside the guarded regions. No guard bytes or instruction masks are hand maintained. Public/standard `.rela.dyn` uses APS2; the decoder follows the grouped-addend behavior in the [AOSP relocation iterator](https://android.googlesource.com/platform/bionic/+/refs/heads/main/linker/linker_reloc_iterators.h), blob `c6a8cf620ab2f7d3ea1c7e921f12e06743265fa2`.

From the directory above `source`:

```
python3 source/tests/weather-binding/generate.py --output <new-dir>
python3 source/tests/weather-binding/generate.py --check --output <new-dir>
python3 source/tests/weather-binding/run.py --output <new-dir>
python3 source/tests/weather-binding/negative_controls.py --output <new-dir>
python3 source/tests/weather-binding/compile_android.py --output <new-dir>
```

The generator emits 34 guards / 9,005 exact bytes: Weather 11, public API 8, Rust standard library 6, Dart 9. This covers the complete five Weather functions in the channel report, all five public DeviceLevel functions, all three matching Rust allocation functions, all six Dart consumer functions, full ELF/program headers and notes, the Dart 832-byte prefix and both complete snapshot headers. Channel and map-field strings are pinned too. This is selected exact runtime-region verification tied to a full source ELF hash, not a runtime hash of every mapped byte or proof of all transitive callees.

Production API:

```
namespace weather_binding {
struct Binding {
    uintptr_t weather_base, public_base, standard_base, slot;
    weather_device_level::Api api;
};
template<class Read> bool verify_weather(uintptr_t base, Read&& read);
template<class Read> bool verify_dart(uintptr_t base, Read&& read);
template<class Read> bool resolve(uintptr_t weather_base, Read&& read, Binding& out);
}
```

`Read` is callable as `bool(uintptr_t, std::span<std::byte>)`, returning false on inaccessible memory. No resolver call obtains a loader handle, reads `/proc`, creates a thread, allocates, locks, invokes any bound object function, or modifies memory. Read chunks are at most 256 bytes of stack storage. `verify_weather` also checks two relocated channel vtable entries. `resolve` requires all five Weather import pointers to match one verified public image, and the public image's allocator/deallocator/shim to match one verified standard image. It rereads imports/dispatch slots before assigning `out` once. Expected original get pointer is `reinterpret_cast<uintptr_t>(binding.api.get)`; the installation slot is `binding.slot`.

The tests load actual ELF PT_LOAD bytes into preallocated host buffers and simulate only the eight relevant import relocations and two channel vtable pointers. They do not build the good fixture from the generated guard bytes. Each of 72,040 guard bits is independently flipped through the memory reader, and the production verifier must reject it. Missing ranges, wrong-image imports, swapped getter functions, allocation mismatch, address overflow, mid-validation pointer changes and untouched failure output are covered. Global `new` throws while the resolver runs; no target function is executed. Four copied production logic mutations must cause runtime test failures. NDK compilation instantiates the actual three entry points for arm64 API35 with exceptions and RTTI disabled.

This is read-only preparation. The caller must verify the observed process/export/loader boundary, keep image lifetimes valid, compare the slot against the expected original pointer at installation, preserve RELRO/protection and foreign ownership, and publish the prepared Binding safely across threads. The final struct assignment is not an atomic publication. Import rereads establish observations, not an atomic transaction across three images or immunity to later changes. Actual native installation, HWS1 lease integration, Android memory-reader behavior, Dart startup timing, card/blur support and real-phone effects are outside these tests. Real phone: NOT_TESTED.
