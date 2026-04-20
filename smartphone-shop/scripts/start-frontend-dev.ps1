param(
    [Parameter(Mandatory = $true)]
    [string]$FrontendDir,

    [Parameter(Mandatory = $true)]
    [string]$LogPath
)

$ErrorActionPreference = "Stop"

Set-Location $FrontendDir
npm.cmd run dev *>> $LogPath
