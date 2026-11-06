# Creates the three test AVDs: Android TV 1080p (API 34), phone and tablet (API 35).
# Usage:
#   .\scripts\android\create-avds.ps1
#   .\scripts\android\create-avds.ps1 -Force     # recreate existing AVDs
param([switch]$Force)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\env.ps1"

$avdmanager = Join-Path $env:ANDROID_HOME 'cmdline-tools\latest\bin\avdmanager.bat'
$sdkmanager = Join-Path $env:ANDROID_HOME 'cmdline-tools\latest\bin\sdkmanager.bat'
if (-not (Test-Path $avdmanager)) { throw "avdmanager not found: $avdmanager" }

# There is no android-tv system image for API 35; the newest x86 TV image is API 34.
# API 36 android-tv;x86_64 exists — it is tracked as an upgrade item on the roadmap.
$profiles = @(
    @{ Name = 'ZeltaTV_TV_1080p'; Image = 'system-images;android-34;android-tv;x86';     Device = @('tv_1080p', 'tv_720p'); Tag = 'tv' },
    @{ Name = 'ZeltaTV_Phone';    Image = 'system-images;android-35;google_apis;x86_64';  Device = @('pixel_7', 'pixel_6', 'pixel_5', 'pixel'); Tag = 'phone' },
    @{ Name = 'ZeltaTV_Tablet';   Image = 'system-images;android-35;google_apis;x86_64';  Device = @('pixel_tablet', 'pixel_c', 'Nexus 10'); Tag = 'tablet' }
)

Write-Host '=== installed packages ==='
& $sdkmanager --list_installed 2>&1 | Select-String -Pattern 'system-images|platform-tools|emulator|platforms;' | ForEach-Object { $_.Line }

$deviceList = (& $avdmanager list device 2>&1) -join "`n"
$installedAvds = (& $avdmanager list avd 2>&1) -join "`n"

foreach ($profile in $profiles) {
    $deviceId = $profile.Device | Where-Object { $deviceList -match ('"' + [regex]::Escape($_) + '"') } | Select-Object -First 1
    if (-not $deviceId) {
        Write-Warning "$($profile.Name): no matching device profile found; the default profile will be used."
    }

    $exists = $installedAvds -match ('Name:\s*' + [regex]::Escape($profile.Name) + '\b')
    if ($exists -and -not $Force) {
        Write-Host "[skip] $($profile.Name) already exists (use -Force to recreate)."
        continue
    }

    Write-Host "[create] $($profile.Name) <- $($profile.Image) / $deviceId"
    # 'no' = answer no to the custom hardware profile prompt
    'no' | & $avdmanager create avd --name $profile.Name --package $profile.Image --device $deviceId --force

    $configPath = Join-Path $env:USERPROFILE ".android\avd\$($profile.Name).avd\config.ini"
    if (Test-Path $configPath) {
        $extra = switch ($profile.Tag) {
            'tv'    { @('hw.dpad=yes', 'hw.keyboard=yes', 'hw.mainKeys=no', 'hw.ramSize=2048') }
            'phone' { @('hw.dpad=yes', 'hw.keyboard=no', 'hw.ramSize=3072') }
            'tablet'{ @('hw.dpad=yes', 'hw.keyboard=no', 'hw.ramSize=4096') }
        }
        Add-Content -Path $configPath -Value '' -Encoding ascii
        Add-Content -Path $configPath -Value '# --- ZeltaTV test settings ---' -Encoding ascii
        $extra | ForEach-Object { Add-Content -Path $configPath -Value $_ -Encoding ascii }
        Write-Host "        config: $configPath"
    }
}

Write-Host ''
Write-Host '=== AVD list ==='
& (Join-Path $env:ANDROID_HOME 'emulator\emulator.exe') -list-avds
