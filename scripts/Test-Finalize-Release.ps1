[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repoRoot = Split-Path $PSScriptRoot -Parent
$fixture = Join-Path $repoRoot ('.local/finalize-test-' + [Guid]::NewGuid().ToString('N'))
$work = Join-Path $fixture 'work'
$origin = Join-Path $fixture 'origin.git'
$utf8 = [Text.UTF8Encoding]::new($false)
$finalizer = Join-Path $PSScriptRoot 'Finalize-Release.ps1'
$checks = 0
function Check([bool]$Condition, [string]$Message) {
    if (!$Condition) { throw "Check failed: $Message" }
    $script:checks++
}
function Fixture-Git([string[]]$Arguments) {
    $result = @(& git -c "safe.directory=$($work.Replace('\', '/'))" -c user.name=NudgeTests -c user.email=nudge-tests@example.invalid -c commit.gpgsign=false -c tag.gpgsign=false -C $work @Arguments)
    if ($LASTEXITCODE -ne 0) { throw "Fixture Git failed: $($Arguments[0])." }
    return $result
}
function Write-Fixture([string]$Name, [string]$Value) {
    $path = Join-Path $work $Name
    New-Item -ItemType Directory -Path (Split-Path $path -Parent) -Force | Out-Null
    [IO.File]::WriteAllText($path, $Value, $utf8)
}
function Commit-Fixture([string]$Message) {
    Fixture-Git @('add', '--all') | Out-Null
    Fixture-Git @('commit', '--quiet', '-m', $Message) | Out-Null
}
try {
    New-Item -ItemType Directory -Path $work -Force | Out-Null
    & git init --bare --quiet --initial-branch=main $origin
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the local fixture remote.' }
    Fixture-Git @('init', '--quiet', '--initial-branch=main') | Out-Null
    Fixture-Git @('config', 'user.name', 'NudgeTests') | Out-Null
    Fixture-Git @('config', 'user.email', 'nudge-tests@example.invalid') | Out-Null
    Write-Fixture '.gitignore' ".local/`n"
    Write-Fixture 'version.properties' "versionName=0.3.1`nversionCode=8`n"
    Write-Fixture 'CHANGELOG.md' "# Changelog`n`n## 0.3.1`n"
    Write-Fixture 'scripts/Release.ps1' @'
param([switch]$CreateTag)
$root = Split-Path $PSScriptRoot -Parent
$trust = "safe.directory=$($root.Replace('\', '/'))"
$branch = & git -c $trust -C $root branch --show-current
if ($branch -ne 'main') { throw 'Mock release must run on main.' }
$commit = & git -c $trust -C $root rev-parse HEAD
[IO.File]::WriteAllText((Join-Path $root '.local/built-commit.txt'), $commit)
if ($CreateTag) {
    & git -c $trust -c tag.gpgsign=false -C $root tag -a v0.4.0 -m 'Nudge 0.4.0'
    if ($LASTEXITCODE -ne 0) { throw 'Mock tag failed.' }
}
'@
    Write-Fixture 'scripts/ReleaseGuard.ps1' ([IO.File]::ReadAllText((Join-Path $PSScriptRoot 'Release.ps1')))
    Commit-Fixture 'chore(release): initialize fixture'
    Fixture-Git @('remote', 'add', 'origin', $origin) | Out-Null
    Fixture-Git @('push', '--quiet', 'origin', 'main') | Out-Null
    Fixture-Git @('switch', '--quiet', '-c', 'feature') | Out-Null
    Write-Fixture 'version.properties' "versionName=0.4.0`nversionCode=9`n"
    Write-Fixture 'CHANGELOG.md' "# Changelog`n`n## 0.4.0`n"
    Write-Fixture 'docs/release-notes-0.4.0.md' "# Nudge 0.4.0`n"
    Commit-Fixture 'chore(release): prepare fixture'
    Write-Fixture '.local/signing/keystore.properties' 'fixture-only'
    & $finalizer -RepositoryRoot $work -CheckOnly | Out-Null
    Check (((Fixture-Git @('branch', '--show-current')) -join '') -eq 'feature') 'check-only leaves the branch unchanged'
    Check (!(Test-Path -LiteralPath (Join-Path $work '.local/built-commit.txt'))) 'check-only does not build'
    $guardMessage = ''
    try { & (Join-Path $work 'scripts/ReleaseGuard.ps1') -CreateTag | Out-Null } catch { $guardMessage = $_.Exception.Message }
    Check ($guardMessage -eq 'Build the final signed release from clean main after the pull request is merged.') 'real release script rejects feature-branch signing'
    $rejected = $false
    try { & $finalizer -RepositoryRoot $work | Out-Null } catch { $rejected = $true }
    Check $rejected 'reject finalization before the GitHub merge'
    Check (((Fixture-Git @('branch', '--show-current')) -join '') -eq 'feature') 'unmerged rejection leaves the branch unchanged'
    Write-Fixture 'pending.txt' 'uncommitted'
    $rejected = $false
    try { & $finalizer -RepositoryRoot $work -CheckOnly | Out-Null } catch { $rejected = $true }
    Check $rejected 'reject a dirty checkout'
    Remove-Item -LiteralPath (Join-Path $work 'pending.txt')
    # Simulate a GitHub merge only inside this disposable fixture.
    Fixture-Git @('switch', '--quiet', 'main') | Out-Null
    Fixture-Git @('merge', '--quiet', '--no-ff', 'feature', '-m', 'feat(trigger): integrate fixture') | Out-Null
    $merged = (Fixture-Git @('rev-parse', 'HEAD')) -join ''
    Fixture-Git @('push', '--quiet', 'origin', 'main') | Out-Null
    Fixture-Git @('switch', '--quiet', 'feature') | Out-Null
    & $finalizer -RepositoryRoot $work | Out-Null
    Check (((Fixture-Git @('branch', '--show-current')) -join '') -eq 'main') 'finalization switches to main'
    Check (((Fixture-Git @('rev-parse', 'HEAD')) -join '') -eq $merged) 'finalization creates no local merge commit'
    Check ([IO.File]::ReadAllText((Join-Path $work '.local/built-commit.txt')) -eq $merged) 'build uses the merged source'
    Check (((Fixture-Git @('rev-parse', 'v0.4.0^{}')) -join '') -eq $merged) 'tag identifies the merged source'
    # Exercise the real release entry point without building or signing an APK.
    Write-Fixture 'version.properties' "versionName=0.4.1`nversionCode=10`n"
    Write-Fixture 'CHANGELOG.md' "# Changelog`n`n## 0.4.1`n"
    Write-Fixture 'docs/release-notes-0.4.1.md' "# Nudge 0.4.1`n"
    Write-Fixture 'scripts/Test-Changelog.ps1' "# Changelog checks run separately against the real generator.`n"
    Write-Fixture 'gradlew.bat' "@echo off`r`n> .local\gradle-arguments.txt echo %*`r`nexit /b 23`r`n"
    Write-Fixture '.local/sdk/build-tools/36.0.0/apksigner.bat' "@exit /b 1`r`n"
    Commit-Fixture 'test(release): prepare failed build fixture'
    $previousAndroidHome = $env:ANDROID_HOME
    $buildMessage = ''
    try {
        $env:ANDROID_HOME = Join-Path $work '.local/sdk'
        try { & (Join-Path $work 'scripts/ReleaseGuard.ps1') -CreateTag | Out-Null } catch { $buildMessage = $_.Exception.Message }
    } finally { $env:ANDROID_HOME = $previousAndroidHome }
    Check ($buildMessage -eq 'Release checks or build failed; nothing was distributed.') 'propagate a failing Gradle exit code'
    $arguments = ([IO.File]::ReadAllText((Join-Path $work '.local/gradle-arguments.txt')).Trim() -split '\s+') | ForEach-Object { $_.Trim('"') }
    Check ($arguments[0] -eq 'clean') 'clean compiled sources before release checks'
    foreach ($argument in @(':app:testDebugUnitTest', ':app:lint', ':app:assembleRelease', '--console=plain', '--no-build-cache', '--no-problems-report', '-Pkotlin.incremental=false')) {
        Check ($arguments -contains $argument) "pass $argument as one Gradle argument"
    }
    Check ($arguments.Count -eq 8) 'preserve the property argument without PowerShell splitting'
    Check (!(Test-Path -LiteralPath (Join-Path $work 'releases/0.4.1'))) 'do not export artifacts after failed checks'
    Check (@(Fixture-Git @('tag', '--list', 'v0.4.1')).Count -eq 0) 'do not tag a failed release'
    Write-Output "Passed $checks release finalization checks."
} finally {
    $resolved = (Resolve-Path -LiteralPath $fixture).Path
    $separator = [IO.Path]::DirectorySeparatorChar
    $prefix = [IO.Path]::GetFullPath((Join-Path $repoRoot '.local')).TrimEnd($separator) + $separator
    if (!$resolved.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) { throw 'Refusing to delete a fixture outside .local.' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
