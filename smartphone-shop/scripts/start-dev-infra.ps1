$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")

function Test-DockerReady {
    try {
        docker info *> $null
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

function Start-DockerDesktop {
    $candidates = @(
        "C:\Program Files\Docker\Docker\Docker Desktop.exe",
        (Join-Path $env:LOCALAPPDATA "Docker\Docker\Docker Desktop.exe")
    )

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            Start-Process -FilePath $candidate | Out-Null
            Write-Host "Starting Docker Desktop..."
            return
        }
    }

    throw "Docker Desktop executable not found. Please install Docker Desktop."
}

function Wait-UntilDockerReady {
    param(
        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-DockerReady) {
            return $true
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Test-TcpPortOpen {
    param(
        [string]$TargetHost,
        [int]$Port
    )

    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $asyncResult = $client.BeginConnect($TargetHost, $Port, $null, $null)
        $connected = $asyncResult.AsyncWaitHandle.WaitOne(1500)
        if (-not $connected) {
            $client.Close()
            return $false
        }
        $client.EndConnect($asyncResult)
        $client.Close()
        return $true
    } catch {
        return $false
    }
}

function Wait-UntilPostgresReady {
    param(
        [int]$TimeoutSeconds = 90
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-TcpPortOpen -TargetHost "127.0.0.1" -Port 5432) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

if (-not (Test-DockerReady)) {
    Start-DockerDesktop
    if (-not (Wait-UntilDockerReady -TimeoutSeconds 120)) {
        throw "Docker did not become ready within 120 seconds."
    }
}

Push-Location $repoRoot
try {
    docker compose up -d postgres redis
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose up failed."
    }
} finally {
    Pop-Location
}

if (-not (Wait-UntilPostgresReady -TimeoutSeconds 90)) {
    throw "PostgreSQL (localhost:5432) is not ready yet."
}

Write-Host "Dev infra ready: Docker + PostgreSQL + Redis."
