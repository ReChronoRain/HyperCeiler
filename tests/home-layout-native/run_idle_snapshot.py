from pathlib import Path
import subprocess, sys, tempfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


workspace = Path(__file__).resolve().parents[2]
root = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else workspace
tests = workspace / "tests/home-layout-native"
jdk = test_paths.JAVA_HOME / 'bin'
endpoint = "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java"
legacy_tests = ["HomeLayoutNativeEndpointOS4Test", "HomeLayoutIdleSnapshotTest"]
sources = list((tests / "stubs").rglob("*.java")) + list((workspace / "tests/home-dock-window/stubs").rglob("*.java"))
sources += [root / endpoint, *(tests / (name + ".java") for name in legacy_tests)]
cases = [(name,) for name in legacy_tests]
if root == workspace:
    # Cadence tests use the demand-recovery API, which older source revisions lack.
    sources.append(tests / "HomeLayoutRefreshCadenceTest.java")
    cases += [
        ("HomeLayoutRefreshCadenceTest",),
        ("HomeLayoutRefreshCadenceTest", "provider"),
        ("HomeLayoutRefreshCadenceTest", "observer"),
        ("HomeLayoutRefreshCadenceTest", "notification"),
    ]

with tempfile.TemporaryDirectory(prefix="hc-idle-snapshot-") as out:
    helper = root / "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/provider/HomeLayoutPrefsSnapshot.java"
    if helper.exists(): sources.append(helper)
    gesture = root / "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/provider/HomeGesturePrefsSnapshot.java"
    if gesture.exists(): sources.append(gesture)
    gate = root / "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/base/FrameworkHookGate.java"
    if gate.exists(): sources.append(gate)
    result = subprocess.run([str(test_paths.java_tool('javac')), "-encoding", "UTF-8", "-d", out] + [str(s) for s in sources], capture_output=True, text=True)
    if result.returncode:
        print(result.stderr); sys.exit(result.returncode)
    for name, *args in cases:
        # Test classes and modes come only from the fixed cases above.
        result = subprocess.run(  # nosec B603
            [str(test_paths.java_tool('java')), "-ea", "-cp", out,
             "com.sevtinge.hyperceiler.libhook.rules.home.dock." + name, *args],
            capture_output=True, text=True,
        )
        print(result.stdout, end=""); print(result.stderr, end="")
        if result.returncode: sys.exit(result.returncode)
        if name == "HomeLayoutNativeEndpointOS4Test": print("Existing layout endpoint regression: PASS")
