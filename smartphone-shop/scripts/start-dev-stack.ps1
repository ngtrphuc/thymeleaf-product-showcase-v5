$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")

Write-Host "[1/3] Ensuring Docker + PostgreSQL + Redis are ready..."
& (Join-Path $repoRoot "scripts/start-dev-infra.ps1")

$backendCommand = "Set-Location '$repoRoot'; .\mvnw.cmd spring-boot:run"
$frontendCommand = "Set-Location '$repoRoot\frontend-next'; npm.cmd run dev"

Write-Host "[2/3] Starting Spring Boot backend in a new terminal..."
Start-Process -FilePath "powershell" -ArgumentList @(
    "-NoExit",
    "-NoProfile",
    "-ExecutionPolicy",
    "Bypass",
    "-Command",
    $backendCommand
) | Out-Null

Write-Host "[3/3] Starting Next.js frontend in a new terminal..."
Start-Process -FilePath "powershell" -ArgumentList @(
    "-NoExit",
    "-NoProfile",
    "-ExecutionPolicy",
    "Bypass",
    "-Command",
    $frontendCommand
) | Out-Null

Write-Host ""
Write-Host "Full stack is booting:"
Write-Host "- Backend:  http://localhost:8080"
Write-Host "- Frontend: http://localhost:3000"
