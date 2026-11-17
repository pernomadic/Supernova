# Refresh our main from upstream, then merge upstream into our working branch.
#
#   pwsh ./scripts/sync-upstream.ps1            # update main + merge into custom
#   pwsh ./scripts/sync-upstream.ps1 -NoMerge    # update main only
#   pwsh ./scripts/sync-upstream.ps1 -Branch x   # merge into another branch
#
# main is a mirror of upstream/main: it only ever fast-forwards.

[CmdletBinding()]
param(
    [string]$Upstream = 'upstream',
    [string]$Origin = 'origin',
    [string]$MirrorBranch = 'main',
    [string]$Branch = 'custom',
    [switch]$NoMerge
)

$ErrorActionPreference = 'Stop'

function Invoke-Git {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Args)
    & git @Args
    if ($LASTEXITCODE -ne 0) {
        throw "git $($Args -join ' ') failed with exit code $LASTEXITCODE"
    }
}

if ((& git status --porcelain)) {
    throw 'Working tree is not clean. Commit or stash first.'
}

Write-Host "==> Fetching $Upstream" -ForegroundColor Cyan
Invoke-Git fetch $Upstream --tags --prune

if ($NoMerge) {
    Write-Host "==> $MirrorBranch is $( & git rev-parse --short "$Upstream/$MirrorBranch")" -ForegroundColor Cyan
    exit 0
}

Write-Host "==> Updating $MirrorBranch (fast-forward only)" -ForegroundColor Cyan
Invoke-Git checkout $MirrorBranch
Invoke-Git merge --ff-only "$Upstream/$MirrorBranch"
Invoke-Git push $Origin $MirrorBranch

Write-Host "==> Merging $Upstream/$MirrorBranch into $Branch" -ForegroundColor Cyan
Invoke-Git checkout $Branch
$behind = (& git rev-list --count "$Branch..$Upstream/$MirrorBranch")
if ($behind -eq 0) {
    Write-Host "==> $Branch already contains $Upstream/$MirrorBranch" -ForegroundColor Green
    exit 0
}

& git merge --no-ff "$Upstream/$MirrorBranch" -m "Merge $Upstream/$MirrorBranch into $Branch"
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Merge stopped with conflicts. Fix them, then: git add -A; git commit"
    Write-Warning "To abandon the merge: git merge --abort"
    exit 1
}

Write-Host "==> Done. $behind upstream commit(s) merged into $Branch." -ForegroundColor Green
Write-Host "    Verify with: ./gradlew :app:testDebugUnitTest :app:lintDebug :app:detekt"
Write-Host "    Then push with: git push $Origin $Branch"
