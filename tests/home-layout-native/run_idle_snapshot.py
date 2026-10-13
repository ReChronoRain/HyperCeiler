from pathlib import Path
import subprocess, sys, tempfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hyperceiler_test_paths as test_paths


workspace = Path(__file__).resolve().parents[2]
root = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else workspace
tests = workspace / "tests/home-layout-native"
jdk = test_paths.JAVA_HOME / 'bin'
endpoint = "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java"
with tempfile.TemporaryDirectory(prefix="hc-idle-snapshot-") as out:
    # Always use the enhanced fault stubs with the requested production source revision.
    sources = list((tests / "stubs").rglob("*.java")) + list((workspace / "tests/home-dock-window/stubs").rglob("*.java")) + [tests / "HomeLayoutIdleSnapshotTest.java", tests / "HomeLayoutNativeEndpointOS4Test.java", root / endpoint]
    helper = root / "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/provider/HomeLayoutPrefsSnapshot.java"
    if helper.exists(): sources.append(helper)
    gate = root / "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/base/FrameworkHookGate.java"
    if gate.exists(): sources.append(gate)
    result = subprocess.run([str(test_paths.java_tool('javac')), "-encoding", "UTF-8", "-d", out] + [str(s) for s in sources], capture_output=True, text=True)
    if result.returncode:
        print(result.stderr); sys.exit(result.returncode)
    for name in ["HomeLayoutNativeEndpointOS4Test", "HomeLayoutIdleSnapshotTest"]:
        result = subprocess.run([str(test_paths.java_tool('java')), "-ea", "-cp", out, "com.sevtinge.hyperceiler.libhook.rules.home.dock." + name], capture_output=True, text=True)
        print(result.stdout, end=""); print(result.stderr, end="")
        if result.returncode: sys.exit(result.returncode)
        if name == "HomeLayoutNativeEndpointOS4Test": print("Existing layout endpoint regression: PASS")
