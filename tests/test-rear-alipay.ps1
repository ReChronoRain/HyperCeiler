param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$javac = Join-Path $JavaHome 'bin/javac.exe'
$java = Join-Path $JavaHome 'bin/java.exe'
if (-not (Test-Path -LiteralPath $javac)) { throw 'Pass -JavaHome pointing to JDK 21 or newer' }
$output = Join-Path $root 'build/rear-alipay-host-tests'
New-Item -ItemType Directory -Path $output -Force | Out-Null
$policy = Join-Path $root 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/systemframework/input/RearAlipayGesturePolicy.java'
$test = Join-Path $PSScriptRoot 'RearAlipayGesturePolicyTest.java'
$nativeTest = Join-Path $PSScriptRoot 'RearAlipayNativeOptionTest.java'
& $javac @('-encoding','UTF-8','-d',$output,$policy,$test,$nativeTest)
if ($LASTEXITCODE -ne 0) { throw "javac failed: $LASTEXITCODE" }
& $java @('-cp',$output,'RearAlipayGesturePolicyTest')
if ($LASTEXITCODE -ne 0) { throw "Host tests failed: $LASTEXITCODE" }
& $java @('-cp',$output,'RearAlipayNativeOptionTest')
if ($LASTEXITCODE -ne 0) { throw "Native option tests failed: $LASTEXITCODE" }

$android = 'http://schemas.android.com/apk/res/android'
[xml]$screen = Get-Content -LiteralPath (Join-Path $root 'library/core/src/main/res/xml/security_core.xml') -Raw
[xml]$navigation = Get-Content -LiteralPath (Join-Path $root 'library/core/src/main/res/xml/framework.xml') -Raw
[xml]$headers = Get-Content -LiteralPath (Join-Path $root 'app/src/main/res/xml/settings_header.xml') -Raw
$app = 'http://schemas.android.com/apk/res-auto'
if ($screen.DocumentElement.GetAttribute('quick_restart', $app) -ne 'system') {
    throw 'Quick restart must target the system to apply both gesture scopes'
}
$switches = @($screen.SelectNodes('//SwitchPreference'))
if ($switches.Count -ne 2) { throw 'Each gesture feature needs its own switch' }
foreach ($key in @('prefs_key_securitycore_power_rear_code_enable', 'prefs_key_securitycore_back_tap_rear_alipay_enable')) {
    $matches = @($switches | Where-Object { $_.GetAttribute('key', $android) -eq $key })
    if ($matches.Count -ne 1 -or $matches[0].GetAttribute('defaultValue', $android) -ne 'false') {
        throw "Missing or enabled-by-default preference: $key"
    }
}
$links = @($headers.SelectNodes('//header') | Where-Object {
    $_.GetAttribute('fragment', $android) -eq 'com.sevtinge.hyperceiler.hooker.SecurityCoreFragment'
})
if ($links.Count -ne 1 -or $links[0].GetAttribute('summary', $android) -ne 'com.miui.securitycore') {
    throw 'SecurityCore must have its own app header'
}
if ($navigation.OuterXml -match 'RearAlipay|rear_alipay|SecurityCoreFragment') {
    throw 'The gesture page must no longer be nested under system framework'
}
$dropdowns = @($screen.SelectNodes('//fan.preference.DropDownPreference'))
if ($dropdowns.Count -ne 1 -or $dropdowns[0].GetAttribute('defaultValue', $android) -ne 'launch_alipay_payment_code') {
    throw 'Power code dropdown must default to payment'
}
[xml]$arrays = Get-Content -LiteralPath (Join-Path $root 'library/core/src/main/res/values/rear_alipay_gestures.xml') -Raw
$values = @($arrays.resources.'string-array' | Where-Object { $_.name -eq 'rear_alipay_power_code_values' })
if ($values.Count -ne 1 -or ($values[0].item -join ',') -ne 'launch_alipay_payment_code,launch_alipay_bus_code') {
    throw 'Power dropdown must contain native payment and transit actions'
}
$fragment = Get-Content -LiteralPath (Join-Path $root 'library/core/src/main/java/com/sevtinge/hyperceiler/hooker/SecurityCoreFragment.java') -Raw
if (-not $fragment.Contains('code.setVisible(power.isChecked())') -or -not $fragment.Contains('code.setVisible((boolean) value)')) {
    throw 'Power dropdown visibility must follow its switch at load and on change'
}
$framework = Get-Content -LiteralPath (Join-Path $root 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/app/SystemFramework/SystemFrameworkB.java') -Raw
if (-not $framework.Contains('initHook(new RearAlipayGestures(), RearAlipayGestures.isEnabled())')) {
    throw 'System hook must be conditionally registered'
}
$entry = Get-Content -LiteralPath (Join-Path $root 'library/libhook/src/main/resources/META-INF/xposed/java_init.list') -Raw
if ($entry.Trim() -ne 'com.sevtinge.hyperceiler.libhook.base.XposedInitEntry') { throw 'Unexpected module entry' }
$scope = Get-Content -LiteralPath (Join-Path $root 'library/libhook/src/main/resources/META-INF/xposed/scope.list')
if (@($scope | Where-Object { $_ -eq 'com.miui.securitycore' }).Count -ne 1 -or $scope -notcontains 'system') {
    throw 'Native option needs exactly one SecurityCore scope and the existing system scope'
}
$registration = Get-Content -LiteralPath (Join-Path $root 'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/app/SecurityCore.java') -Raw
if (-not $registration.Contains('@HookBase(targetPackage = "com.miui.securitycore", minSdk = 37)')) { throw 'Missing native UI hook registration' }
if (-not $registration.Contains('initHook(new RearAlipayBackTapOption(), PrefsBridge.getBoolean(RearAlipayGestures.BACK_PREF))')) {
    throw 'Native list hook must require the back-tap switch'
}
[xml]$nativeStrings = Get-Content -LiteralPath (Join-Path $root 'library/libhook/src/main/res/values-zh-rCN/rear_alipay_gestures.xml') -Raw
$paymentLabel = $nativeStrings.resources.string | Where-Object { $_.name -eq 'rear_alipay_back_native_entry' }
$transitLabel = $nativeStrings.resources.string | Where-Object { $_.name -eq 'rear_alipay_back_native_transit_entry' }
if ($paymentLabel.'#text' -ne '支付宝付款码(背屏)' -or $transitLabel.'#text' -ne '支付宝乘车码(背屏)') {
    throw 'Native option labels do not match the requested text'
}
foreach ($locale in @('values','values-zh-rCN')) {
    [xml]$strings = Get-Content -LiteralPath (Join-Path $root "library/core/src/main/res/$locale/rear_alipay_gestures.xml") -Raw
    foreach ($node in $screen.SelectNodes('//*')) {
        foreach ($attr in $node.Attributes) {
            if ($attr.Value.StartsWith('@string/rear_alipay_')) {
                $name = $attr.Value.Substring(8)
                if (@($strings.resources.string | Where-Object { $_.name -eq $name }).Count -ne 1) {
                    throw "Missing or duplicate localized resource: $locale/$name"
                }
            }
        }
    }
}
Write-Output 'PASS: UI keys, defaults, navigation, localized resources and existing Xposed entry'
