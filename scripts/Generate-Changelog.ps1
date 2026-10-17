[CmdletBinding()]
param(
    [string]$Version,
    [string]$FromTag,
    [string]$ToRef,
    [switch]$Write,
    [switch]$Artifact,
    [string]$RepositoryRoot
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
if (!$RepositoryRoot) { $RepositoryRoot = Split-Path $PSScriptRoot -Parent }
$repoRoot = (Resolve-Path -LiteralPath $RepositoryRoot).Path
$gitTrust = "safe.directory=$($repoRoot.Replace('\', '/'))"
function Read-Git([string[]]$Arguments) {
    $result = @(& git -c $gitTrust -C $repoRoot @Arguments)
    if ($LASTEXITCODE -ne 0) { throw "Git query failed: $($Arguments[0])." }
    return $result
}
if (!$Version) {
    $line = Get-Content -LiteralPath (Join-Path $repoRoot 'version.properties') | Where-Object { $_ -match '^versionName=' } | Select-Object -First 1
    if (!$line) { throw 'Missing versionName in version.properties.' }
    $Version = $line.Substring(12).Trim()
}
if ($Version -notmatch '^\d+\.\d+\.\d+$') { throw 'Version must have the form 0.1.2.' }
$releaseTag = "v$Version"
$tags = @(Read-Git @('tag', '--list', 'v*') | Where-Object { $_ -match '^v\d+\.\d+\.\d+$' })
$released = $releaseTag -in $tags
if ($released -and $Write) { throw "Release $releaseTag already exists. Increase the version; do not replace its changelog." }
if (!$ToRef) { $ToRef = if ($released) { $releaseTag } else { 'HEAD' } }
$target = (Read-Git @('rev-parse', '--verify', '--end-of-options', "$ToRef^{commit}")) -join ''
if (!$FromTag) {
    $reachable = @(Read-Git @('tag', '--merged', $target, '--list', 'v*'))
    $FromTag = $tags | Where-Object { $_ -in $reachable -and [version]$_.Substring(1) -lt [version]$Version } |
        Sort-Object { [version]$_.Substring(1) } -Descending | Select-Object -First 1
}
if ($FromTag) {
    if ($FromTag -notin $tags -or [version]$FromTag.Substring(1) -ge [version]$Version) { throw 'FromTag must be an existing, lower release version.' }
    & git -c $gitTrust -C $repoRoot merge-base --is-ancestor $FromTag $target
    if ($LASTEXITCODE -ne 0) { throw 'FromTag is not an ancestor of the release commit.' }
}
$range = if ($FromTag) { "$FromTag..$target" } else { $target }
# %B is deliberate: Git's %s joins body bullets into the subject when there is no blank line.
$log = (Read-Git @('log', '--no-merges', '--reverse', '--format=%H%x1f%B%x1e', $range, '--')) -join "`n"
$groups = [ordered]@{
    'Breaking changes' = [Collections.Generic.List[string]]::new()
    'Added' = [Collections.Generic.List[string]]::new()
    'Fixed' = [Collections.Generic.List[string]]::new()
    'Performance' = [Collections.Generic.List[string]]::new()
    'Changed' = [Collections.Generic.List[string]]::new()
    'Maintenance' = [Collections.Generic.List[string]]::new()
}
foreach ($record in ($log -split [char]0x1e)) {
    $parts = $record.Trim() -split [char]0x1f, 2
    if ($parts.Count -ne 2) { continue }
    $hash = $parts[0].Trim()
    $message = $parts[1].Trim()
    $title = ($message -split '\r?\n', 2)[0].Trim()
    $category = 'Changed'
    $description = $title
    if ($title -match '^(?<type>[a-z]+)(?:\((?<scope>[^)]+)\))?(?<breaking>!)?:\s+(?<description>.+)$') {
        $description = $Matches.description
        $category = switch ($Matches.type) {
            'feat' { 'Added' }
            'fix' { 'Fixed' }
            'perf' { 'Performance' }
            { $_ -in @('build', 'chore', 'ci', 'docs', 'style', 'test') } { 'Maintenance' }
            default { 'Changed' }
        }
        if ($Matches['breaking']) { $category = 'Breaking changes' }
    }
    if ($message -match '(?m)^BREAKING[ -]CHANGE:\s*(.+)') {
        $category = 'Breaking changes'
        $description += ". $($Matches[1].Trim())"
    }
    # Commit messages are plain text, never PowerShell commands or replacement expressions.
    $description = $description.Replace('&', '&amp;').Replace('<', '&lt;').Replace('>', '&gt;')
    $groups[$category].Add("- $description ($($hash.Substring(0, 7)))")
}
$body = [Collections.Generic.List[string]]::new()
foreach ($category in $groups.Keys) {
    if ($groups[$category].Count -eq 0) { continue }
    $body.Add("### $category")
    $body.Add('')
    $body.AddRange($groups[$category])
    $body.Add('')
}
if ($body.Count -eq 0) { $body.Add('- No commits since the previous release.'); $body.Add('') }
$bodyText = ($body -join "`n").TrimEnd()
$notes = "# Nudge $Version`n`n" + [regex]::Replace($bodyText, '(?m)^### ', '## ') + "`n"
$utf8 = [Text.UTF8Encoding]::new($false)
if ($Write) {
    $changelog = Join-Path $repoRoot 'CHANGELOG.md'
    $existing = if (Test-Path -LiteralPath $changelog) { [IO.File]::ReadAllText($changelog) } else { "# Changelog`n" }
    $section = "## $Version - $([DateTimeOffset]::Now.ToString('yyyy-MM-dd'))`n`n$bodyText`n`n"
    $pattern = '(?ms)^##[ \t]+' + [regex]::Escape($Version) + '(?=[ \t\r\n])[^\r\n]*\r?\n.*?(?=^##[ \t]+|\z)'
    $matcher = [regex]::new($pattern)
    if ($matcher.IsMatch($existing)) {
        $updated = $matcher.Replace($existing, [Text.RegularExpressions.MatchEvaluator]{ param($match) $section })
    } else {
        $heading = [regex]::Match($existing, '\A# Changelog[^\r\n]*(?:\r?\n|\z)')
        if (!$heading.Success) { throw 'CHANGELOG.md must start with # Changelog.' }
        $updated = $heading.Value.TrimEnd() + "`n`n$section" + $existing.Substring($heading.Length).TrimStart()
    }
    $notesDirectory = Join-Path $repoRoot 'docs'
    New-Item -ItemType Directory -Force -Path $notesDirectory | Out-Null
    [IO.File]::WriteAllText($changelog, $updated.TrimEnd() + "`n", $utf8)
    [IO.File]::WriteAllText((Join-Path $notesDirectory "release-notes-$Version.md"), $notes, $utf8)
    Write-Output "Updated CHANGELOG.md and docs/release-notes-$Version.md. Review and commit them before building the release."
}
if ($Artifact) {
    $directory = Join-Path $repoRoot 'releases'
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    [IO.File]::WriteAllText((Join-Path $directory "Nudge-$Version-notes.md"), $notes, $utf8)
}
if (!$Write -and !$Artifact) { Write-Output $notes }
