[CmdletBinding()]
param([switch]$CreateTag)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repoRoot = Split-Path $PSScriptRoot -Parent
Push-Location $repoRoot
try {
    $gitTrust = "safe.directory=$($repoRoot.Replace('\', '/'))"
    $changes = & git -c $gitTrust status --porcelain
    if ($LASTEXITCODE -ne 0) { throw 'Cannot read Git status.' }
    if ($changes) { throw 'Commit source changes before building a release so its commit is reproducible.' }
    $version = @{}
    Get-Content -LiteralPath 'version.properties' | ForEach-Object {
        if ($_ -match '^([^#=]+)=(.*)$') { $version[$matches[1].Trim()] = $matches[2].Trim() }
    }
    $name = $version['versionName']
    $code = [int]$version['versionCode']
    if ($name -notmatch '^\d+\.\d+\.\d+$' -or $code -le 0) { throw 'Use a semantic versionName and positive versionCode in version.properties.' }
    $commit = & git -c $gitTrust rev-parse HEAD
    if ($LASTEXITCODE -ne 0) { throw 'Could not read the release commit.' }
    $tagAlreadyExists = $false
    $previousReleaseName = $null
    $previousReleaseCode = 0
    $tags = @(& git -c $gitTrust tag --list 'v*')
    if ($LASTEXITCODE -ne 0) { throw 'Could not read release tags.' }
    foreach ($tag in $tags) {
        if ($tag -notmatch '^v\d+\.\d+\.\d+$') { continue }
        $oldVersion = & git -c $gitTrust show "${tag}:version.properties"
        if ($LASTEXITCODE -ne 0) { throw "Could not read version information for $tag." }
        $oldCodeLine = $oldVersion | Where-Object { $_ -match '^versionCode=' } | Select-Object -First 1
        if (!$oldCodeLine) { throw "Missing versionCode in $tag." }
        $oldCode = [int]$oldCodeLine.Substring(12)
        if ($tag -eq "v$name") {
            $tagCommit = & git -c $gitTrust rev-list -n 1 $tag
            if ($LASTEXITCODE -ne 0 -or $tagCommit -ne $commit) { throw "Release $tag already exists at another commit. Increase both version values; do not move the tag." }
            $tagAlreadyExists = $true
        } elseif ($code -le $oldCode) { throw "versionCode must be greater than $oldCode from $tag." }
        elseif ($oldCode -gt $previousReleaseCode) {
            $previousReleaseName = $tag.Substring(1)
            $previousReleaseCode = $oldCode
        }
    }
    $notesSource = Join-Path $repoRoot "docs/release-notes-$name.md"
    $changelog = Get-Content -LiteralPath 'CHANGELOG.md' -Raw -Encoding UTF8
    if (!(Test-Path -LiteralPath $notesSource) -or $changelog -notmatch ('(?m)^## ' + [regex]::Escape($name) + '(?=[ \t\r\n])')) {
        throw 'Generate the changelog first: run scripts/Generate-Changelog.ps1 -Write, review the notes, and commit them with the new version.'
    }
    if (!(Test-Path -LiteralPath '.local/signing/keystore.properties')) { throw 'Run scripts/Initialize-Signing.ps1 or restore your original .local/signing directory first.' }
    $sdkRoot = $env:ANDROID_HOME
    if (!$sdkRoot) { $sdkRoot = $env:ANDROID_SDK_ROOT }
    if (!$sdkRoot -and (Test-Path -LiteralPath 'local.properties')) {
        $sdkLine = Get-Content -LiteralPath 'local.properties' | Where-Object { $_ -match '^sdk.dir=' } | Select-Object -First 1
        if ($sdkLine) { $sdkRoot = $sdkLine.Substring(8).Replace('\\', '\').Replace('\:', ':') }
    }
    if (!$sdkRoot) { throw 'Set ANDROID_HOME or sdk.dir in local.properties.' }
    $apksigner = Join-Path $sdkRoot 'build-tools/36.0.0/apksigner.bat'
    if (!(Test-Path -LiteralPath $apksigner)) { throw 'Install Android build-tools 36.0.0 for signature verification.' }
    & (Join-Path $PSScriptRoot 'Test-Changelog.ps1')
    & .\gradlew.bat :app:testDebugUnitTest :app:lint :app:assembleRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Release checks or build failed; nothing was distributed.' }
    $apk = Join-Path $repoRoot 'app/build/outputs/apk/release/app-release.apk'
    if (!(Test-Path -LiteralPath $apk)) { throw 'Signed APK was not generated. Verify the signing configuration.' }
    $certificateReport = @(& $apksigner verify --verbose --print-certs $apk)
    if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed.' }
    $certificateReport | Write-Output
    $certificateLine = $certificateReport | Where-Object { $_ -match '^Signer #1 certificate SHA-256 digest: ' } | Select-Object -First 1
    if (!$certificateLine) { throw 'Could not read the verified signing certificate digest.' }
    $certificateDigest = ($certificateLine -split ': ', 2)[1].Trim().ToLowerInvariant()
    $updateIdentity = 'No archived prior APK available for signer comparison; perform the device upgrade check.'
    if ($previousReleaseName) {
        $previousApk = Join-Path $repoRoot "releases/$previousReleaseName/Nudge-$previousReleaseName.apk"
        if (!(Test-Path -LiteralPath $previousApk)) {
            # Retain signer checks for copies that still use the older flat layout.
            $previousApk = Join-Path $repoRoot "releases/Nudge-$previousReleaseName.apk"
        }
        if (Test-Path -LiteralPath $previousApk) {
            $previousCertificate = @(& $apksigner verify --print-certs $previousApk)
            if ($LASTEXITCODE -ne 0) { throw 'Archived prior APK failed signature verification.' }
            $previousLine = $previousCertificate | Where-Object { $_ -match '^Signer #1 certificate SHA-256 digest: ' } | Select-Object -First 1
            if (!$previousLine -or ($previousLine -split ': ', 2)[1].Trim().ToLowerInvariant() -ne $certificateDigest) {
                throw 'Release signer differs from the prior APK. Restore the original signing key; do not distribute this APK.'
            }
            $updateIdentity = "Signer matches $previousReleaseName; versionCode increases from $previousReleaseCode to $code. Installation/settings check still requires a device."
        }
    }
    $metadata = Get-Content -LiteralPath 'app/build/outputs/apk/release/output-metadata.json' -Raw | ConvertFrom-Json
    if ($metadata.applicationId -ne 'com.modukey.app' -or $metadata.elements[0].versionCode -ne $code -or $metadata.elements[0].versionName -ne $name) {
        throw 'APK metadata does not match the release identity/version.'
    }
    $releaseDirectory = Join-Path $repoRoot "releases/$name"
    New-Item -ItemType Directory -Force -Path $releaseDirectory | Out-Null
    $destination = Join-Path $releaseDirectory "Nudge-$name.apk"
    Copy-Item -LiteralPath $apk -Destination $destination
    $hash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  Nudge-$name.apk" | Set-Content -LiteralPath "$destination.sha256" -Encoding ASCII
    $notesName = "Nudge-$name-notes.md"
    Copy-Item -LiteralPath $notesSource -Destination (Join-Path $releaseDirectory $notesName)
    [ordered]@{
        name = 'Nudge'; versionName = $name; versionCode = $code; applicationId = 'com.modukey.app'
        commit = $commit; sha256 = $hash; signingCertificateSha256 = $certificateDigest; builtAtUtc = [DateTime]::UtcNow.ToString('o')
        compileSdk = 36; targetSdk = 36; minSdk = 31
        automatedValidation = 'Changelog integration checks, unit tests, lint and signed release build passed.'
        updateIdentity = $updateIdentity
        releaseNotes = $notesName
        deviceCompatibility = 'Local checklist: plans/compatibility.md (not tracked in Git); automated checks do not confirm Redmi behavior.'
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $releaseDirectory "Nudge-$name.json") -Encoding UTF8
    if ($CreateTag -and !$tagAlreadyExists) {
        & git -c $gitTrust tag -a "v$name" -m "Nudge $name"
        if ($LASTEXITCODE -ne 0) { throw 'APK was built, but the Git tag could not be created. An existing release tag must not be moved.' }
    }
    Write-Output "Verified APK: $destination"
    Write-Output "SHA-256: $hash"
    Write-Output "Release notes: $(Join-Path $releaseDirectory $notesName)"
} finally { Pop-Location }
