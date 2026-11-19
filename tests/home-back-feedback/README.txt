OS4 native back gesture feedback

HoldState in native code owns activation and cancellation. The Java state validates
session ownership and the final task snapshot; its old hold helpers are not the
activation timer for OS4.

The activation gate is 33% of the screen short side, in both orientations.
The short side is read through the launcher's imported Configuration getters,
without writing configuration or relying on a ROM-specific region-body layout.
Left/right classification still uses the current display width. Rotation cancels an
in-flight gesture instead of changing its threshold midway. Before reaching it, a
stationary finger cannot arm a switch. After reaching it, two position followers
with 1/4 and 1/2 gains, sampled at 17 ms, must both be within 20 px of the finger.
This follows the legacy launcher's stability check instead of adding a fixed
300 ms hold. A 4 dp retreat from the deepest point cancels, including overpull;
stationary retreat cannot rearm, and a new inward pull must reach the gate again.

Incoming icons and arrows grow over 100 ms using quadratic ease-out. The app icon
uses the original arrow center. Native background geometry is passed through
unchanged. Frame requests retain the generation, thread and Arc lifetime checks.

Behavioral references:
- The OS2 QuickBack hooks enable the launcher's ReadyState and callbacks:
  https://github.com/ReChronoRain/HyperCeiler/blob/os2/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/gesture/QuickBack.java
- The inspected launcher dump is RELEASE-4.39.14.7750-12111906.
  GestureStubView initializes mScreenWidth from the display short side; its
  handler contains the 33%, 17 ms and 20 px checks. updateAssistXPosition defines
  the two followers; GestureBackArrowView.changeScale defines the 100 ms icon
  transition. Background animation from that implementation is not ported.
  https://github.com/Little-W/miui-home-sourse/tree/c55b688eac734faac3d6870c6378ef127a2a10c4/smali_classes4/com/miui/home/recents

Run portable state and wire-format checks from the repository root:

    python3 tests/home-back-feedback/run.py

Add one or more --launcher-library /path/to/libapp_launcher.so arguments to run
native resolution and negative controls. OEM fixtures are not committed. The
runner writes build products to ~/.cache/hyperceiler-build/home-back-feedback,
or the directory selected by --build-dir.

Inspected native fixtures:
- RELEASE-8.01.02.7755-260904-10151930-R
- RELEASE-8.01.02.8602-260918-10261905-R

Resolution uses instruction relationships and imported APIs, not fixed addresses
or private symbol names. ArrowResolverTest covers the real image and eight
rejections for invalid drawing, view lifetime and configuration contracts.

Optional Android Parcel checks need android.jar, R8 and privileged app_process:

    python3 tests/home-back-feedback/run.py --device --android-jar /path/to/android.jar --r8-jar /path/to/r8.jar

The portable checks exercise preview serialization and packet validation without
reflecting into private handlers. Android checks exercise request peeking, rejection and the
live WMS endpoint's refusal of a non-launcher UID. They do not inject touch or
validate a successful launcher-authorized task switch. Live gesture verification
is a separate device check. Standard Gradle assembleDebug remains the full build
check; focused tests do not replace it.

TouchProbe is an optional privileged device probe. Compile against android.jar,
convert with D8, and run com.sevtinge.hyperceiler.libhook.rules.home.gesture.TouchProbe
with app_process and input-injection permission. It uses
1200x2608 coordinates and actively operates the screen. Arguments are:

  side(left/right) holdMs finish(release/retract/rearm/cancel) stepMs distancePx retreatPx

Retraction waits 450 ms before release to check that a stationary finger does not
rearm. The rearm case then pulls inward and holds again. Inspect the
HyperCeiler.BackArrow and HyperCeiler.BackFeedback logcat tags: full pull should
produce ready/shown/confirmed, retreat should produce cancelled, and only an
accepted release should produce commit. Closing a source activity with ordinary
back can expose the previous app, so foreground package alone is not a reliable
assertion of a quick switch.

The preview transport uses event 13 of the existing authenticated WMS Binder
endpoint (transaction 0x00484351). A bounded byte-array request/reply carries the
same gesture packet and icon snapshot. The framework validates the launcher UID,
process name, timestamp, interface and synchronous call before dispatch. No Unix
socket, additional SELinux rule or companion policy module is used.

Both the native launcher code and the system-server module must load the new
build. Updating an already-running system-server hook requires a device restart.
An older in-memory endpoint rejects the new event instead of accepting a switch.
