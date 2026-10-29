# Builds the debug APK, installs it on the connected device/emulator and runs basic smoke checks.
# Usage:
#   .\scripts\android\run-smoke.ps1                 # build + install + launch + collect evidence
#   .\scripts\android\run-smoke.ps1 -SkipBuild
#   .\scripts\android\run-smoke.ps1 -Instrumented   # also run the UiAutomator test
param(
    [switch]$SkipBuild,
    [switch]$Instrumented
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\env.ps1"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
Push-Location $repoRoot
try {
    $devices = (& cmd /c "adb devices 2>nul") -join "`n"
    if ($devices -notmatch '(?m)\bdevice\s*$') {
        throw "No device/emulator connected. Run .\scripts\android\start-emulator.ps1 first.`n$devices"
    }

    if (-not $SkipBuild) {
        Write-Host '[gradle] :app:assembleDebug'
        & .\gradlew.bat :app:assembleDebug
        if ($LASTEXITCODE -ne 0) { throw "Gradle build failed (exit $LASTEXITCODE)" }
    }

    $apk = Join-Path $repoRoot 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path $apk)) { throw "APK not found: $apk" }

    Write-Host '[adb] device info'
    adb shell getprop ro.build.version.sdk
    adb shell getprop ro.product.model

    Write-Host '[adb] installing APK'
    adb install -r $apk
    adb shell am force-stop tv.zelta.app
    adb shell am start -W -n tv.zelta.app/.MainActivity
    Start-Sleep -Seconds 6

    Write-Host '[adb] focused window'
    adb shell dumpsys window | Select-String 'mCurrentFocus|mFocusedApp'

    $artifacts = Join-Path $repoRoot 'artifacts\android'
    New-Item -ItemType Directory -Force -Path $artifacts | Out-Null
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    adb shell screencap -p /sdcard/zelta-smoke.png
    adb pull /sdcard/zelta-smoke.png (Join-Path $artifacts "$stamp-smoke.png") | Out-Null
    adb shell rm /sdcard/zelta-smoke.png
    adb logcat -d -b crash > (Join-Path $artifacts "$stamp-crash.log")
    Write-Host "[artifacts] $artifacts"

    if ($Instrumented) {
        Write-Host '[gradle] connectedDebugAndroidTest'
        & .\gradlew.bat :app:connectedDebugAndroidTest
        if ($LASTEXITCODE -ne 0) { throw "Instrumented tests failed (exit $LASTEXITCODE)" }
    }

    Write-Host '[ok] smoke completed'
} finally {
    Pop-Location
}
