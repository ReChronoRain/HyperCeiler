# Scaled large-folder app return

The reproduced path is opening an exposed application directly from a closed
large folder on the desktop, then returning home. The launcher's landing helper
already obtains the selected preview item's scaled position, but constructs its
right/bottom boundaries with an unrelated cached square length. The desktop
renderer instead uses that item's actual width and height. Scaling therefore
leaves the animation destination and the painted preview with different sizes.

The fix observes the original coordinate producer's live item, copies only two
unboxed dimensions, and uses them in the landing function's existing arithmetic.
It does not apply another scale multiplier. Neutral/disabled folder scaling keeps
the stock result. The existing five open-folder close-animation banks retain
their ordinary-icon source dimensions.

## Runtime proof and ABI

`launcher-7751.json`, `launcher-8583.json` and `launcher-8584.json` contain real
instruction fixtures and source hashes. Their names and addresses are offline
test evidence only. Production discovers the executable producer/list/accessor,
shared coordinate function, independent painted-Rect consumer, and landing
consumer graph. Both Rect paths must use the same allocator and field roles.
Addresses, caller return, object fields and frame operands come from that image.
Missing links, ambiguous candidates, changed arithmetic, allocator mismatches
and branches into replay interiors decline the bank.

The fixed register roles and four-instruction Dart frame/consumer ABI are checked
explicitly. Tagged field offsets remain byte offsets and dimensions are read as
eight-byte doubles. No original Dart BL, allocation or GC return PC moves. No
movable object pointer is retained and no new Dart stack-map slot is introduced.

Eight native thread-local scalar records cover nested invocations. Records use
the native Dart stack-frame address as an identity, never as an object root.
Width and height consume one snapshot in order; missing captures, invalid values,
overflow and partial installation safely retain stock lengths. Once width was
accepted, its height remains valid across readiness/preference changes. Image
generation changes invalidate thread-local state lazily.

## Offline checks

```sh
python3 tests/folder-preview-return/run.py \
  --image /path/to/7751/libapp.so \
  --image /path/to/8583/libapp.so \
  --image /path/to/8584/libapp.so
python3 tests/folder-preview-return/run_state.py
python3 tests/folder-preview-return/run_callbacks.py
python3 tests/folder-preview-return/verify_arm.py /path/to/android-ndk
```

The resolver tests cover sectionless fixtures, independent function relocation,
changed object/frame fields and 126 missing/ambiguous/corrupt graph cases, plus
the three complete executable images. The ARM64 test executes the shipping
assembly in 360 complete lifecycles over 90 geometries, reproduces 144 original
scaled-size failures, and verifies square/rectangular 60/70/100/125/140% previews,
caller filtering, stock fallback and GPR/SIMD/flags/DSP/native-SP preservation.
State and extracted shipping-callback tests cover reset, nesting, frame reuse,
neutral settings, partial installation, overflow, paired snapshots and TLS
isolation. These tests do not simulate Dart GC or establish device frame pacing.

## Performance audit

The new full-image graph discovery runs in the existing general feature pass,
outside the first-render dependency subset. Bound slots are reused; animation
callbacks do not parse ELF, resolve symbols, query Binder/preferences, take a
mutex, or allocate Dart objects. The shared coordinate hook immediately skips
unrelated callers before entering native C++. Each matching landing query uses
three bounded scalar callbacks; record lookup has a fixed maximum of eight.
The first successful pair emits one diagnostic, rather than per-frame logging.
Android's native TLS runtime may initialize storage on first use; the scalar
state itself does not grow or allocate per query.

`run_callbacks.py` includes an informational host warm-callback benchmark. Its
timings exclude ARM register saving, Android TLS initialization/logging, original
Dart work and device scheduling; they must not be reported as phone frame times.
The existing render-performance regression still verifies that 500 repeated
first-render reads perform one installation and one image/config read, including
unsupported and failed-install cases.

No phone was operated for this change. Device follow-up remains: directly launch
and return from exposed regular/corner previews in 2×2, 2×1 and 1×2 folders at
70/125%, interrupt both directions, and inspect the last-frame handoff and frame
pacing. Also check closing an opened folder, ordinary icon independence, neutral
settings and the limited built-in `feature=folder_preview_return` diagnostic.
