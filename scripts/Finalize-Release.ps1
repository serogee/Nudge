[CmdletBinding()]
param([switch]$CheckOnly, [string]$RepositoryRoot)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
if (!$RepositoryRoot) { $RepositoryRoot = Split-Path $PSScriptRoot -Parent }
$repoRoot = (Resolve-Path -LiteralPath $RepositoryRoot).Path
$gitTrust = "safe.directory=$($repoRoot.Replace('\', '/'))"
function Read-Git([string[]]$Arguments) {
    $result = @(& git -c $gitTrust -C $repoRoot @Arguments)
    if ($LASTEXITCODE -ne 0) { throw "Git command failed: $($Arguments[0])." }
    return $result
}
if (@(Read-Git @('status', '--porcelain')).Count) { throw 'Commit or save workspace changes before finalizing a release.' }
$version = @{}
Get-Content -LiteralPath (Join-Path $repoRoot 'version.properties') | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $version[$matches[1].Trim()] = $matches[2].Trim() }
}
$name = $version['versionName']; $code = [int]$version['versionCode']
if ($name -notmatch '^\d+\.\d+\.\d+$' -or $code -le 0) { throw 'Use a semantic version and a positive version code.' }
$notes = Join-Path $repoRoot "docs/release-notes-$name.md"
$changelog = Get-Content -LiteralPath (Join-Path $repoRoot 'CHANGELOG.md') -Raw -Encoding UTF8
if (!(Test-Path -LiteralPath $notes) -or $changelog -notmatch ('(?m)^## ' + [regex]::Escape($name) + '(?=[ \t\r\n])')) {
    throw 'Prepare and commit the reviewed release notes first.'
}
if (!(Test-Path -LiteralPath (Join-Path $repoRoot '.local/signing/keystore.properties'))) { throw 'Restore the original local signing configuration first.' }
if ($CheckOnly) {
    Write-Output "Release preparation is ready for Nudge $name, code $code. Merge on GitHub before finalizing."
    return
}
$preparedCommit = (Read-Git @('rev-parse', 'HEAD')) -join ''
Read-Git @('fetch', '--quiet', 'origin', 'main', '--tags') | Out-Null
& git -c $gitTrust -C $repoRoot merge-base --is-ancestor $preparedCommit origin/main
if ($LASTEXITCODE -ne 0) { throw 'The prepared commits are not on origin/main. Merge the pull request with Create a merge commit first.' }
$remoteVersion = Read-Git @('show', 'origin/main:version.properties')
if ($remoteVersion -notcontains "versionName=$name" -or $remoteVersion -notcontains "versionCode=$code") {
    throw 'The version on origin/main differs from the prepared release. Review it before building.'
}
Read-Git @('switch', '--quiet', 'main') | Out-Null
# Fast-forward only. Never create a local merge commit or discard local commits.
Read-Git @('merge', '--ff-only', 'origin/main') | Out-Null
& (Join-Path $repoRoot 'scripts/Release.ps1') -CreateTag
Write-Output "Next: git push origin v$name"
Write-Output "Create the GitHub release for v$name and attach the four files from releases/$name/."
