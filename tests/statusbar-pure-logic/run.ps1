# SPDX-License-Identifier: AGPL-3.0-or-later
$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$out = Join-Path ([IO.Path]::GetTempPath()) ("statusbar-pure-logic-" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $out | Out-Null
try {
    $sources = @(
        (Join-Path $PSScriptRoot "BarFollowPureLogicTest.java"),
        (Join-Path $PSScriptRoot "BootFuseControllerTest.java"),
        (Join-Path $PSScriptRoot "ServerBarLifecycleTest.java"),
        (Join-Path $root "library\libhook\src\main\java\com\sevtinge\hyperceiler\libhook\rules\systemui\statusbarfollow\BootFuseController.java"),
        (Join-Path $root "library\libhook\src\main\java\com\sevtinge\hyperceiler\libhook\rules\systemui\barfollow\AppBarFeatureState.java"),
        (Join-Path $root "library\libhook\src\main\java\com\sevtinge\hyperceiler\libhook\rules\systemui\barfollow\AppBarModeSession.java"),
        (Join-Path $root "library\libhook\src\main\java\com\sevtinge\hyperceiler\libhook\rules\systemui\barfollow\BarFolmeCommands.java"),
        (Join-Path $root "library\libhook\src\main\java\com\sevtinge\hyperceiler\libhook\rules\systemui\barfollow\RecentsBarCallbackSession.java"),
        (Join-Path $root "library\libhook\src\main\java\com\sevtinge\hyperceiler\libhook\rules\systemui\barfollow\RecentsBarReuseSession.java"),
        (Join-Path $root "library\libhook\src\main\java\com\sevtinge\hyperceiler\libhook\rules\systemui\barfollow\StatusBarAppearanceInputs.java"),
        (Join-Path $root "library\libhook\src\main\java\com\sevtinge\hyperceiler\libhook\rules\systemui\barfollow\XiaomiShadeBarSelection.java"),
        (Join-Path $root "library\libhook\src\main\java\com\sevtinge\hyperceiler\libhook\rules\systemui\statusbarfollow\RecentsOpenSession.java"),
        (Join-Path $root "library\libhook\src\main\java\com\sevtinge\hyperceiler\libhook\rules\systemframework\statusbarfollow\ServerBarFollowLifecycle.java")
    )
    & javac -d $out @sources
    if ($LASTEXITCODE -ne 0) { throw "javac failed" }
    & java -cp $out com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.BarFollowPureLogicTest
    if ($LASTEXITCODE -ne 0) { throw "BarFollowPureLogicTest failed" }
    & java -cp $out com.sevtinge.hyperceiler.libhook.rules.systemui.statusbarfollow.BootFuseControllerTest
    if ($LASTEXITCODE -ne 0) { throw "BootFuseControllerTest failed" }
    & java -cp $out com.sevtinge.hyperceiler.libhook.rules.systemframework.statusbarfollow.ServerBarLifecycleTest
    if ($LASTEXITCODE -ne 0) { throw "ServerBarLifecycleTest failed" }
}
finally {
    Remove-Item -LiteralPath $out -Recurse -Force
}
