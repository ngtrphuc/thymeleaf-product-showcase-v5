param(
    [Parameter(Mandatory = $true)]
    [string]$FrontendDir,

    [Parameter(Mandatory = $true)]
    [string]$LogPath
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

Set-Location $FrontendDir
if (-not (Test-Path -LiteralPath (Join-Path $FrontendDir "node_modules"))) {
    "node_modules is missing. Running npm.cmd install before starting dev server..." *>> $LogPath
    npm.cmd install *>> $LogPath
}
npm.cmd run dev *>> $LogPath
