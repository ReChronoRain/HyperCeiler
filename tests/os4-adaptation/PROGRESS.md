# OS4 adaptation work in progress

The active request covers all currently unadapted OS4 functions. The inventory is
an initial audit of the five explicit desktop preference gates, not the whole
module. It currently has 47 gated entries, 46 declared-adapted entries that still
need execution evidence, and five replaced/legacy entries. Do not enable gates or
mark the overall request complete merely because the code compiles.

## Current changes (2026-10-25)

- Clear-button **hide visibility** (feature 18) and **disable clear** (feature 9):
  both runtime entry paths now discover executable producer/consumer graphs.
  Disable mode links the normal overlay selector and the fold container through
  the same controller lookup and mode field. It proves every overlay call's
  result is immediately dead and uses the fold builder's original constant
  empty-widget arm. No named lookup, cached root or site pack can re-enable
  either migrated path. Before reapplication, the engine rechecks the original
  instructions after journal restoration and declines later external patches.
  All three launcher fixtures (7751, 8583, 8584) pass relocation, moved fields,
  missing links, ambiguity, 34 visibility / 54 disable corruption checks and
  unsafe-result/inbound-branch rejection. The actual production planner emits
  the five expected disable patches with zero symbol/signature requests;
  24 ARM64 executions cover original/relocated graphs, pool operands and exact
  frame/register restoration. Device hide/disable mode tests are pending.
- Hide the report option in the uninstall dialog: an executable eligibility
  predicate and all three consumers are linked at runtime. The boolean-return
  gateway does not allocate or enter a Dart frame. Three launcher fixtures,
  35 predicate corruptions and eight full register cases per fixture pass;
  18 executions of the actual original ARM64 predicates verify list cardinality,
  compressed byte-field offsets and typed-predicate polarity. The existing
  setting has been added to the OS4 UI allowlist. It is implemented and exposed,
  with new endpoint/device verification still pending; it is not marked fully
  device-verified. No uninstall action has been executed in testing.
- Layout policy transport: optional HCPO/count=2/low-word/high-word extension in
  the ordinary authenticated layout response; cache v18 reads old v17 defaults.
  Immutable snapshots and defensive CombinedSnapshot copies preserve the policy
  bits from the same provider revision. Java/native revision is 20261025,
  capability bit 16 advertises the extension. HCS1's 188-word combined wire format
  is unchanged and does not transport these new policy bits; that backend has not
  gained the report feature. This is not evidence of full combined adaptation.
- Object-scale regression: folder dimension objects and widget Observer objects
  were built before the worker installed their hooks. They now participate in
  the same executable-only first-render discovery/slot publication as normal
  icons. No symbol lookup, Binder call, sleep, Dart object retention or slow
  allocation was added to that bootstrap.

## Object-scale device evidence

Xiaomi 15, OS4.0.0.22, launcher RELEASE-8.01.02.8584-260918-10231931-R.
Installed Release SHA-256:
`0e811162f058e14d5e4d4cca52239a7281b64503f134f48e4855cd5c9c5510b9`.

The original installed build reproduced the failure: 70% reached the process,
slots became live, yet the existing folder/widget objects retained stock sizes.
The fixed build was installed using non-incremental ADB and its on-device APK
hash was checked. Separate 70% and 125% desktop restarts showed:

- Small-folder background and preview icons scale together.
- 2x2, 2x1 and 1x2 large-folder backgrounds and previews scale together.
- The existing steps widget scales, and tapping its rendered position opens the
  health application. Its card width is 492 px at baseline, 344 px at 70%, and
  614 px at 125% (rounding included).
- Normal application icons retain their original size while the independent
  folder/widget switches are used. Small and large folders still open.
- Native diagnostics report both first-render families ready and widget
  `hits=2 applied=2` at each non-neutral factor. This is execution evidence in
  addition to screenshots; readiness alone was insufficient in the failing run.

All eight touched scale preferences were restored to their original values and
`debug.hyperceiler.prefs_write` was reset to zero. Desktop is back at its default
page. Raw screenshots, logs, settings comparison and APK verification are under
`/tmp/hc-object-scale-live` on the test host; they are not checked into Git.
Only the existing steps widget was visually exercised, not every widget vendor.
Animation smoothness, other ROM versions and every drag/drop variant have not
been exhaustively tested in this pass.

