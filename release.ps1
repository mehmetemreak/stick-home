# Builds a signed release APK as .\stick-home.apk
# Needs JAVA_HOME pointing at a JDK 17. Keystore defaults to ..\stick-home-release.jks
# (override with $env:STICK_KEYSTORE). The password is asked here and never written to disk.
$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) { throw "JAVA_HOME ayarlı değil (JDK 17 gerekli)." }

$secure = Read-Host "Keystore şifresi" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $env:STICK_KEYSTORE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    & "$PSScriptRoot\gradlew.bat" -p "$PSScriptRoot" assembleRelease
    if ($LASTEXITCODE -ne 0) { throw "Derleme başarısız." }
    Copy-Item "$PSScriptRoot\app\build\outputs\apk\release\app-release.apk" "$PSScriptRoot\stick-home.apk" -Force
    Write-Host "Hazır: $PSScriptRoot\stick-home.apk"
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    $env:STICK_KEYSTORE_PASSWORD = $null
}
