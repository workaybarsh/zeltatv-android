# Starts a ZeltaTV test AVD and waits until boot completes.
# Usage:
#   .\scripts\android\start-emulator.ps1 -Profile tv
#   .\scripts\android\start-emulator.ps1 -Profile phone -NoWindow -ColdBoot
param(
    [ValidateSet('tv', 'phone', 'tablet')]
    [string]$Profile = 'tv',
    [switch]$NoWindow,
    [switch]$ColdBoot,
    [int]$TimeoutSeconds = 300
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\env.ps1"

$avdName = switch ($Profile) {
    'tv'    { 'ZeltaTV_TV_1080p' }
    'phone' { 'ZeltaTV_Phone' }
    'tablet'{ 'ZeltaTV_Tablet' }
}

$emulator = Join-Path $env:ANDROID_HOME 'emulator\emulator.exe'
$available = & $emulator -list-avds
if ($available -notcontains $avdName) {
    throw "AVD not found: $avdName. Run .\scripts\android\create-avds.ps1 first."
}

$arguments = @('-avd', $avdName, '-gpu', 'auto', '-no-boot-anim', '-no-snapshot-save')
if ($NoWindow) { $arguments += '-no-window' }
if ($ColdBoot) { $arguments += '-no-snapshot-load' }

Write-Host "[emulator] starting $avdName..."
Start-Process -FilePath $emulator -ArgumentList $arguments | Out-Null

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$booted = ''
do {
    Start-Sleep -Seconds 3
    $devices = (& cmd /c "adb devices 2>nul") -join "`n"
    if ($devices -match '(?m)\bdevice\s*$') {
        $booted = (& cmd /c "adb -e shell getprop sys.boot_completed 2>nul" | Select-Object -First 1)
    }
} while ($booted -ne '1' -and (Get-Date) -lt $deadline)

if ($booted -ne '1') {
    throw "The emulator did not boot within $TimeoutSeconds s. Log: %LOCALAPPDATA%\Temp\AndroidEmulator\*.log"
}

Write-Host '[adb] device ready:'
adb devices
Write-Host ''
Write-Host "Tip: on a TV without touch input, use 'adb shell input keyevent 20' (DOWN) for the D-pad."
