[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repoRoot = Split-Path $PSScriptRoot -Parent
$fixture = Join-Path $repoRoot ('.local/changelog-test-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $fixture -Force | Out-Null
$utf8 = [Text.UTF8Encoding]::new($false)
$generator = Join-Path $PSScriptRoot 'Generate-Changelog.ps1'
$checks = 0
function Check([bool]$Condition, [string]$Message) {
    if (!$Condition) { throw "Check failed: $Message" }
    $script:checks++
}
function Fixture-Git([string[]]$Arguments) {
    $result = @(& git -c "safe.directory=$($fixture.Replace('\', '/'))" -c user.name=NudgeTests -c user.email=nudge-tests@example.invalid -c commit.gpgsign=false -c tag.gpgsign=false -c core.autocrlf=false -c "core.hooksPath=$fixture/.git/hooks" -C $fixture @Arguments)
    if ($LASTEXITCODE -ne 0) { throw "Fixture Git command failed: $($Arguments[0])" }
    return $result
}
function Commit([string]$Message) {
    [IO.File]::WriteAllText((Join-Path $fixture 'commit-message.txt'), $Message, $utf8)
    Fixture-Git @('add', '--all') | Out-Null
    Fixture-Git @('commit', '--quiet', '--allow-empty', '--file', 'commit-message.txt') | Out-Null
}
try {
    Fixture-Git @('init', '--quiet', '--initial-branch=main') | Out-Null
    [IO.File]::WriteAllText((Join-Path $fixture 'version.properties'), "versionName=0.1.0`nversionCode=1`n", $utf8)
    $oldEntry = "## 0.1.0 - 2026-10-16`n`n- Keep this hand-written history.`n"
    [IO.File]::WriteAllText((Join-Path $fixture 'CHANGELOG.md'), "# Changelog`n`n$oldEntry", $utf8)
    Commit 'feat: create the old feature'
    Fixture-Git @('tag', 'v0.1.0') | Out-Null
    # An unrelated branch with a higher version must not become the automatic baseline.
    Fixture-Git @('checkout', '--quiet', '-b', 'other') | Out-Null
    Commit 'feat(ui): add a branch-only feature'
    Fixture-Git @('tag', 'v0.1.9') | Out-Null
    Fixture-Git @('checkout', '--quiet', 'main') | Out-Null
    Commit "feat(trigger): add a new gesture`n- Keep this detail out of the subject."
    Commit 'fix(settings): retain the final value'
    Commit 'perf(trigger): reduce frame requests'
    Commit 'chore(repo): add commit rules'
    Commit 'refactor(settings)!: change the preference format'
    Commit "fix(settings): migrate an old field`nBREAKING CHANGE: remove the old key"
    Commit 'Improve an older unscoped commit'
    Commit 'fix(ui): keep $values and <text> literal'
    Fixture-Git @('tag', 'v0.1.1') | Out-Null
    $preview = (& $generator -RepositoryRoot $fixture -Version 0.1.1) -join "`n"
    Check ($preview.Contains('## Added')) 'feat grouping'
    Check ($preview.Contains('## Fixed')) 'fix grouping'
    Check ($preview.Contains('## Performance')) 'performance grouping'
    Check ($preview.Contains('## Maintenance')) 'maintenance grouping'
    Check ($preview.Contains('## Changed')) 'legacy title support'
    Check ($preview.Contains('## Breaking changes')) 'breaking-change grouping'
    Check ($preview.Contains('remove the old key')) 'breaking footer details'
    Check (!$preview.Contains('Keep this detail')) 'no blank line needed before commit body'
    Check (!$preview.Contains('old feature')) 'exclude commits before the baseline'
    Check (!$preview.Contains('branch-only')) 'ignore tags on unrelated branches'
    Check ($preview.Contains('keep $values and &lt;text&gt; literal')) 'treat commit text as data'
    Commit 'fix(ui): exclude work after the released tag and preserve $values'
    $samePreview = (& $generator -RepositoryRoot $fixture -Version 0.1.1) -join "`n"
    Check ($samePreview -eq $preview) 'tagged preview remains fixed to its release commit'
    $rejected = $false
    try { & $generator -RepositoryRoot $fixture -Version 0.1.1 -Write | Out-Null } catch { $rejected = $true }
    Check $rejected 'refuse to overwrite a released changelog'
    Check ([IO.File]::ReadAllText((Join-Path $fixture 'CHANGELOG.md')) -eq "# Changelog`n`n$oldEntry") 'released history is unchanged'
    [IO.File]::WriteAllText((Join-Path $fixture 'version.properties'), "versionName=0.1.2`nversionCode=3`n", $utf8)
    & $generator -RepositoryRoot $fixture -Write | Out-Null
    $changelog = [IO.File]::ReadAllText((Join-Path $fixture 'CHANGELOG.md'))
    $notesPath = Join-Path $fixture 'docs/release-notes-0.1.2.md'
    $notes = [IO.File]::ReadAllText($notesPath)
    Check ($notes.Contains('exclude work after the released tag')) 'future version uses the latest reachable tag'
    Check ($changelog.Contains('preserve $values')) 'literal dollar signs survive changelog writes'
    Check (!$notes.Contains('add a new gesture')) 'exclude the previous release from the next release'
    Check ($changelog.EndsWith($oldEntry)) 'preserve hand-written older entries'
    & $generator -RepositoryRoot $fixture -Write | Out-Null
    Check ([IO.File]::ReadAllText((Join-Path $fixture 'CHANGELOG.md')) -eq $changelog) 'generation is idempotent'
    Check ([IO.File]::ReadAllText($notesPath) -eq $notes) 'notes are deterministic'
    $future = (& $generator -RepositoryRoot $fixture -Version 0.2.0) -join "`n"
    Check ($future.Contains('exclude work after the released tag') -and !$future.Contains('add a new gesture')) 'exclude unreachable tags even when their versions are lower'
    & $generator -RepositoryRoot $fixture -Artifact | Out-Null
    $artifactPath = Join-Path $fixture 'releases/0.1.2/Nudge-0.1.2-notes.md'
    Check ([IO.File]::ReadAllText($artifactPath) -eq $notes) 'artifact matches generated notes in its version folder'
    Check (!(Test-Path -LiteralPath (Join-Path $fixture 'releases/Nudge-0.1.2-notes.md'))) 'keep artifacts out of the releases root'
    & $generator -RepositoryRoot $fixture -Version 0.1.1 -Artifact | Out-Null
    Check ([IO.File]::ReadAllText((Join-Path $fixture 'releases/0.1.1/Nudge-0.1.1-notes.md')) -eq ($preview.TrimEnd() + "`n")) 'export another version to its own folder'
    Check ([IO.File]::ReadAllText($artifactPath) -eq $notes) 'another version does not replace existing artifacts'
    $rejected = $false
    try { & $generator -RepositoryRoot $fixture -Version 0.2.0 -FromTag v0.1.9 | Out-Null } catch { $rejected = $true }
    Check $rejected 'reject an invalid or unrelated baseline'
    $rejected = $false
    try { & $generator -RepositoryRoot $fixture -Version '../bad' | Out-Null } catch { $rejected = $true }
    Check $rejected 'reject path traversal through version input'
    $first = (& $generator -RepositoryRoot $fixture -Version 0.1.0) -join "`n"
    Check ($first.Contains('create the old feature')) 'first release works without a previous tag'
    $empty = (& $generator -RepositoryRoot $fixture -Version 0.1.2 -ToRef v0.1.1) -join "`n"
    Check ($empty.Contains('No commits since the previous release.')) 'empty release range'
    Write-Output "Passed $checks changelog integration checks."
} finally {
    # Delete only this freshly created fixture inside the workspace, using one shell end to end.
    $resolvedFixture = (Resolve-Path -LiteralPath $fixture).Path
    $separator = [IO.Path]::DirectorySeparatorChar
    $workspacePrefix = [IO.Path]::GetFullPath($repoRoot).TrimEnd($separator) + $separator
    if (!$resolvedFixture.StartsWith($workspacePrefix, [StringComparison]::OrdinalIgnoreCase)) { throw 'Refusing to remove a fixture outside the workspace.' }
    Remove-Item -LiteralPath $resolvedFixture -Recurse -Force
}
