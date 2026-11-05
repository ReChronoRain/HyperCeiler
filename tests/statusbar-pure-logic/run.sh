#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -d "$OUT" \
  "$ROOT/tests/statusbar-pure-logic/BarFollowPureLogicTest.java" \
  "$ROOT/tests/statusbar-pure-logic/BootFuseControllerTest.java" \
  "$ROOT/tests/statusbar-pure-logic/ServerBarLifecycleTest.java" \
  "$ROOT/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/statusbarfollow/BootFuseController.java" \
  "$ROOT/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/barfollow/AppBarFeatureState.java" \
  "$ROOT/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/barfollow/AppBarModeSession.java" \
  "$ROOT/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/barfollow/BarFolmeCommands.java" \
  "$ROOT/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/barfollow/RecentsBarCallbackSession.java" \
  "$ROOT/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/barfollow/RecentsBarReuseSession.java" \
  "$ROOT/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/barfollow/StatusBarAppearanceInputs.java" \
  "$ROOT/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/barfollow/XiaomiShadeBarSelection.java" \
  "$ROOT/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemui/statusbarfollow/RecentsOpenSession.java" \
  "$ROOT/library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemframework/statusbarfollow/ServerBarFollowLifecycle.java"
java -cp "$OUT" com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.BarFollowPureLogicTest
java -cp "$OUT" com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow.BootFuseControllerTest
java -cp "$OUT" com.sevtinge.hyperceiler.libhook.rules.systemframework.statusbarfollow.ServerBarLifecycleTest
