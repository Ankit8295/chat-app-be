param(
    [switch]$SkipBuild
)

. "$PSScriptRoot\_home-common.ps1"
Assert-DockerRunning
Assert-HomeEnv

if (-not $SkipBuild) {
    Write-Host "Building bootJars..."
    Push-Location $RepoRoot
    try {
        & .\gradlew.bat :auth:bootJar :user:bootJar :chat:bootJar -x test
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    } finally {
        Pop-Location
    }
}

Write-Host "Starting home server stack..."
Invoke-Compose up -d --build --remove-orphans
Invoke-Compose ps
