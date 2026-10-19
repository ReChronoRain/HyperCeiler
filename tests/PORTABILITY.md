# Running the regression scripts from another checkout

Run commands from the repository root. Python scripts resolve production sources
from their own checkout, so the checkout can have any directory name and the
caller's current directory does not select a different source tree.

`hyperceiler_test_paths.py` centralizes tool and external-input locations. Importing
it does not download tools, create directories, or start Android services.

| Environment variable | Default when unset |
| --- | --- |
| `HYPERCEILER_BUILD_CACHE` | `~/.cache/hyperceiler-build` |
| `JAVA_HOME` | `<build cache>/jdk-25.0.4.1` |
| `ANDROID_HOME` | `ANDROID_SDK_ROOT`, then `<build cache>/android-sdk` |
| `ANDROID_NDK_HOME` | `<Android SDK>/ndk/28.2.13676358` |
| `HYPERCEILER_TEST_DATA` | Parent directory of the current checkout |
| `KOTLIN_HOME` | `<test data>/tools-cache/kotlin-2.4.10/kotlinc` |
| `HYPERCEILER_ROM_ROOT` | `<test data>/firmware` |
| `HYPERCEILER_EXTRACTED_ROM_ROOT` | `<ROM root>/extracted` |
| `HYPERCEILER_SNAPSHOT_ROOT` | Build cache |

Use absolute paths for environment overrides. `~` is expanded. Existing CLI
options remain available: `--build-cache` selects that runner's cache, while an
explicit `JAVA_HOME`, `ANDROID_HOME`, or `HYPERCEILER_SNAPSHOT_ROOT` still selects
the corresponding tool or input. `native-remaining-audit/audit.py --project`
selects its external data/output root; `HYPERCEILER_ROM_ROOT` can independently
select its ROM inputs.

For example, with tools and inputs already installed:

```sh
export HYPERCEILER_BUILD_CACHE="$HOME/.cache/hyperceiler-build"
export JAVA_HOME="/opt/jdk-25"
export KOTLIN_HOME="/opt/kotlinc"
export ANDROID_HOME="/opt/android-sdk"
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/28.2.13676358"
export HYPERCEILER_TEST_DATA="/data/hyperceiler-inputs"

python3 tests/os4-pin-layout/run.py --output /tmp/hyperceiler-pin-results
python3 tests/os4-custom-background-renderer/run.py --output /tmp/hyperceiler-renderer-results
```

Choose an unused output directory where a runner requires a new directory.
Install Python dependencies from `tests/requirements.txt` for scripts using them.
The cache-based Java runners expect `changed-hooks-compile/api-102.0.0.jar`,
`hook-xposed-102-1.3.0.jar`, and, for some runners, `core-1.3.0.jar`. Some runners
also use the Kotlin 2.4.10 JARs in the supplied Gradle cache. Android compilation
scripts retain their API 37 (`platforms/android-37.0`), build-tools 37.0.0, and NDK
version expectations. Setting a path override does not remove those requirements.

## External inputs are separate from the repository

The repository includes the checked-in fixtures. It does not contain Xiaomi ROM
images, extracted APKs/libraries, earlier generated reports, historical compiled
peer classes, or all build tools. Specialized ROM, native-image, and report
aggregation scripts need the matching inputs, for example:

```text
<test data>/
  acceptance/                  Earlier extraction and regression outputs
  tools/                       Referenced extraction helpers
  emulation/                   Probe build inputs, including test-only keys
<ROM root>/
  components/                  Extracted system packages and libraries
  launcher-native/lib/arm64-v8a/
  indexes/
  component-inventory.json
<extracted ROM root>/
  system_ext/lib64/             Original vendor libraries
  system/system/lib64/
<snapshot root>/
  source-os4-hyos-test*/        Exact named source/build snapshots
<build cache>/
  changed-hooks-compile/
  gradle-home/
  arm64-emulation/root/        Linux ARM64 toolchain and QEMU used by those runners
```

The exact required subpaths are declared by each runner. A missing file or failed
input hash check means that input must be supplied or regenerated; it is not a
passed check. Point the appropriate environment variable at the matching input
tree instead of editing paths in the scripts. Historical report files can embed
paths from their original run and bind source hashes; regenerate those reports
for the new checkout before aggregating them. Do not replace their hashes just
to make a report pass.

`home-startup-snapshot/compile_actual.py` additionally requires `--classpath`
with the current machine's SDK, project-peer, and Xposed dependency classpath. It
no longer copies an absolute classpath out of an earlier machine's report.

The ARM64 toolchain and native runners currently require Linux or WSL. Probe
scripts retain their explicit emulator/AVD checks and are separate commands;
ordinary path configuration does not run them. A portable path setup alone does
not supply the missing ROM, toolchain, historical artifacts, or Android runtime.
