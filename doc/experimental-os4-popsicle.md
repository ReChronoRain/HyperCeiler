# Experimental HyperOS 4 adaptation: popsicle

Target: Xiaomi 17 Pro Max (`popsicle`), `OS4.0.0.44.XPBCNXM`, Android 17 / API 37.
Base: HyperCeiler r4669, `5e4686069dd7ab1f3697e256d5fc7d68fb73e317`.

This draft proposes target-specific compatibility changes for upstream review.
The scope below counts modified feature groups: related switches and parameters,
and the two media-card locations, are grouped together. It does not represent
complete HyperOS 4 coverage.

## Modified feature groups

| Area | Count | Changes |
| --- | ---: | --- |
| Home / recents | 14 | App open/close animation duration; recents animation duration; hotseat capacity; folder columns; folder row spacing; hotseat bottom margin; icon scale; indicator bottom margin; hide clear button; disable clear button; workspace bottom, horizontal and top margins; page indicator policy. |
| Media cards | 4 | Title/artist/time sizes; ambient light and color extraction; custom backgrounds with seekbar colors; always-dark mode. Notification-center and island settings remain independent. |
| Status / notifications / control center | 9 | Expanded heads-up duration; notification-channel settings redirect; notification count limit; carrier text; Wi-Fi standard; data-saver icon; speaker-group stereo preference key; Wi-Fi/mobile slot order; network-speed refresh interval. |
| Lockscreen | 3 | PIN randomization; local Zen notification visibility; the stock dismissible-keyguard swipe hint. |
| Gallery creation provider | 4 | ID photo, magic matting, video effects, and vlog/video editor availability replies. |
| Weather | 3 | Device/animation level; material capability; card light/dark mode. |
| Other | 3 | Never-sleep option; GetApps device identity; barrage text length. |
| Total | **40** | Modified feature groups. |

Preference initialization, framework-status publication, coherent native
snapshots, expiry and native lifetime guards are supporting infrastructure and
are not counted as additional user features.

## Architecture and limits

- Java/Kotlin hooks target the reviewed OS4 shapes and preserve existing routes
  where applicable. Shape matching alone is not a runtime DEX identity proof.
  The shared ambient drawable also changes, including behavior used by older
  HyperOS versions where their original hook routes remain present.
- Home and Weather use a **separate experimental native companion** with Zygisk
  Next API 4 / Hyos Runtime API 1. Building or installing the APK alone does not
  deliver these native paths. The APK's system scope supplies their configuration.
- Native paths depend on exact image/build IDs, instruction/import guards,
  startup ordering and expiring coherent snapshots. Mismatches are declined.
  The companion installer is restricted to the stated device/ROM.
- Phone, Pad and Fold custom grids remain declined. The unmerged grid experiments
  are excluded. The old `RecentsAnimationRust` snapshot path remains declined;
  the independent pre-entry recents path covers 30–200% and requires a cold
  launcher restart after changing its value or authorization.
- Revocation closes future overrides and attempts owned code restoration; it
  cannot undo already cached objects or in-flight results. Once Home startup
  gateways have been activated, failure cleanup retains their hooks and original
  continuations until process exit. Closing the gate is not proof of quiescence.
  Backend publication must preserve original continuations while callbacks are in flight.
- The unlock-hint change only covers the default-theme stock type-13
  `dismissible_keyguard_swipe` message; it preserves authentication/fingerprint
  messages and does not cover every bottom hint or custom theme.
- Gallery changes only existing provider availability replies for Gallery calls.
  They do not supply missing algorithms, downloads or other capability gates.
- Notification count changes affect the reviewed SystemUI dismissal callback,
  not system-server posting quotas. Weather card changes are limited to reviewed
  card consumers, not every widget/theme. GetApps changes selected identity fields,
  not every hardware identifier or server-side restriction.
- SystemUI settings may require a SystemUI restart; Home/Weather startup changes
  require cold process restarts. Live library unloading is unsupported.

## Source provenance and packaging

The contribution includes 117 production files, companion build/installer sources,
and focused regressions. PR preparation additionally fixes the Home startup
post-activation cleanup lifetime issue.

Native generated guard tables contain target-specific byte signatures and
instruction fragments derived from the supplied popsicle full OTA. These are
used for exact matching and bounded patch plans. They are not claimed to be
newly authored OEM code. Maintainers should review this representation and
provenance before merging or choose a different guard representation.
No full OEM APK, ELF library, OTA archive, original DEX probe, signing key,
credential or prebuilt module is included in this contribution.

This contribution was prepared with AI assistance and is organized as a draft
for maintainer review.

## Builds

Use the project's usual `assembleDebug` process for the APK. Upstream settings
require GitHub Packages credentials (`GIT_ACTOR` / `GIT_TOKEN`); keep those outside
source control. The PR job supplies its automatic repository token with only
`contents: read` and `packages: read` permissions. Fork contributions stay on the
`pull_request` event without signing or publication secrets. Package access must
be granted to the repository by the package owner.

The stereo preference harness uses `defusedxml` to reject unsafe XML constructs.
Install its Python dependency with `python3 -m pip install -r tests/requirements.txt`.

For the optional companion, obtain the external Zygisk Next API header from
commit `22fe320814656e7b44715b5f32c967ceb51cda08`. Its SHA-256 must be
`957922aeec1bb31091cc37dca29267b7fa41e4e109f679af1526ee4c5e4325c8`.
The SDK is not bundled. The reviewed toolchain is NDK 28.2.13676358 and CMake
3.22.1. Supply actual absolute paths and fresh build/staging/report locations:

```sh
python3 tools/build_hyos_companion.py \
  --source-dir . --build-dir /tmp/hc-hyos-build \
  --sdk /path/to/zygisk-next-sdk --ndk /path/to/android-ndk \
  --cmake /path/to/cmake --ninja /path/to/ninja \
  --stage /tmp/hc-hyos-stage --label test15 \
  --report /tmp/hc-hyos-build-report.json
```

The script builds and stages the library, installer metadata and required
notices. It never installs anything. `--label` is packaging metadata, not a
statement that the result matches a previously built test APK; record the exact
Git commit used. Default NDK host tag is `linux-x86_64`; other hosts can supply
`--ndk-host-tag`. The stage can be archived with its contents at the ZIP root
after review.

## Related contributions

Upstream PRs #1702 and #1720 cover related compatibility areas. No code from
those PRs is incorporated here. Some hook changes overlap and should be
coordinated during review. The generated guard representation and companion
integration are also separate review points.