The installed app uses the new module while system_server is still on endpoint
20261024. All existing scale fields were delivered and verified. HCPO/report
end-to-end testing needs the next authorized phone reboot with the matching
system_server endpoint. Do not describe that new feature as device-verified.

## Validation

- Latest Release assembly and signing verification passed (three-minute Gradle build). The scale-only installed build was the earlier 53-second build.
- Recents native graph/render/cache tests, old v17/v16/v15/v14 cache readers,
  exact policy word order, truncated/unsupported policy rejection, immutable and
  concurrent Java revisions passed.
- Dock-title, persistent logging diagnostics, clear visibility, report predicate,
  ordinary/combined snapshot, folder and widget executable graph regressions run
  independently; preserve their actual result logs, including failures.
- ARM64 gateways: 46 cases including report return, full register/FP state and
  native clobbers; independent scales: 30 scalar and 32 Observer cases passed.
- Folder/widget allocator tests retain both valid Dart nursery alignments and
  malformed/insufficient/disabled/neutral rejection cases required by AGENTS.md.

## Still outstanding

Continue the 47-entry gated inventory, then audit other module/version branches. The
inventory's `declared_adapted_requires_evidence` label deliberately does not mean
all 46 entries were retested. Clear-mode and report device checks, full policy
device transport, the remaining 47 gated features and other module audits are
unfinished. Git publication is tracked by the branch histories; publication does not establish device acceptance.

The latest clear/report Release is compiled and signed but was not installed in
this goal continuation. The phone still has the preceding scale-tested APK and
restored preferences. Revalidate APK hashes and endpoint revisions before the
next device test; a successful local build does not update system_server.

## Render-performance investigation (2026-10-25)

User steering: investigate third-party interrupted-animation stutter and desktop
dropped frames. Reporter logs/settings/version were unavailable. Verified
code risks were fixed: repeated synchronous first-render bank installation when
an optional family is unsupported or an install fails, and uncached process-name
I/O on property queries. A one-attempt image-scoped bootstrap retains worker
recovery; native column/back-region primers are likewise bounded once their image
is readable and hand off to the same worker; PID/rename-generation identity caching retains safe uncached fallback.

The actual pre-fix bootstrap fixture produced 500 installs/500 calls; the fixed
fixture produces one, including missing-family and failed-install cases. The
actual process cache produces one read/10,000 queries with fork/rename/race tests.
Layout/drawer and independent scale host regressions plus 30 scalar/32 Observer
ARM64 cases passed. Latest Release assembly passed in 53 seconds; device frame
performance remains unverified and the phone was not touched for this task.
See `tests/home-render-performance/README.md`. This performance work does not
complete the outstanding OS4 feature inventory or prove the reporter's sole cause.

The native primer regression now also covers missing native contracts: 500
requests past the columns throttle produce one columns/back-region scan. Late
image/configuration and failed-install worker adoption remain available. The
production columns bootstrap passes 53 checks with the real NHK hook bank.

## Intermittent desktop touch investigation (2026-10-25)

User steering: investigate occasional desktop swipes not responding. A production
native scene-adapter reproduction confirmed a stale cancelled stream across a
source-bank readiness gap: the next DOWN was forwarded while unready, then its
MOVE/UP were swallowed when readiness returned, leaving the original receiver
pressed. The fix makes readiness gate new claims and continues sequence cleanup
while unready, clears the suppression range at the next DOWN, and retires stale
state if the event ABI is unavailable. Normal custom dispatch and stock paging
are retained. No Dart allocation, field offset or animation duration was changed.

The failing/fixed original-receiver traces are documented under
`tests/home-touch-lifecycle/README.md`. Eighteen production-adapter sequences
passed, as did 37 extra receiver checks, 82 region/state checks and 63 SystemUI
quick-switch lifecycle checks. Release assembled in 46 seconds and APK signature
verification passed. The updated APK has not been installed; phone
behavior and the third-party user's actual configuration remain unverified.
