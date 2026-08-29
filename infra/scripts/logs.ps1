param(
    [string]$Service
)

. "$PSScriptRoot\_home-common.ps1"
Assert-DockerRunning
Assert-HomeEnv

if ($Service) {
    Invoke-Compose logs -f $Service
} else {
    Invoke-Compose logs -f
}
