[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repoRoot = Split-Path $PSScriptRoot -Parent
$signingDirectory = Join-Path $repoRoot '.local\signing'
$keyPath = Join-Path $signingDirectory 'nudge-release.jks'
$propertiesPath = Join-Path $signingDirectory 'keystore.properties'
if ((Test-Path -LiteralPath $keyPath) -or (Test-Path -LiteralPath $propertiesPath)) {
    throw 'Signing material already exists. Restore the original key rather than replacing it; updates must use the same key.'
}
$keytool = Get-Command keytool -ErrorAction Stop
New-Item -ItemType Directory -Force -Path $signingDirectory | Out-Null
$random = [Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$random.GetBytes($bytes)
$password = [Convert]::ToBase64String($bytes)
$previousStore = [Environment]::GetEnvironmentVariable('NUDGE_STORE_PASSWORD', 'Process')
$previousKey = [Environment]::GetEnvironmentVariable('NUDGE_KEY_PASSWORD', 'Process')
try {
    $env:NUDGE_STORE_PASSWORD = $password
    $env:NUDGE_KEY_PASSWORD = $password
    & $keytool.Source -genkeypair -keystore $keyPath -storetype JKS -alias nudge-release -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Nudge Personal Release' -storepass:env NUDGE_STORE_PASSWORD -keypass:env NUDGE_KEY_PASSWORD -noprompt
    if ($LASTEXITCODE -ne 0) { throw 'Key generation failed. Inspect .local/signing before retrying.' }
    @(
        'storeFile=.local/signing/nudge-release.jks'
        "storePassword=$password"
        'keyAlias=nudge-release'
        "keyPassword=$password"
    ) | Set-Content -LiteralPath $propertiesPath -Encoding ASCII
    Write-Output 'Created the Nudge release key in .local/signing (excluded from Git).'
    Write-Output 'Back up the entire signing directory securely before distributing your first APK. Keep both the key and its properties file.'
} finally {
    [Environment]::SetEnvironmentVariable('NUDGE_STORE_PASSWORD', $previousStore, 'Process')
    [Environment]::SetEnvironmentVariable('NUDGE_KEY_PASSWORD', $previousKey, 'Process')
    $random.Dispose()
    $password = $null
}
