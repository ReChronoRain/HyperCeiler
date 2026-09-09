param([Parameter(Mandatory)][string]$Apk)
$ErrorActionPreference = 'Stop'
$apkPath = (Resolve-Path -LiteralPath $Apk).Path
$descriptors = [System.Collections.Generic.HashSet[string]]::new()
$zip = [System.IO.Compression.ZipFile]::OpenRead($apkPath)
try {
    $dexEntries = @($zip.Entries | Where-Object { $_.FullName -match '^classes[0-9]*\.dex$' })
    if ($dexEntries.Count -eq 0) { throw 'No DEX files in APK' }
    foreach ($entry in $dexEntries) {
        $stream = $entry.Open()
        $memory = [System.IO.MemoryStream]::new()
        try { $stream.CopyTo($memory); $bytes = $memory.ToArray() }
        finally { $stream.Dispose(); $memory.Dispose() }
        if ([System.Text.Encoding]::ASCII.GetString($bytes, 0, 4) -ne "dex`n") { throw 'Unexpected DEX header' }
        $stringIds = [BitConverter]::ToUInt32($bytes, 60)
        $typeIds = [BitConverter]::ToUInt32($bytes, 68)
        $classCount = [BitConverter]::ToUInt32($bytes, 96)
        $classDefs = [BitConverter]::ToUInt32($bytes, 100)
        for ($i = 0; $i -lt $classCount; $i++) {
            $type = [BitConverter]::ToUInt32($bytes, $classDefs + 32 * $i)
            $string = [BitConverter]::ToUInt32($bytes, $typeIds + 4 * $type)
            $offset = [BitConverter]::ToUInt32($bytes, $stringIds + 4 * $string)
            do { $value = $bytes[$offset]; $offset++ } while (($value -band 128) -ne 0)
            $end = $offset
            while ($bytes[$end] -ne 0) { $end++ }
            [void]$descriptors.Add([System.Text.Encoding]::UTF8.GetString($bytes, $offset, $end - $offset))
        }
    }
    foreach ($required in @(
        'Lcom/sevtinge/hyperceiler/libhook/rules/systemframework/input/RearAlipayGestures;',
        'Lcom/sevtinge/hyperceiler/libhook/rules/systemframework/input/RearAlipayGesturePolicy;',
        'Lcom/sevtinge/hyperceiler/hooker/SecurityCoreFragment;',
        'Lcom/sevtinge/hyperceiler/libhook/app/SecurityCore;',
        'Lcom/sevtinge/hyperceiler/libhook/rules/securitycore/RearAlipayBackTapOption;'
    )) {
        if (-not $descriptors.Contains($required)) { throw "Missing implementation: $required" }
    }
    if (@($descriptors | Where-Object { $_.StartsWith('Lio/github/libxposed/api/') }).Count -ne 0) {
        throw 'libxposed API was bundled into the APK'
    }
    $entry = $zip.GetEntry('META-INF/xposed/java_init.list')
    if ($null -eq $entry) { throw 'Missing Xposed module entry' }
    $reader = [System.IO.StreamReader]::new($entry.Open())
    try { $entryText = $reader.ReadToEnd().Trim() } finally { $reader.Dispose() }
    if ($entryText -ne 'com.sevtinge.hyperceiler.libhook.base.XposedInitEntry') { throw 'Unexpected entry class' }
    $scopeEntry = $zip.GetEntry('META-INF/xposed/scope.list')
    if ($null -eq $scopeEntry) { throw 'Missing recommended scope' }
    $reader = [System.IO.StreamReader]::new($scopeEntry.Open())
    try { $scope = $reader.ReadToEnd() -split '\r?\n' } finally { $reader.Dispose() }
    if (@($scope | Where-Object { $_ -eq 'com.miui.securitycore' }).Count -ne 1 -or $scope -notcontains 'system') {
        throw 'Missing or duplicated SecurityCore scope in APK'
    }
    if ($null -eq $zip.GetEntry('res/xml/security_core.xml')) { throw 'SecurityCore settings page not packaged' }
    if ($null -ne $zip.GetEntry('res/xml/framework_rear_alipay_gestures.xml')) { throw 'Obsolete framework page still packaged' }
    [ordered]@{
        apk = $apkPath
        sha256 = (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash
        dexFiles = $dexEntries.Count
        classCount = $descriptors.Count
        implementationPresent = $true
        settingsPagePresent = $true
        securityCoreScopePresent = $true
        libxposedApiBundled = $false
        xposedEntry = $entryText
    } | ConvertTo-Json
} finally {
    $zip.Dispose()
}
