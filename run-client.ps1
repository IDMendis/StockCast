# StockCast Client Launcher
param(
    [string]$host = "localhost",
    [int]$port = 9090
)

Write-Host "Connecting to StockCast Server at $host`:$port..." -ForegroundColor Green
Write-Host ""

# Compile and run from backend directory
Push-Location "$PSScriptRoot\backend"
try {
    mvn compile -q
    java -cp target/classes com.stockcast.client.StockCastClient $host $port
}
finally {
    Pop-Location
}
