# Shared paths for home-server scripts. Dot-source from other scripts in this folder.
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$ComposeFile = Join-Path $RepoRoot "infra\compose.home.yml"
$EnvFile = Join-Path $RepoRoot ".env"

function Assert-HomeEnv {
    if (-not (Test-Path $EnvFile)) {
        Write-Error ".env not found at $EnvFile - copy .env.home.example to .env and fill secrets."
        exit 1
    }
}

function Assert-DockerRunning {
    docker info 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Docker is not running. Start Docker Desktop first."
        exit 1
    }
}

function Invoke-Compose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$ComposeArgs)
    Push-Location $RepoRoot
    try {
        docker compose -f $ComposeFile --env-file $EnvFile @ComposeArgs
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    } finally {
        Pop-Location
    }
}
