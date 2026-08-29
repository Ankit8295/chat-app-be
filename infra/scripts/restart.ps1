param(
    [switch]$SkipBuild
)

. "$PSScriptRoot\stop.ps1"
& "$PSScriptRoot\start.ps1" @PSBoundParameters
