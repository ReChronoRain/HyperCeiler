Three-runtime Weather integration fixture

Run from source/:
  python3 tests/weather-card-joint-runtime/run.py --output ../acceptance/weather-card-next/NEW-JOINT-RUN --negative-controls

The fixture includes the complete shared Weather controller, existing material
runtime and new card runtime. It retains the prior 34 two-runtime scenarios and
adds 14 card cases plus 4 lifetime response cases (52 total): card-only startup, all three components sharing one
HWS snapshot/worker, current configuration changes, default/malformed/expired
frames, immediate cancellation on contradictory Dart exports, missed/early/second
windows, complete-image refusal, real card permission residue closing all Weather
overrides, no late startup after default, and current cache epoch/revision handling.
Seven source mutations test that these actual cross-component connections matter.

Card image binding uses the actual pinned Dart/Flutter ELF bytes and full production
guards, real near mmap, RX pages, maps reads, CAS and rollback. Its ARM templates are
extracted after Android shared linking and matched against the separately reviewed
and executed machine code. The host embeds them as data; it does not execute ARM.

The old level/material image-identity predicates use the established explicit
adapters, while their actual GOT installers, wrappers and policies execute. Binder
transport, clock, scheduling and selected permission faults are also adapted. One
real pthread worker and the actual HWS decoder/cache connect all three components.

The new lifetime module is an explicit state adapter in this three-runtime suite.
The added cases verify early residue refusal before any worker starts, isolated
card rejection when the observer is unavailable, a busy protection handshake and
lease cancellation when the observer is lost. They do not test an actual madvise
GOT observer or page reclamation. Those require the separate real lifetime suite
and four-runtime joint execution.

The fixture calls the first-card-snapshot API with a controlled prerequisite. The
real loader caller/handle/TID/first-use association is a separate entry test. No
actual phone, Dart VM/GC, Android seccomp/SELinux or long-term pass is claimed.
