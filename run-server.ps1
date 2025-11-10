# StockCast Server Startup Script
# This script starts the StockCast server with proper logging and error handling

param(
    [switch]$Clean,
    [switch]$Verbose,
    [switch]$Help
)

function Show-Help {
    Write-Host "StockCast Server Startup Script" -ForegroundColor Green
    Write-Host ""
    Write-Host "Usage: .\run-server.ps1 [options]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Clean    Clean rebuild before starting"
    Write-Host "  -Verbose  Show detailed output"
    Write-Host "  -Help     Show this help message"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\run-server.ps1"
    Write-Host "  .\run-server.ps1 -Clean"
    Write-Host "  .\run-server.ps1 -Verbose"
}

if ($Help) {
    Show-Help
    exit 0
}

$BackendPath = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "backend"

if (-not (Test-Path $BackendPath)) {
    Write-Host "Error: backend directory not found at $BackendPath" -ForegroundColor Red
    exit 1
}

# Check for Java
Write-Host "Checking Java installation..." -ForegroundColor Yellow
$JavaCheck = java -version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Java not found. Please install Java 17+" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Java found" -ForegroundColor Green

# Check for Maven
Write-Host "Checking Maven installation..." -ForegroundColor Yellow
$MvnCheck = mvn -version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Maven not found. Please install Maven 3.6+" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Maven found" -ForegroundColor Green

Write-Host ""
Write-Host "Starting StockCast Server..." -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Green

# Navigate to backend
Push-Location $BackendPath

# Build if necessary
if ($Clean) {
    Write-Host "Cleaning and building project..." -ForegroundColor Yellow
    mvn clean package
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error: Maven build failed" -ForegroundColor Red
        exit 1
    }
}
elseif (-not (Test-Path "target/classes")) {
    Write-Host "Building project (first time)..." -ForegroundColor Yellow
    mvn compile
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error: Maven compile failed" -ForegroundColor Red
        exit 1
    }
}

# Start server
Write-Host ""
Write-Host "Launching Spring Boot server on port 9090..." -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Cyan
Write-Host ""

mvn spring-boot:run

Pop-Location
