# Debug APK build helper (uses the same commands as CI).
# Usage:
#   .\scripts\android\build-debug.ps1
#   .\scripts\android\build-debug.ps1 -Test
param([switch]$Test)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\env.ps1"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
Push-Location $repoRoot
try {
    if ($Test) {
        Write-Host '[gradle] :core:playlist:test'
        & .\gradlew.bat :core:playlist:test
        if ($LASTEXITCODE -ne 0) { throw "Unit tests failed (exit $LASTEXITCODE)" }
    }

    Write-Host '[gradle] :app:assembleDebug'
    & .\gradlew.bat :app:assembleDebug
    if ($LASTEXITCODE -ne 0) { throw "Build failed (exit $LASTEXITCODE)" }

    $apk = Join-Path $repoRoot 'app\build\outputs\apk\debug\app-debug.apk'
    Get-Item $apk | Select-Object FullName, Length, LastWriteTime
} finally {
    Pop-Location
}
