# OS4 recent-card dismissal animation

Run `python3 run.py` in this directory. These tests exercise the shipping resolver
and renderer, using executable-only captures from desktop 8584. The historical five-function
8583 fixture lacks the appearance helper and checks missing-graph refusal; it
is not evidence that this revised helper is supported on that version. The
fixture source hashes identify the independent original ELF captures. Symbols
were used only to extract test evidence and never participate in production.

The complete 8584 capture covers instruction corruption, missing/ambiguous owners, independent
function relocation, shifted object fields and frame slots, neutral/disabled and
downward passthrough, bit-identical original displacement, full-size/opaque upward drag, and late
scale/opacity-only arguments in the original appearance helper. There is no
extra drag gain or enum/branch redirection. Controller progress, normalisation,
friction, translation, springs and timing retain their original calculation.
The scale argument local is replaced only after its trajectory readers have
finished; the resolver rejects additional later readers. No Dart objects
are allocated or retained and no managed helper is invoked by the native body.
The original dismissal controller, task deletion, lock and cancellation logic
are retained.

The provider publishes the preference in schema-4 cursor extras from the same
getAll revision. HCL4 HCPO remains count=2, low-word then high-word; bit 1 carries
this option. Bit 0 still controls hiding the report item. HCRC remains 11 words
and HCRH remains the two header flags. HCS1 is unchanged. New code is required in
system_server and launcher; an APK update requires a reboot before device
verification. Default off and missing/unsupported graphs preserve stock behavior.

Host checks do not prove the rendered animation. Device checks still need upward
drag/release, cancelled removal, locked cards, supported task-list orientations,
and interactions with card scaling and animation-speed settings.
