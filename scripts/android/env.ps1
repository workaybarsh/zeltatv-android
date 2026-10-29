# ZeltaTV Android — sets up the environment variables.
# Usage (dot-source):  . .\scripts\android\env.ps1
# Contains no absolute user paths; the SDK/JDK are located in the standard user directories.

$ErrorActionPreference = 'Stop'

$sdkRoot = $env:ANDROID_HOME
if ([string]::IsNullOrWhiteSpace($sdkRoot)) { $sdkRoot = $env:ANDROID_SDK_ROOT }
if ([string]::IsNullOrWhiteSpace($sdkRoot)) {
    $sdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
}
if (-not (Test-Path $sdkRoot)) {
    throw "Android SDK not found: $sdkRoot. Install: https://developer.android.com/studio#command-tools"
}

$jdkCandidates = @()
if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) { $jdkCandidates += $env:JAVA_HOME }
$managedJdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Jdk'
if (Test-Path $managedJdkRoot) {
    $jdkCandidates += Get-ChildItem $managedJdkRoot -Directory |
        Sort-Object Name -Descending |
        ForEach-Object { $_.FullName }
}
$jdkRoot = $jdkCandidates | Where-Object { Test-Path (Join-Path $_ 'bin\java.exe') } | Select-Object -First 1
if (-not $jdkRoot) {
    throw "JDK not found. Set JAVA_HOME or install a JDK (17+) under %LOCALAPPDATA%\Android\Jdk."
}

$env:JAVA_HOME = $jdkRoot
$env:ANDROID_HOME = $sdkRoot
$env:ANDROID_SDK_ROOT = $sdkRoot

$pathParts = @(
    (Join-Path $jdkRoot 'bin'),
    (Join-Path $sdkRoot 'platform-tools'),
    (Join-Path $sdkRoot 'emulator'),
    (Join-Path $sdkRoot 'cmdline-tools\latest\bin')
) | Where-Object { Test-Path $_ }

$env:PATH = ($pathParts -join ';') + ';' + $env:PATH

Write-Host "JAVA_HOME      = $env:JAVA_HOME"
Write-Host "ANDROID_HOME   = $env:ANDROID_HOME"
