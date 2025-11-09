# StockCast Client Launcher
param(
    [string]$host = "localhost",
    [int]$port = 9090
)

Write-Host "Connecting to StockCast Server at $host`:$port..." -ForegroundColor Green
Write-Host ""

# Compile first
mvn compile -q

# Run the client
java -cp target/classes com.stockcast.client.StockCastClient $host $port
