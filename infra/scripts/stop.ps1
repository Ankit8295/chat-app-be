. "$PSScriptRoot\_home-common.ps1"
Assert-DockerRunning

Write-Host "Stopping home server stack (data volumes preserved)..."
Invoke-Compose down

Write-Host "Done. WARNING: 'docker compose down -v' would destroy the database."
