param(
    [Parameter(Mandatory = $true)]
    [string]$File,

    [ValidateSet("auth_db", "user_db", "chat_db")]
    [string]$Database
)

. "$PSScriptRoot\_home-common.ps1"
Assert-DockerRunning
Assert-HomeEnv

if (-not (Test-Path $File)) {
    Write-Error "Backup file not found: $File"
    exit 1
}

if (-not $Database) {
    if ($File -match "auth_db") { $Database = "auth_db" }
    elseif ($File -match "user_db") { $Database = "user_db" }
    elseif ($File -match "chat_db") { $Database = "chat_db" }
    else {
        Write-Error "Could not infer database from filename. Pass -Database auth_db|user_db|chat_db"
        exit 1
    }
}

$serviceMap = @{
    auth_db = @{ Service = "auth-postgres"; UserVar = "AUTH_DB_USER" }
    user_db = @{ Service = "user-postgres"; UserVar = "USER_DB_USER" }
    chat_db = @{ Service = "chat-postgres"; UserVar = "CHAT_DB_USER" }
}
$service = $serviceMap[$Database].Service
$userVar = $serviceMap[$Database].UserVar
$dbUser = (Get-Content $EnvFile | Where-Object { $_ -match "^${userVar}=(.+)$" } | ForEach-Object { $matches[1] } | Select-Object -First 1)
if (-not $dbUser) {
    Write-Error "${userVar} not found in .env"
    exit 1
}

Write-Host "Restoring $Database on $service from $File ..."
Write-Host "WARNING: This overwrites data in $Database."

$confirm = Read-Host "Type YES to continue"
if ($confirm -ne "YES") {
    Write-Host "Aborted."
    exit 0
}

$tempSql = Join-Path $env:TEMP "the-chat-restore-$Database.sql"
try {
    if ($File.EndsWith(".gz")) {
        $fs = [System.IO.File]::OpenRead($File)
        $gzip = New-Object System.IO.Compression.GZipStream($fs, [System.IO.Compression.CompressionMode]::Decompress)
        $reader = New-Object System.IO.StreamReader($gzip)
        $reader.ReadToEnd() | Set-Content -Path $tempSql -Encoding utf8NoBOM
        $reader.Close()
        $gzip.Close()
        $fs.Close()
    } else {
        Copy-Item $File $tempSql
    }

    Push-Location $RepoRoot
    try {
        Get-Content $tempSql -Raw | docker compose -f $ComposeFile --env-file $EnvFile exec -T $service `
            psql -U $dbUser -d $Database
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    } finally {
        Pop-Location
    }

    Write-Host "Restore complete for $Database."
} finally {
    if (Test-Path $tempSql) { Remove-Item $tempSql -Force }
}
