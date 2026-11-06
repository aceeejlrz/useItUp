param(
    [string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$Serial = "emulator-5554"
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$reportDirectory = Join-Path $repoRoot 'app\build\reports\room-force-stop'
New-Item -ItemType Directory -Force -Path $reportDirectory | Out-Null
$appPackage = 'com.team15.useitup'
$runner = 'com.team15.useitup.test/androidx.test.runner.AndroidJUnitRunner'
$testClass = 'com.team15.useitup.data.ColdStartPersistenceTest'

function Invoke-Device([string[]]$Arguments) {
    $result = & $Adb -s $Serial @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $($Arguments -join ' ')`n$result" }
    return ($result -join "`n")
}

function Invoke-Phase([string]$Phase) {
    Invoke-Device @('shell', 'am', 'force-stop', $appPackage) | Out-Null
    $result = Invoke-Device @('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $testClass,
        '-e', 'persistencePhase', $Phase, $runner)
    Set-Content -LiteralPath (Join-Path $reportDirectory "$Phase.txt") -Value $result
    if ($result -notmatch 'OK \(1 test\)') { throw "Persistence phase $Phase failed:`n$result" }
    Write-Output "PASS: $Phase"
}

# Connected Gradle tests may uninstall the APKs on completion. Reinstall the built
# artifacts with -r (preserving existing data) so this check also works after that task.
foreach ($relativeApk in @('app\build\outputs\apk\debug\app-debug.apk',
        'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk')) {
    $apk = Join-Path $repoRoot $relativeApk
    if (-not (Test-Path -LiteralPath $apk)) { throw "Build the debug and androidTest APKs first: $apk" }
    Invoke-Device @('install', '-r', $apk) | Out-Null
}
Invoke-Device @('shell', 'pm', 'grant', $appPackage, 'android.permission.POST_NOTIFICATIONS') | Out-Null
$seeded = $false
try {
    Invoke-Phase 'seed'
    $seeded = $true
    Invoke-Device @('shell', 'am', 'force-stop', $appPackage) | Out-Null
    $appProcess = & $Adb -s $Serial shell pidof $appPackage
    if ($appProcess) { throw "The app process survived force-stop: $appProcess" }
    Write-Output 'PASS: app process is stopped'

    # Reopen the actual launcher Activity and verify the persisted pantry item is visible.
    $launch = Invoke-Device @('shell', 'am', 'start', '-W', '-n',
        "$appPackage/.ui.main.MainActivity")
    Set-Content -LiteralPath (Join-Path $reportDirectory 'launch.txt') -Value $launch
    # am start may return while the launch transition/accessibility tree is still settling.
    $screen = ''
    for ($attempt = 0; $attempt -lt 5; $attempt++) {
        Invoke-Device @('shell', 'uiautomator', 'dump', '/sdcard/useitup-persistence.xml') | Out-Null
        $screen = Invoke-Device @('shell', 'cat', '/sdcard/useitup-persistence.xml')
        if ($screen -match 'Room cold-start pears') { break }
        Start-Sleep -Milliseconds 250
    }
    Set-Content -LiteralPath (Join-Path $reportDirectory 'reopened-pantry.xml') -Value $screen
    if ($screen -notmatch 'Room cold-start pears') { throw 'Persisted pantry item missing from reopened UI' }
    Write-Output 'PASS: pantry item visible after force-stop and reopening'

    # Start a fresh process with no Activity, assert all three stores and the actual worker's
    # notification, then open the persisted recipe detail screen from the instrumentation test.
    Invoke-Device @('shell', 'am', 'force-stop', $appPackage) | Out-Null
    Invoke-Phase 'verify'
} finally {
    if ($seeded) { Invoke-Phase 'cleanup' }
}
Write-Output "Persistence evidence: $reportDirectory"
