$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$frontendDir = Join-Path $repoRoot "frontend-next"
$logDir = Join-Path $repoRoot ".data\\logs"
$frontendLog = Join-Path $logDir "frontend-live.log"
$backendLog = Join-Path $logDir "backend-live.log"
New-Item -ItemType Directory -Path $logDir -Force | Out-Null

function Test-HttpReady {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url
    )

    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
    } catch {
        return $false
    }
}

function Wait-Until {
    param(
        [Parameter(Mandatory = $true)]
        [scriptblock]$Condition,
        [Parameter(Mandatory = $true)]
        [int]$TimeoutSeconds,
        [Parameter(Mandatory = $true)]
        [string]$Label
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (& $Condition) {
            return $true
        }
        Start-Sleep -Seconds 2
    }

    Write-Host "$Label did not become ready within $TimeoutSeconds seconds."
    return $false
}

function Get-ListeningPid {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $connection = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($null -eq $connection) {
        return $null
    }
    return [int]$connection.OwningProcess
}

function Get-ProcessCommandLine {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Pid
    )

    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $Pid" -ErrorAction SilentlyContinue
    if ($null -eq $processInfo) {
        return ""
    }
    return [string]$processInfo.CommandLine
}

function Is-FrontendProcessForThisRepo {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Pid
    )

    $cmd = (Get-ProcessCommandLine -Pid $Pid).ToLowerInvariant()
    if ([string]::IsNullOrWhiteSpace($cmd)) {
        return $false
    }

    return $cmd.Contains("frontend-next") -and ($cmd.Contains("next") -or $cmd.Contains("npm"))
}

function Is-BackendProcessForThisRepo {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Pid
    )

    $cmd = (Get-ProcessCommandLine -Pid $Pid).ToLowerInvariant()
    if ([string]::IsNullOrWhiteSpace($cmd)) {
        return $false
    }

    return $cmd.Contains("smartphone-shop") -or $cmd.Contains("smartphone_shop")
}

function Ensure-FrontendDependencies {
    $nodeModulesDir = Join-Path $frontendDir "node_modules"
    if (Test-Path -LiteralPath $nodeModulesDir) {
        return
    }

    Write-Host "Frontend dependencies missing. Running npm.cmd install..."
    & npm.cmd install --prefix $frontendDir
}

Write-Host "[1/4] Ensuring Docker + PostgreSQL + Redis + Meilisearch are ready..."
& (Join-Path $repoRoot "scripts/start-dev-infra.ps1")

Write-Host "[2/4] Ensuring Next.js frontend is healthy on :3000..."
$frontendReady = Test-HttpReady -Url "http://localhost:3000/products"
if (-not $frontendReady) {
    $frontendPid = Get-ListeningPid -Port 3000
    if ($null -ne $frontendPid) {
        if (Is-FrontendProcessForThisRepo -Pid $frontendPid) {
            Write-Host "Found stale frontend process on :3000 (PID $frontendPid). Restarting it..."
            Stop-Process -Id $frontendPid -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 1
        } else {
            throw "Port 3000 is in use by PID $frontendPid (not smartphone-shop frontend). Please free this port and run again."
        }
    }

    Ensure-FrontendDependencies
    $frontendCommand = "Set-Location '$frontendDir'; npm.cmd run dev *>> '$frontendLog'"
    Start-Process -FilePath "powershell" -ArgumentList @(
        "-NoExit",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-Command",
        $frontendCommand
    ) | Out-Null

    if (-not (Wait-Until -Condition { Test-HttpReady -Url "http://localhost:3000/products" } -TimeoutSeconds 180 -Label "Frontend")) {
        throw "Frontend failed to boot. Check .data/logs/frontend-live.log"
    }
}

Write-Host "[3/4] Ensuring Spring Boot backend is healthy on :8080..."
$backendReady = Test-HttpReady -Url "http://localhost:8080/actuator/health"
if (-not $backendReady) {
    $backendPid = Get-ListeningPid -Port 8080
    if ($null -ne $backendPid) {
        if (Is-BackendProcessForThisRepo -Pid $backendPid) {
            Write-Host "Found stale backend process on :8080 (PID $backendPid). Restarting it..."
            Stop-Process -Id $backendPid -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 1
        } else {
            throw "Port 8080 is in use by PID $backendPid (not smartphone-shop backend). Please free this port and run again."
        }
    }

    $backendCommand = "$env:SMARTPHONE_SHOP_DEV_AUTO_START_FRONTEND='false'; Set-Location '$repoRoot'; .\mvnw.cmd spring-boot:run *>> '$backendLog'"
    Start-Process -FilePath "powershell" -ArgumentList @(
        "-NoExit",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-Command",
        $backendCommand
    ) | Out-Null

    if (-not (Wait-Until -Condition { Test-HttpReady -Url "http://localhost:8080/actuator/health" } -TimeoutSeconds 180 -Label "Backend")) {
        throw "Backend failed to boot. Check .data/logs/backend-live.log"
    }
}

Write-Host "[4/4] Startup verification complete."
Write-Host ""
Write-Host "Ready:"
Write-Host "- Frontend: http://localhost:3000"
Write-Host "- Backend:  http://localhost:8080"
Write-Host "- API docs: http://localhost:8080/swagger-ui/index.html"
Write-Host "- Jaeger:   http://localhost:16686"
