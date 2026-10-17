[CmdletBinding()]
param([string]$Serial, [string]$ApplicationId = 'com.modukey.app')
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repoRoot = Split-Path $PSScriptRoot -Parent
$adb = Get-Command adb -ErrorAction Stop
$adbArguments = @()
if ($Serial) { $adbArguments = @('-s', $Serial) }
function Read-Device([string[]]$Arguments) {
    $value = & $adb.Source @adbArguments @Arguments
    if ($LASTEXITCODE -ne 0) { throw 'ADB query failed. Connect and authorize one device, or pass -Serial.' }
    return ($value -join "`n").Trim()
}
$state = Read-Device @('get-state')
if ($state -ne 'device') { throw 'No authorized device is connected.' }
$profile = [ordered]@{
    recordedAtUtc = [DateTime]::UtcNow.ToString('o')
    manufacturer = Read-Device @('shell','getprop','ro.product.manufacturer')
    model = Read-Device @('shell','getprop','ro.product.model')
    device = Read-Device @('shell','getprop','ro.product.device')
    androidVersion = Read-Device @('shell','getprop','ro.build.version.release')
    apiLevel = Read-Device @('shell','getprop','ro.build.version.sdk')
    build = Read-Device @('shell','getprop','ro.build.display.id')
    hyperOsVersion = Read-Device @('shell','getprop','ro.mi.os.version.name')
    miuiVersion = Read-Device @('shell','getprop','ro.miui.ui.version.name')
    romRegion = Read-Device @('shell','getprop','ro.miui.region')
    navigationMode = Read-Device @('shell','settings','get','secure','navigation_mode')
    xiaomiGestureMode = Read-Device @('shell','settings','get','global','force_fsg_nav_bar')
    applicationId = $ApplicationId
    nativePopup = 'NOT TESTED: confirm popup opens from another app and volume does not change.'
    backgroundAndUpgrade = 'NOT TESTED: complete the local plans/compatibility.md checklist (not tracked in Git).'
}
$directory = Join-Path $repoRoot '.local/device-checks'
New-Item -ItemType Directory -Force -Path $directory | Out-Null
$destination = Join-Path $directory ("device-{0}.json" -f [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss'))
$profile | ConvertTo-Json | Set-Content -LiteralPath $destination -Encoding UTF8
Write-Output "Device profile saved to $destination. Empty ROM fields should be copied from About phone."
