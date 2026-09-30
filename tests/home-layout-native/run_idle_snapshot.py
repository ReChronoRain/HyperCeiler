from pathlib import Path
import subprocess, sys, tempfile

workspace = Path(__file__).resolve().parents[2]
root = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else workspace
tests = workspace / "tests/home-layout-native"
jdk = Path("D:/build-tools/jdk-25.0.4.1+1/bin")
endpoint = "library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/dock/HomeLayoutNativeEndpointOS4.java"
with tempfile.TemporaryDirectory(prefix="hc-idle-snapshot-") as out:
    # Always use the enhanced fault stubs with the requested production source revision.
    sources = list((tests / "stubs").rglob("*.java")) + list((workspace / "tests/home-dock-window/stubs").rglob("*.java")) + [tests / "HomeLayoutIdleSnapshotTest.java", tests / "HomeLayoutNativeEndpointOS4Test.java", root / endpoint]
    result = subprocess.run([str(jdk / "javac.exe"), "-encoding", "UTF-8", "-d", out] + [str(s) for s in sources], capture_output=True, text=True)
    if result.returncode:
        print(result.stderr); sys.exit(result.returncode)
    for name in ["HomeLayoutNativeEndpointOS4Test", "HomeLayoutIdleSnapshotTest"]:
        result = subprocess.run([str(jdk / "java.exe"), "-ea", "-cp", out, "com.sevtinge.hyperceiler.libhook.rules.home.dock." + name], capture_output=True, text=True)
        print(result.stdout, end=""); print(result.stderr, end="")
        if result.returncode: sys.exit(result.returncode)
        if name == "HomeLayoutNativeEndpointOS4Test": print("Existing layout endpoint regression: PASS")
