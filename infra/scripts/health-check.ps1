param(
    [string]$ApiUrl = "https://api.ankitdev.in"
)

. "$PSScriptRoot\_home-common.ps1"

$ok = $true

Write-Host "=== Docker ==="
try {
    docker info 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Docker not running" }
    Write-Host "[OK] Docker Desktop is running"
} catch {
    Write-Host "[FAIL] Docker Desktop is not running"
    $ok = $false
}

if (Test-Path $EnvFile) {
    Write-Host "[OK] .env exists"
} else {
    Write-Host "[FAIL] .env missing - copy .env.home.example to .env"
    $ok = $false
}

Write-Host "`n=== Containers ==="
Push-Location $RepoRoot
try {
    docker compose -f $ComposeFile --env-file $EnvFile ps 2>$null
} finally {
    Pop-Location
}

Write-Host "`n=== Internal checks ==="
Push-Location $RepoRoot
try {
    foreach ($pg in @("auth-postgres", "user-postgres", "chat-postgres", "url-postgres")) {
        docker compose -f $ComposeFile --env-file $EnvFile exec -T $pg pg_isready 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { Write-Host "[OK] $pg" } else { Write-Host "[FAIL] $pg"; $ok = $false }
    }

    $redisPass = (Get-Content $EnvFile | Where-Object { $_ -match '^REDIS_PASSWORD=(.+)$' } | ForEach-Object { $matches[1] } | Select-Object -First 1)
    if ($redisPass) {
        docker compose -f $ComposeFile --env-file $EnvFile exec -T redis redis-cli -a $redisPass ping 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { Write-Host "[OK] Redis" } else { Write-Host "[FAIL] Redis"; $ok = $false }
    } else {
        Write-Host "[WARN] REDIS_PASSWORD not found in .env"
    }
} finally {
    Pop-Location
}

Write-Host "`n=== External API ($ApiUrl) ==="
try {
    $resp = Invoke-WebRequest -Uri "$ApiUrl/health" -UseBasicParsing -TimeoutSec 10
    if ($resp.StatusCode -eq 200) {
        Write-Host "[OK] GET $ApiUrl/health -> $($resp.StatusCode)"
    } else {
        Write-Host "[WARN] GET $ApiUrl/health -> $($resp.StatusCode)"
        $ok = $false
    }
} catch {
    Write-Host "[WARN] Could not reach $ApiUrl/health (tunnel may be down or API not started)"
    Write-Host "       $($_.Exception.Message)"
}

if ($ok) {
    Write-Host "`nHealth check passed."
    exit 0
} else {
    Write-Host "`nHealth check reported failures."
    exit 1
}
