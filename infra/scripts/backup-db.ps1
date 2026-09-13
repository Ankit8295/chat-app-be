param(
    [string]$BackupDir = "$env:USERPROFILE\Backups\the-chat",
    [int]$RetentionDays = 7
)

. "$PSScriptRoot\_home-common.ps1"
Assert-DockerRunning
Assert-HomeEnv

New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
$timestamp = (Get-Date).ToUniversalTime().ToString("yyyyMMddTHHmmssZ")

$dbTargets = @(
    @{ Name = "auth_db"; Service = "auth-postgres"; User = "AUTH_DB_USER" },
    @{ Name = "user_db"; Service = "user-postgres"; User = "USER_DB_USER" },
    @{ Name = "chat_db"; Service = "chat-postgres"; User = "CHAT_DB_USER" },
    @{ Name = "url_db"; Service = "url-postgres"; User = "URL_DB_USER" }
)

foreach ($target in $dbTargets) {
    $db = $target.Name
    $service = $target.Service
    $userVar = $target.User
    $dbUser = (Get-Content $EnvFile | Where-Object { $_ -match "^${userVar}=(.+)$" } | ForEach-Object { $matches[1] } | Select-Object -First 1)
    if (-not $dbUser) {
        Write-Error "${userVar} not found in .env"
        exit 1
    }

    $outFile = Join-Path $BackupDir "${db}_${timestamp}.sql"
    $gzFile = "${outFile}.gz"
    Write-Host "Dumping $db from $service -> $gzFile"

    Push-Location $RepoRoot
    try {
        docker compose -f $ComposeFile --env-file $EnvFile exec -T $service `
            pg_dump -U $dbUser $db | Set-Content -Path $outFile -Encoding utf8NoBOM
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

        $bytes = [System.IO.File]::ReadAllBytes($outFile)
        $fs = [System.IO.File]::Create($gzFile)
        $gzip = New-Object System.IO.Compression.GZipStream($fs, [System.IO.Compression.CompressionMode]::Compress)
        $gzip.Write($bytes, 0, $bytes.Length)
        $gzip.Close()
        $fs.Close()
        Remove-Item $outFile
    } finally {
        Pop-Location
    }
}

Get-ChildItem $BackupDir -Filter "*.sql.gz" |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays) } |
    Remove-Item -Force

Write-Host "Backup complete at $timestamp -> $BackupDir"
